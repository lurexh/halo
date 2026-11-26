use base64::engine::general_purpose::STANDARD as B64;
use base64::Engine;
use boringtun::x25519::{PublicKey, StaticSecret};
use serde::Deserialize;

/// Result of a WARP device registration (the same flow `wgcf register` uses).
#[derive(Debug, Clone)]
pub struct Registration {
    pub private_key: String,
    pub public_key: String,
    pub peer_public_key: String,
    pub endpoint: String,
    pub ipv4: String,
    pub ipv6: String,
    pub reserved: Vec<u8>,
}

#[derive(Deserialize, Default)]
struct RegResponse {
    #[serde(default)]
    id: String,
    #[serde(default)]
    config: PeerConfig,
}

#[derive(Deserialize, Default)]
struct PeerConfig {
    #[serde(default)]
    client_id: String,
    #[serde(default)]
    peers: Vec<Peer>,
    #[serde(default)]
    interface: Interface,
}

#[derive(Deserialize, Default)]
struct Peer {
    #[serde(default)]
    public_key: String,
    #[serde(default)]
    endpoint: Endpoint,
}

#[derive(Deserialize, Default)]
struct Endpoint {
    #[serde(default)]
    host: String,
}

#[derive(Deserialize, Default)]
struct Interface {
    #[serde(default)]
    addresses: Addresses,
}

#[derive(Deserialize, Default)]
struct Addresses {
    #[serde(default)]
    v4: String,
    #[serde(default)]
    v6: String,
}

/// Register a fresh WARP device against Cloudflare's client API.
pub fn register() -> Result<Registration, String> {
    let mut seed = [0u8; 32];
    getrandom::getrandom(&mut seed).map_err(|e| e.to_string())?;
    let secret = StaticSecret::from(seed);
    let public = PublicKey::from(&secret);
    let private_key = B64.encode(secret.to_bytes());
    let public_key = B64.encode(public.as_bytes());

    let tos = time::OffsetDateTime::now_utc()
        .format(&time::format_description::well_known::Rfc3339)
        .unwrap_or_default();

    let body = serde_json::json!({
        "key": public_key,
        "install_id": "",
        "fcm_token": "",
        "tos": tos,
        "model": "PC",
        "serial_number": "",
        "locale": "en_US",
    });

    let resp: RegResponse = ureq::post("https://api.cloudflareclient.com/v0i1909051800/reg")
        .set("Content-Type", "application/json")
        .set("CF-Client-Version", "a-6.11-2223")
        .send_json(body)
        .map_err(|e| format!("registration failed: {e}"))?
        .into_json()
        .map_err(|e| format!("bad registration body: {e}"))?;

    let peer = resp
        .config
        .peers
        .first()
        .ok_or("registration response had no peer")?;

    if peer.public_key.is_empty() || peer.endpoint.host.is_empty() {
        return Err(format!(
            "incomplete registration response (device {})",
            resp.id
        ));
    }

    let host = &peer.endpoint.host;
    let endpoint = if host.matches(':').count() == 1 {
        host.clone()
    } else {
        format!("{host}:2408")
    };

    let reserved = B64
        .decode(resp.config.client_id.trim())
        .ok()
        .filter(|b| b.len() >= 3)
        .map(|b| b[..3].to_vec())
        .unwrap_or_default();

    Ok(Registration {
        private_key,
        public_key,
        peer_public_key: peer.public_key.clone(),
        endpoint,
        ipv4: resp.config.interface.addresses.v4.clone(),
        ipv6: resp.config.interface.addresses.v6.clone(),
        reserved,
    })
}
