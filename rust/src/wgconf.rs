use serde_json::{json, Value};

/// Parse a standard WireGuard `.conf` (Interface + Peer) into Halo's profile JSON.
pub fn parse(text: &str, name: Option<&str>) -> Result<Value, String> {
    let mut private_key = String::new();
    let mut addresses: Vec<String> = Vec::new();
    let mut dns: Vec<String> = Vec::new();
    let mut mtu: u16 = 1420;
    let mut peer_key = String::new();
    let mut psk: Option<String> = None;
    let mut endpoint = String::new();
    let mut allowed: Vec<String> = Vec::new();
    let mut keepalive: u16 = 0;
    let mut in_peer = false;

    for raw in text.lines() {
        let line = raw.split(['#', ';']).next().unwrap_or("").trim();
        if line.is_empty() {
            continue;
        }
        if line.starts_with('[') {
            in_peer = line.eq_ignore_ascii_case("[Peer]");
            continue;
        }
        let (k, v) = match line.split_once('=') {
            Some(x) => (x.0.trim().to_ascii_lowercase(), x.1.trim().to_string()),
            None => continue,
        };
        match k.as_str() {
            "privatekey" => private_key = v,
            "address" => {
                addresses = v
                    .split(',')
                    .map(|s| s.trim().to_string())
                    .filter(|s| !s.is_empty())
                    .collect()
            }
            "dns" => {
                dns = v
                    .split(',')
                    .map(|s| s.trim().to_string())
                    .filter(|s| !s.is_empty())
                    .collect()
            }
            "mtu" => mtu = v.parse().unwrap_or(mtu),
            "publickey" => {
                if in_peer {
                    peer_key = v;
                }
            }
            "presharedkey" => {
                if in_peer {
                    psk = Some(v);
                }
            }
            "endpoint" => {
                if in_peer {
                    endpoint = v;
                }
            }
            "allowedips" => {
                if in_peer {
                    allowed = v
                        .split(',')
                        .map(|s| s.trim().to_string())
                        .filter(|s| !s.is_empty())
                        .collect();
                }
            }
            "persistentkeepalive" => {
                if in_peer {
                    keepalive = v.parse().unwrap_or(0);
                }
            }
            _ => {}
        }
    }

    if private_key.is_empty() {
        return Err("missing PrivateKey".into());
    }
    if peer_key.is_empty() {
        return Err("missing peer PublicKey".into());
    }
    if endpoint.is_empty() {
        return Err("missing Endpoint".into());
    }
    if addresses.is_empty() {
        return Err("missing Address".into());
    }
    if allowed.is_empty() {
        allowed = vec!["0.0.0.0/0".into(), "::/0".into()];
    }
    if dns.is_empty() {
        dns = vec!["1.1.1.1".into()];
    }

    Ok(json!({
        "name": name.unwrap_or("WireGuard"),
        "private_key": private_key,
        "peer_public_key": peer_key,
        "preshared_key": psk,
        "endpoint": endpoint,
        "addresses": addresses,
        "dns": dns,
        "mtu": mtu,
        "allowed_ips": allowed,
        "keepalive": keepalive,
        "reserved": Value::Null,
    }))
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE: &str = "[Interface]\nPrivateKey = aGVsbG8=\nAddress = 10.2.0.2/32, fd00::2/128\nDNS = 1.1.1.1, 1.0.0.1\nMTU = 1380\n\n[Peer]\nPublicKey = d29ybGQ=\nPresharedKey = cHNr\nAllowedIPs = 0.0.0.0/0, ::/0\nEndpoint = example.com:51820\nPersistentKeepalive = 25\n";

    #[test]
    fn parses_full_config() {
        let v = parse(SAMPLE, Some("Test")).expect("parse");
        assert_eq!(v["name"], "Test");
        assert_eq!(v["private_key"], "aGVsbG8=");
        assert_eq!(v["peer_public_key"], "d29ybGQ=");
        assert_eq!(v["preshared_key"], "cHNr");
        assert_eq!(v["endpoint"], "example.com:51820");
        assert_eq!(v["mtu"], 1380);
        assert_eq!(v["keepalive"], 25);
        assert_eq!(v["addresses"].as_array().unwrap().len(), 2);
        assert_eq!(v["dns"].as_array().unwrap().len(), 2);
    }

    #[test]
    fn rejects_missing_key() {
        assert!(parse("[Interface]\nAddress = 10.0.0.2/32\n", None).is_err());
    }
}
