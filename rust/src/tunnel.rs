use crate::config::Config;
use crate::dns::{sinkhole_packet, Blocklist};
use crate::tun::Tun;
use base64::engine::general_purpose::STANDARD as B64;
use base64::Engine;
use boringtun::noise::{Tunn, TunnResult};
use boringtun::x25519::{PublicKey, StaticSecret};
use std::net::{SocketAddr, ToSocketAddrs, UdpSocket};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::thread::JoinHandle;
use std::time::Duration;

static RUNNING: AtomicBool = AtomicBool::new(false);
static RX_BYTES: AtomicU64 = AtomicU64::new(0);
static TX_BYTES: AtomicU64 = AtomicU64::new(0);
static WORKERS: Mutex<Vec<JoinHandle<()>>> = Mutex::new(Vec::new());

struct Shared {
    tunnel: Mutex<Tunn>,
    socket: UdpSocket,
    tun: Tun,
    endpoint: SocketAddr,
    blocklist: Blocklist,
}

fn key32(value: &str) -> Result<[u8; 32], String> {
    let bytes = B64
        .decode(value.trim())
        .map_err(|e| format!("bad key: {e}"))?;
    bytes.try_into().map_err(|_| "key must be 32 bytes".to_string())
}

pub fn start_fd(fd: i32, cfg_json: &str) -> Result<(), String> {
    start(Tun::from_fd(fd), cfg_json)
}

pub fn start_named_tun(name: &str, cfg_json: &str) -> Result<(), String> {
    let tun = Tun::open_linux(name).map_err(|e| format!("open tun {name}: {e}"))?;
    start(tun, cfg_json)
}

pub fn is_running() -> bool {
    RUNNING.load(Ordering::SeqCst)
}

pub fn stats() -> (u64, u64) {
    (
        RX_BYTES.load(Ordering::Relaxed),
        TX_BYTES.load(Ordering::Relaxed),
    )
}

fn start(tun: Tun, cfg_json: &str) -> Result<(), String> {
    if RUNNING.load(Ordering::SeqCst) {
        return Err("tunnel already running".to_string());
    }
    let cfg = Config::parse(cfg_json)?;
    if cfg.private_key.is_empty() || cfg.peer_public_key.is_empty() || cfg.endpoint.is_empty() {
        return Err("incomplete WireGuard profile".to_string());
    }

    let endpoint = cfg
        .endpoint
        .to_socket_addrs()
        .map_err(|e| format!("resolve {}: {e}", cfg.endpoint))?
        .next()
        .ok_or_else(|| format!("no address for {}", cfg.endpoint))?;

    let secret = StaticSecret::from(key32(&cfg.private_key)?);
    let peer = PublicKey::from(key32(&cfg.peer_public_key)?);
    let tunnel = Tunn::new(secret, peer, None, Some(25), random_index(), None);

    let socket = UdpSocket::bind("0.0.0.0:0").map_err(|e| format!("udp bind: {e}"))?;
    let _ = socket.set_read_timeout(Some(Duration::from_millis(200)));

    let mut blocklist = Blocklist::from_lines(cfg.blocklists.iter().map(|s| s.as_str()));
    if let Some(path) = &cfg.blocklist_file {
        match std::fs::read_to_string(path) {
            Ok(text) => blocklist = Blocklist::from_lines(text.lines()),
            Err(e) => log::warn!("blocklist {path}: {e}"),
        }
    }
    log::info!(
        "halo-core up: endpoint={endpoint} mtu={} blocklist={} domains",
        cfg.mtu,
        blocklist.len()
    );

    let shared = Arc::new(Shared {
        tunnel: Mutex::new(tunnel),
        socket,
        tun,
        endpoint,
        blocklist,
    });

    RX_BYTES.store(0, Ordering::SeqCst);
    TX_BYTES.store(0, Ordering::SeqCst);
    RUNNING.store(true, Ordering::SeqCst);

    let mut workers = Vec::new();
    let s = shared.clone();
    workers.push(spawn("halo-tun", move || tun_loop(s))?);
    let s = shared.clone();
    workers.push(spawn("halo-udp", move || udp_loop(s))?);
    let s = shared.clone();
    workers.push(spawn("halo-timer", move || timer_loop(s))?);
    *WORKERS.lock().unwrap() = workers;
    Ok(())
}

