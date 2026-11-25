use std::collections::HashSet;

/// Domain blocklist used by the in-tunnel DNS sinkhole.
#[derive(Debug, Default, Clone)]
pub struct Blocklist {
    domains: HashSet<String>,
}

impl Blocklist {
    pub fn from_lines<'a>(lines: impl IntoIterator<Item = &'a str>) -> Self {
        let mut domains = HashSet::new();
        for line in lines {
            let line = line.trim();
            if line.is_empty() || line.starts_with('#') || line.starts_with('!') {
                continue;
            }
            // Accept hosts-format ("0.0.0.0 ads.example") and bare domains.
            let domain = line.split_whitespace().last().unwrap_or(line);
            let domain = domain.trim_end_matches('.').to_ascii_lowercase();
            if !domain.is_empty() {
                domains.insert(domain);
            }
        }
        Self { domains }
    }

    pub fn is_blocked(&self, query: &str) -> bool {
        let mut domain = query.trim_end_matches('.').to_ascii_lowercase();
        loop {
            if self.domains.contains(&domain) {
                return true;
            }
            match domain.find('.') {
                Some(idx) => domain = domain[idx + 1..].to_string(),
                None => return false,
            }
        }
    }

    pub fn len(&self) -> usize {
        self.domains.len()
    }
}

/// If `query` is a DNS A query for a blocked domain, return a synthetic
/// response that resolves it to 0.0.0.0. Returns `None` otherwise (forward it).
pub fn maybe_sinkhole(query: &[u8], blocklist: &Blocklist) -> Option<Vec<u8>> {
    if query.len() < 17 {
        return None;
    }
    let qdcount = u16::from_be_bytes([query[4], query[5]]);
    if qdcount == 0 {
        return None;
    }

    let (name, offset) = parse_qname(query, 12)?;
    if offset + 4 > query.len() {
        return None;
    }
    let qtype = u16::from_be_bytes([query[offset], query[offset + 1]]);
    if qtype != 1 {
        // Only A records for now; AAAA can be added the same way.
        return None;
    }
    if !blocklist.is_blocked(&name) {
        return None;
    }

    Some(build_a_response(&query[..12], &query[12..offset + 4], 60))
}

fn parse_qname(packet: &[u8], mut offset: usize) -> Option<(String, usize)> {
    let mut labels = Vec::new();
    loop {
        let len = *packet.get(offset)? as usize;
        if len == 0 {
            offset += 1;
            break;
        }
        if len & 0xC0 != 0 {
            return None; // compression pointers are not valid in questions
        }
        offset += 1;
        let end = offset + len;
        let label = packet.get(offset..end)?;
        labels.push(String::from_utf8_lossy(label).into_owned());
        offset = end;
    }
    Some((labels.join("."), offset))
}

fn build_a_response(header: &[u8], question: &[u8], ttl: u32) -> Vec<u8> {
    let mut out = Vec::with_capacity(12 + question.len() + 16);
    out.extend_from_slice(&header[..2]); // transaction id
    out.extend_from_slice(&[0x81, 0x80]); // response, recursion available
    out.extend_from_slice(&[0x00, 0x01]); // QDCOUNT
    out.extend_from_slice(&[0x00, 0x01]); // ANCOUNT
    out.extend_from_slice(&[0x00, 0x00, 0x00, 0x00]); // NSCOUNT + ARCOUNT
    out.extend_from_slice(question);
    // Answer: pointer to question name, A, IN, ttl, 4-byte rdata
    out.extend_from_slice(&[0xC0, 0x0C, 0x00, 0x01, 0x00, 0x01]);
    out.extend_from_slice(&ttl.to_be_bytes());
    out.extend_from_slice(&[0x00, 0x04, 0x00, 0x00, 0x00, 0x00]);
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn a_query(id: u16, name: &str) -> Vec<u8> {
        let mut q = Vec::new();
        q.extend_from_slice(&id.to_be_bytes());
        q.extend_from_slice(&[0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
        for label in name.split('.') {
            q.push(label.len() as u8);
            q.extend_from_slice(label.as_bytes());
        }
        q.push(0);
        q.extend_from_slice(&[0x00, 0x01, 0x00, 0x01]);
        q
    }

    #[test]
    fn blocks_exact_and_subdomain() {
        let bl = Blocklist::from_lines(["0.0.0.0 ads.example", "tracker.net"]);
        assert!(bl.is_blocked("ads.example"));
        assert!(bl.is_blocked("x.y.ads.example"));
        assert!(bl.is_blocked("tracker.net"));
        assert!(!bl.is_blocked("example.com"));
        assert!(!bl.is_blocked("nottracker.net"));
    }

    #[test]
    fn sinkholes_blocked_a_query() {
        let bl = Blocklist::from_lines(["ads.example"]);
        let query = a_query(0x1234, "ads.example");
        let resp = maybe_sinkhole(&query, &bl).expect("should sinkhole");
        assert_eq!(&resp[0..2], &[0x12, 0x34]);
        assert_eq!(&resp[2..4], &[0x81, 0x80]);
        assert_eq!(&resp[resp.len() - 4..], &[0, 0, 0, 0]);
    }

    #[test]
    fn forwards_allowed_query() {
        let bl = Blocklist::from_lines(["ads.example"]);
        assert!(maybe_sinkhole(&a_query(1, "good.example"), &bl).is_none());
    }
}
