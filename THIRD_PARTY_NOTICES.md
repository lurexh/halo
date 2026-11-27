# Third-party notices

Halo itself is licensed under the GNU General Public License, version 2
(`LICENSE`). It embeds or links the following components:

## ics-openvpn

- Artifact: `network.mysterium.openvpn:icsopenvpn:0.7.55-myst`
- Source: https://github.com/schwabe/ics-openvpn (and the Mysterium fork used to
  publish the Android library)
- License: GNU General Public License, version 2, with the additional terms
  included in the upstream project.
- Use: OpenVPN client engine, service, profile parser and VPN launch helper.
- The AAR is downloaded by CI from the Mysterium `openvpn_dart` local Maven
  mirror. It is not committed to this repository.

## boringtun

- Source: https://github.com/cloudflare/boringtun
- License: BSD-3-Clause
- Use: WireGuard datapath inside the Rust core.

## VPN Gate

- Source: https://www.vpngate.net/
- Use: public volunteer relay catalog and OpenVPN configuration data.
- Halo does not operate these relays and cannot guarantee their availability,
  speed, logging policy or trustworthiness. They are third-party public
  endpoints.

## Cloudflare WARP

- Source: https://developers.cloudflare.com/warp-client/
- Use: optional zero-config WARP registration and WireGuard endpoint.
- Halo is not affiliated with Cloudflare.

## AdGuard DNS

- Source: https://adguard-dns.io/
- Use: optional filtering DNS resolvers (`94.140.14.14`, `94.140.15.15`) when
  DNS adblock is enabled for the OpenVPN engine.
- Halo is not affiliated with AdGuard.

## StevenBlack hosts

- Source: https://github.com/StevenBlack/hosts
- Use: starter blocklist for the WARP DNS sinkhole.
