use std::collections::VecDeque;
use std::sync::{Mutex, OnceLock};

static LOGS: OnceLock<Mutex<VecDeque<String>>> = OnceLock::new();

fn buf() -> &'static Mutex<VecDeque<String>> {
    LOGS.get_or_init(|| Mutex::new(VecDeque::with_capacity(512)))
}

pub fn push(level: &str, msg: impl AsRef<str>) {
    let text = msg.as_ref().to_string();
    let line = format!("[{level}] {text}");
    #[cfg(target_os = "android")]
    match level {
        "E" => log::error!("{text}"),
        "W" => log::warn!("{text}"),
        _ => log::info!("{text}"),
    }
    #[cfg(not(target_os = "android"))]
    eprintln!("{line}");
    let mut b = buf().lock().unwrap();
    if b.len() >= 512 {
        b.pop_front();
    }
    b.push_back(line);
}

pub fn snapshot() -> String {
    buf()
        .lock()
        .unwrap()
        .iter()
        .cloned()
        .collect::<Vec<_>>()
        .join("\n")
}
