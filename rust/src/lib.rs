mod config;
pub mod dns;
pub mod logbuf;
pub mod probe;
pub mod tun;
mod tunnel;
pub mod warp;
pub mod wgconf;

#[cfg(target_os = "android")]
mod android_ffi {
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

    fn read_string(env: &mut JNIEnv, value: &JString) -> Option<String> {
        env.get_string(value).ok().map(|s| s.into())
    }

    #[no_mangle]
    pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeStart(
        mut env: JNIEnv,
        _this: JObject,
        fd: jint,
        config: JString,
    ) -> jint {
        init_logger();
        let json = match read_string(&mut env, &config) {
            Some(s) => s,
            None => return -1,
        };
        match crate::tunnel::start_fd(fd, &json) {
            Ok(()) => 0,
            Err(e) => {
                crate::logbuf::push("E", format!("startTunnel: {e}"));
                -2
            }
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeStop(
        _env: JNIEnv,
        _this: JObject,
    ) {
        crate::tunnel::stop();
    }

    #[no_mangle]
    pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeStats(
        mut env: JNIEnv,
        _this: JObject,
    ) -> jstring {
        let (rx, tx, blocked) = crate::tunnel::stats();
        let json = serde_json::json!({
            "rx": rx,
            "tx": tx,
            "blocked": blocked,
            "running": crate::tunnel::is_running(),
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
    pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeParseConfig(
        mut env: JNIEnv,
        _this: JObject,
        text: JString,
        name: JString,
    ) -> jstring {
        let body = match read_string(&mut env, &text) {
            Some(s) => s,
            None => return to_jstring(&mut env, "{\"error\":\"bad text\"}".to_string()),
        };
        let label = read_string(&mut env, &name).unwrap_or_else(|| "WireGuard".to_string());
        let out = match crate::wgconf::parse(&body, Some(&label)) {
            Ok(v) => v.to_string(),
            Err(e) => serde_json::json!({ "error": e }).to_string(),
        };
        to_jstring(&mut env, out)
    }

    #[no_mangle]
    pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeProbe(
        mut env: JNIEnv,
        _this: JObject,
        endpoint: JString,
    ) -> jint {
        match read_string(&mut env, &endpoint) {
            Some(s) => crate::probe::probe(&s),
            None => -1,
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeLogs(
        mut env: JNIEnv,
        _this: JObject,
    ) -> jstring {
        to_jstring(&mut env, crate::logbuf::snapshot())
    }

    #[no_mangle]
    pub extern "system" fn Java_dev_lurexh_halo_core_Native_nativeRegisterWarp(
        mut env: JNIEnv,
        _this: JObject,
    ) -> jstring {
        init_logger();
        let out = match crate::warp::register() {
            Ok(r) => {
                let addresses: Vec<String> = if r.ipv4.is_empty() {
                    Vec::new()
                } else {
                    vec![r.ipv4.clone()]
                };
                serde_json::json!({
                    "name": "WARP",
                    "private_key": r.private_key,
                    "peer_public_key": r.peer_public_key,
                    "preshared_key": serde_json::Value::Null,
                    "endpoint": r.endpoint,
                    "addresses": addresses,
                    "dns": ["1.1.1.1", "1.0.0.1"],
                    "mtu": 1280,
                    "allowed_ips": ["0.0.0.0/0", "::/0"],
                    "keepalive": 25,
                    "reserved": r.reserved,
                })
                .to_string()
            }
            Err(e) => serde_json::json!({ "error": e }).to_string(),
        };
        to_jstring(&mut env, out)
    }
}