pub fn stop() {
    if !RUNNING.swap(false, Ordering::SeqCst) {
        return;
    }
    let workers = std::mem::take(&mut *WORKERS.lock().unwrap());
    for worker in workers {
        let _ = worker.join();
    }
    log::info!("halo-core stopped");
}

fn spawn(name: &str, f: impl FnOnce() + Send + 'static) -> Result<JoinHandle<()>, String> {
    std::thread::Builder::new()
        .name(name.to_string())
        .spawn(f)
        .map_err(|e| format!("spawn {name}: {e}"))
}

fn random_index() -> u32 {
    let mut bytes = [0u8; 4];
    if getrandom::getrandom(&mut bytes).is_err() {
        return 1;
    }
    u32::from_ne_bytes(bytes)
}

fn tun_loop(shared: Arc<Shared>) {
    let mut buf = vec![0u8; 65_535];
    let mut out = vec![0u8; 65_535];
    while RUNNING.load(Ordering::SeqCst) {
        let n = match shared.tun.read_packet(&mut buf) {
            Ok(0) => continue,
            Ok(n) => n,
            Err(e) => {
                if RUNNING.load(Ordering::SeqCst) {
                    log::warn!("tun read: {e}");
                }
                break;
            }
        };
        let packet = &buf[..n];

        // Adblock: answer blocked DNS locally, before it ever leaves the device.
        if let Some(reply) = sinkhole_packet(packet, &shared.blocklist) {
            if shared.tun.write_packet(&reply).is_ok() {
                RX_BYTES.fetch_add(reply.len() as u64, Ordering::Relaxed);
            }
            continue;
        }

        let mut tunnel = shared.tunnel.lock().unwrap();
        match tunnel.encapsulate(packet, &mut out) {
            TunnResult::WriteToNetwork(bytes) => {
                if let Err(e) = shared.socket.send_to(bytes, shared.endpoint) {
                    log::warn!("udp send: {e}");
                }
                TX_BYTES.fetch_add(n as u64, Ordering::Relaxed);
            }
            TunnResult::Done => {
                TX_BYTES.fetch_add(n as u64, Ordering::Relaxed);
            }
            TunnResult::Err(e) => log::warn!("encapsulate: {e:?}"),
            _ => {}
        }
    }
}

fn udp_loop(shared: Arc<Shared>) {
    let mut buf = vec![0u8; 65_535];
    let mut out = vec![0u8; 65_535];
    while RUNNING.load(Ordering::SeqCst) {
        let (n, src) = match shared.socket.recv_from(&mut buf) {
            Ok(value) => value,
            Err(e)
                if e.kind() == std::io::ErrorKind::WouldBlock
                    || e.kind() == std::io::ErrorKind::TimedOut =>
            {
                continue
            }
            Err(e) => {
                if RUNNING.load(Ordering::SeqCst) {
                    log::warn!("udp recv: {e}");
                }
                break;
            }
        };

        let mut tunnel = shared.tunnel.lock().unwrap();
        match tunnel.decapsulate(Some(src.ip()), &buf[..n], &mut out) {
            TunnResult::WriteToTunnelV4(packet, _) | TunnResult::WriteToTunnelV6(packet, _) => {
                if shared.tun.write_packet(packet).is_ok() {
                    RX_BYTES.fetch_add(packet.len() as u64, Ordering::Relaxed);
                }
            }
            TunnResult::WriteToNetwork(bytes) => {
                let _ = shared.socket.send_to(bytes, shared.endpoint);
            }
            TunnResult::Done => {}
            TunnResult::Err(e) => log::warn!("decapsulate: {e:?}"),
        }
    }
}

fn timer_loop(shared: Arc<Shared>) {
    let mut out = vec![0u8; 2048];
    while RUNNING.load(Ordering::SeqCst) {
        {
            let mut tunnel = shared.tunnel.lock().unwrap();
            match tunnel.update_timers(&mut out) {
                TunnResult::WriteToNetwork(bytes) => {
                    let _ = shared.socket.send_to(bytes, shared.endpoint);
                }
                TunnResult::Err(e) => log::warn!("timers: {e:?}"),
                _ => {}
            }
        }
        std::thread::sleep(Duration::from_millis(200));
    }
}
