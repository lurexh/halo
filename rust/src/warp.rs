use base64::engine::general_purpose::STANDARD as B64;
use base64::Engine;
use serde::Deserialize;
use x25519_dalek::{PublicKey, StaticSecret};

/// Result of a WARP device registration (the same flow `wgcf register` uses).
#[derive(Debug, Clone)]
pub struct Registration {
    pub private_key: String,
    pub public_key: String,
    pub peer_public_key: String,
    pub endpoint: String,
    pub ipv4: String,
    pub ipv6: String,
    pub client_id: String,
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
///
/// Uses your own account/device only — this is the standard wgcf flow.
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
        return Err(format!("incomplete registration response (device {})", resp.id));
    }

    Ok(Registration {
        private_key,
        public_key,
        peer_public_key: peer.public_key.clone(),
        endpoint: format!("{}:2408", peer.endpoint.host),
        ipv4: resp.config.interface.addresses.v4.clone(),
        ipv6: resp.config.interface.addresses.v6.clone(),
        client_id: resp.config.client_id.clone(),
    })
}
