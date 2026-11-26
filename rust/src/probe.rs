use std::net::{TcpStream, ToSocketAddrs};
use std::time::{Duration, Instant};

/// Round-trip probe against a server endpoint. WireGuard is UDP, so we measure a
/// TCP connect attempt: even a refused connection returns the real RTT.
/// Returns milliseconds, or -1 on resolution failure / timeout.
pub fn probe(endpoint: &str) -> i32 {
    let addrs: Vec<_> = match endpoint.to_socket_addrs() {
        Ok(it) => it.collect(),
        Err(_) => return -1,
    };
    let addr = match addrs.iter().find(|a| a.is_ipv4()).or_else(|| addrs.first()) {
        Some(a) => *a,
        None => return -1,
    };
    let start = Instant::now();
    let _ = TcpStream::connect_timeout(&addr, Duration::from_millis(1500));
    let ms = start.elapsed().as_millis() as i32;
    if ms >= 1500 {
        -1
    } else {
        ms.max(1)
    }
}
