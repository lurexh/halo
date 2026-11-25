use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    pub name: String,
    pub private_key: String,
    pub peer_public_key: String,
    pub endpoint: String,
    pub address: String,
    #[serde(default = "default_prefix")]
    pub prefix: u8,
    #[serde(default = "default_dns")]
    pub dns: String,
    #[serde(default = "default_mtu")]
    pub mtu: u16,
    #[serde(default)]
    pub allowed_ips: Vec<String>,
    #[serde(default)]
    pub blocklists: Vec<String>,
}

fn default_prefix() -> u8 {
    32
}
fn default_dns() -> String {
    "1.1.1.1".to_string()
}
fn default_mtu() -> u16 {
    1280
}

impl Config {
    pub fn parse(raw: &str) -> Result<Self, String> {
        serde_json::from_str(raw).map_err(|e| format!("bad config: {e}"))
    }
}
