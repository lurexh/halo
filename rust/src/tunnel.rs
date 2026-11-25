use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;
use std::thread::JoinHandle;
use std::time::Duration;

static RUNNING: AtomicBool = AtomicBool::new(false);
static WORKER: Mutex<Option<JoinHandle<()>>> = Mutex::new(None);

/// Bring up the tunnel on the VpnService tun fd.
///
/// TODO(datapath): the real implementation drives `boringtun::noise::Tunn`
/// against a UDP socket to `endpoint`, shuttling IP packets between the tun fd
/// and the smoltcp netstack, and diverts UDP/53 through `dns::maybe_sinkhole`
/// first (the rootless-proxy design that replaces wireproxy). For now this is a
/// control-plane stub so the UI/service can be built and demoed end-to-end.
pub fn start(fd: i32, cfg_json: &str) -> Result<(), String> {
    let cfg = crate::config::Config::parse(cfg_json)?;
    if cfg.private_key.is_empty() || cfg.peer_public_key.is_empty() || cfg.endpoint.is_empty() {
        return Err("incomplete WireGuard profile".to_string());
    }

    log::warn!(
        "datapath stub active (fd={fd}, endpoint={}, mtu={}) — packets are not forwarded yet",
        cfg.endpoint,
        cfg.mtu
    );

    RUNNING.store(true, Ordering::SeqCst);
    let handle = std::thread::Builder::new()
        .name("halo-tunnel".to_string())
        .spawn(move || {
            while RUNNING.load(Ordering::SeqCst) {
                std::thread::sleep(Duration::from_millis(250));
            }
            log::info!("tunnel worker stopped");
        })
        .map_err(|e| format!("spawn tunnel thread: {e}"))?;

    *WORKER.lock().unwrap() = Some(handle);
    Ok(())
}

pub fn stop() {
    RUNNING.store(false, Ordering::SeqCst);
    if let Some(handle) = WORKER.lock().unwrap().take() {
        let _ = handle.join();
    }
}
