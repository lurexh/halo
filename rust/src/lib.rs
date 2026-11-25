mod config;
pub mod dns;
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

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_startTunnel(
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
    match tunnel::start(fd, &json) {
        Ok(()) => 0,
        Err(e) => {
            log::error!("startTunnel failed: {e}");
            -2
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_stopTunnel(_env: JNIEnv, _this: JObject) {
    tunnel::stop();
}

#[no_mangle]
pub extern "system" fn Java_dev_lurexh_halo_core_Native_version(
    mut env: JNIEnv,
    _this: JObject,
) -> jstring {
    let value = format!("halo-core {}", env!("CARGO_PKG_VERSION"));
    match env.new_string(value) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
