use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    #[serde(default)]
    pub name: String,
    pub private_key: String,
    pub peer_public_key: String,
    #[serde(default)]
    pub preshared_key: Option<String>,
    pub endpoint: String,
    #[serde(default)]
    pub addresses: Vec<String>,
    #[serde(default)]
    pub dns: Vec<String>,
    #[serde(default = "default_mtu")]
    pub mtu: u16,
    #[serde(default)]
    pub allowed_ips: Vec<String>,
    #[serde(default)]
    pub keepalive: u16,
    #[serde(default)]
    pub reserved: Option<Vec<u8>>,
    #[serde(default)]
    pub blocklists: Vec<String>,
    #[serde(default)]
    pub blocklist_file: Option<String>,
    #[serde(default)]
    pub allowlist_file: Option<String>,
}

fn default_mtu() -> u16 {
    1420
}

impl Config {
    pub fn parse(raw: &str) -> Result<Self, String> {
        serde_json::from_str(raw).map_err(|e| format!("bad config: {e}"))
    }
}
