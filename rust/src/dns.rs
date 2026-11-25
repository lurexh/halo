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

    pub fn is_empty(&self) -> bool {
        self.domains.is_empty()
    }
}

/// If `query` is a DNS A query for a blocked domain, return a synthetic response
/// that resolves it to 0.0.0.0. Returns `None` otherwise (forward it).
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
        return None;
    }
    if !blocklist.is_blocked(&name) {
        return None;
    }

    Some(build_a_response(&query[..12], &query[12..offset + 4], 60))
}

/// Given a full IPv4/UDP packet destined for port 53, return a complete IPv4/UDP
/// reply that sinkholes it (or `None` if it should be forwarded).
pub fn sinkhole_packet(packet: &[u8], blocklist: &Blocklist) -> Option<Vec<u8>> {
    if packet.len() < 28 || packet[0] >> 4 != 4 {
        return None;
    }
    let ihl = ((packet[0] & 0x0f) as usize) * 4;
    if ihl < 20 || packet.len() < ihl + 8 {
        return None;
    }
    if packet[9] != 17 {
        return None;
    }
    let total = u16::from_be_bytes([packet[2], packet[3]]) as usize;
    let end = total.min(packet.len());
    if end < ihl + 8 {
        return None;
    }

    let src_port = u16::from_be_bytes([packet[ihl], packet[ihl + 1]]);
    let dst_port = u16::from_be_bytes([packet[ihl + 2], packet[ihl + 3]]);
    if dst_port != 53 {
        return None;
    }
    let udp_len = u16::from_be_bytes([packet[ihl + 4], packet[ihl + 5]]) as usize;
    let payload_end = (ihl + udp_len).min(end);
    if payload_end <= ihl + 8 {
        return None;
    }
    let payload = &packet[ihl + 8..payload_end];

    let response = maybe_sinkhole(payload, blocklist)?;

    let src_ip: [u8; 4] = packet[12..16].try_into().ok()?;
    let dst_ip: [u8; 4] = packet[16..20].try_into().ok()?;
    Some(build_ipv4_udp(dst_ip, src_ip, 53, src_port, &response))
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
            return None;
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
    out.extend_from_slice(&header[..2]);
    out.extend_from_slice(&[0x81, 0x80]);
    out.extend_from_slice(&[0x00, 0x01]);
    out.extend_from_slice(&[0x00, 0x01]);
    out.extend_from_slice(&[0x00, 0x00, 0x00, 0x00]);
    out.extend_from_slice(question);
    out.extend_from_slice(&[0xC0, 0x0C, 0x00, 0x01, 0x00, 0x01]);
    out.extend_from_slice(&ttl.to_be_bytes());
    out.extend_from_slice(&[0x00, 0x04, 0x00, 0x00, 0x00, 0x00]);
    out
}

fn build_ipv4_udp(src: [u8; 4], dst: [u8; 4], src_port: u16, dst_port: u16, payload: &[u8]) -> Vec<u8> {
    let udp_len = 8 + payload.len();
    let total = 20 + udp_len;
    let mut p = vec![0u8; total];
    p[0] = 0x45;
    p[2..4].copy_from_slice(&(total as u16).to_be_bytes());
    p[6..8].copy_from_slice(&[0x40, 0]);
    p[8] = 64;
    p[9] = 17;
    p[12..16].copy_from_slice(&src);
    p[16..20].copy_from_slice(&dst);
    let csum = ipv4_checksum(&p[..20]);
    p[10..12].copy_from_slice(&csum.to_be_bytes());
    p[20..22].copy_from_slice(&src_port.to_be_bytes());
    p[22..24].copy_from_slice(&dst_port.to_be_bytes());
    p[24..26].copy_from_slice(&(udp_len as u16).to_be_bytes());
    p[26..28].copy_from_slice(&[0, 0]);
    p[28..].copy_from_slice(payload);
    p
}

fn ipv4_checksum(header: &[u8]) -> u16 {
    let mut sum = 0u32;
    let mut i = 0;
    while i + 1 < header.len() {
        sum += u16::from_be_bytes([header[i], header[i + 1]]) as u32;
        i += 2;
    }
    if i < header.len() {
        sum += (header[i] as u32) << 8;
    }
    while sum >> 16 != 0 {
        sum = (sum & 0xffff) + (sum >> 16);
    }
    !(sum as u16)
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

    fn udp_packet(src: [u8; 4], dst: [u8; 4], sport: u16, dport: u16, payload: &[u8]) -> Vec<u8> {
        let udp_len = 8 + payload.len();
        let total = 20 + udp_len;
        let mut p = vec![0u8; 28];
        p[0] = 0x45;
        p[2..4].copy_from_slice(&(total as u16).to_be_bytes());
        p[8] = 64;
        p[9] = 17;
        p[12..16].copy_from_slice(&src);
        p[16..20].copy_from_slice(&dst);
        let csum = ipv4_checksum(&p[..20]);
        p[10..12].copy_from_slice(&csum.to_be_bytes());
        p[20..22].copy_from_slice(&sport.to_be_bytes());
        p[22..24].copy_from_slice(&dport.to_be_bytes());
        p[24..26].copy_from_slice(&(udp_len as u16).to_be_bytes());
        p[26..28].copy_from_slice(&[0, 0]);
        p.extend_from_slice(payload);
        p
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
        let resp = maybe_sinkhole(&a_query(0x1234, "ads.example"), &bl).expect("sinkhole");
        assert_eq!(&resp[0..2], &[0x12, 0x34]);
        assert_eq!(&resp[2..4], &[0x81, 0x80]);
        assert_eq!(&resp[resp.len() - 4..], &[0, 0, 0, 0]);
    }

    #[test]
    fn forwards_allowed_query() {
        let bl = Blocklist::from_lines(["ads.example"]);
        assert!(maybe_sinkhole(&a_query(1, "good.example"), &bl).is_none());
    }

    #[test]
    fn sinkholes_full_udp_packet_and_swaps_endpoints() {
        let bl = Blocklist::from_lines(["ads.example"]);
        let pkt = udp_packet([10, 0, 0, 2], [1, 1, 1, 1], 44444, 53, &a_query(7, "ads.example"));
        let reply = sinkhole_packet(&pkt, &bl).expect("reply");
        assert_eq!(&reply[12..16], &[1, 1, 1, 1]);
        assert_eq!(&reply[16..20], &[10, 0, 0, 2]);
        assert_eq!(u16::from_be_bytes([reply[20], reply[21]]), 53);
        assert_eq!(u16::from_be_bytes([reply[22], reply[23]]), 44444);
        assert_eq!(&reply[reply.len() - 4..], &[0, 0, 0, 0]);
    }

    #[test]
    fn does_not_sinkhole_other_ports() {
        let bl = Blocklist::from_lines(["ads.example"]);
        let pkt = udp_packet([10, 0, 0, 2], [1, 1, 1, 1], 44444, 443, &a_query(7, "ads.example"));
        assert!(sinkhole_packet(&pkt, &bl).is_none());
    }
}
