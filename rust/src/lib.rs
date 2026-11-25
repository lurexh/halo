mod config;
pub mod dns;
pub mod tun;
mod tunnel;
pub mod warp;

use jni::objects::{JObject, JString};
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use std::sync::Once;

fn init_logger() {
    static ONCE: Once = Once::new();
    ONCE.call_once(|| {
        android_logger::init_once(
            android_logger::Config::default()
                .with_max_level(log::LevelFilter::Info)
                .with_tag("HaloCore"),
        );
    });
}

fn to_jstring(env: &mut JNIEnv, value: String) -> jstring {
    match env.new_string(value) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeStart(
    mut env: JNIEnv,
    _this: JObject,
    fd: jint,
    config: JString,
) -> jint {
    init_logger();
    let json: String = match env.get_string(&config) {
        Ok(s) => s.into(),
        Err(e) => {
            log::error!("invalid config string: {e}");
            return -1;
        }
    };
    match tunnel::start_fd(fd, &json) {
        Ok(()) => 0,
        Err(e) => {
            log::error!("startTunnel: {e}");
            -2
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeStop(_env: JNIEnv, _this: JObject) {
    tunnel::stop();
}

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeStats(
    mut env: JNIEnv,
    _this: JObject,
) -> jstring {
    let (rx, tx) = tunnel::stats();
    let json = serde_json::json!({
        "rx": rx,
        "tx": tx,
        "running": tunnel::is_running(),
    })
    .to_string();
    to_jstring(&mut env, json)
}

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeVersion(
    mut env: JNIEnv,
    _this: JObject,
) -> jstring {
    let value = format!("halo-core {}", env!("CARGO_PKG_VERSION"));
    to_jstring(&mut env, value)
}

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeRegisterWarp(
    mut env: JNIEnv,
    _this: JObject,
) -> jstring {
    init_logger();
    let payload = match warp::register() {
        Ok(r) => {
            let address = r.ipv4.split('/').next().unwrap_or("").to_string();
            serde_json::json!({
                "private_key": r.private_key,
                "peer_public_key": r.peer_public_key,
                "endpoint": r.endpoint,
                "address": address,
                "prefix": 32,
                "dns": "1.1.1.1",
                "mtu": 1280,
            })
            .to_string()
        }
        Err(e) => serde_json::json!({ "error": e }).to_string(),
    };
    to_jstring(&mut env, payload)
}
