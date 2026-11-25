# Halo VPN

A free, open-source (MIT) VPN + adblocker with a small Rust core and an original
Compose UI. One app for WARP, any WireGuard profile (Proton, Mullvad, self-hosted),
and network-wide DNS adblocking.

> **Not affiliated** with Cloudflare, Proton, or AdGuard. Uses your own accounts.

---

## What actually works (and what can't)

| Capability | Status | Notes |
|---|---|---|
| WireGuard tunnel (WARP / Proton / any `.conf`) | **Implemented** | `boringtun` over the VpnService tun fd |
| In-app WARP registration | **Implemented** | Same flow as `wgcf` |
| DNS adblock (all apps, network-wide) | **Implemented** | Sinkhole at UDP/53 inside the tunnel |
| Live rx/tx stats | **Implemented** | JNI poll, 1 Hz |
| Blocklist auto-update | **Implemented** | StevenBlack unified hosts, 24 h cache |
| Linux (named tun + core) | **Implemented** | Needs root + `ip` setup |
| macOS / Windows | **Not yet** | Needs utun / wintun integration |
| Split tunnel, kill switch | **Roadmap** | Hooks exist in `VpnService` |

### Ads: the honest limits

- **DNS/VPN-level blocking cannot block YouTube ads.** YouTube serves ads from the
  same hosts as the video (`googlevideo.com`); sinkholing them breaks playback.
  The same applies to **Peacock** and other streamers — ads are stitched into the
  stream server-side on the same CDN. Blocking them requires client-side content
  filtering (a browser extension or an accessibility-based scroller), not a VPN.
  What Halo *does* block: ad/tracker domains, telemetry, and most in-app banner
  and pre-roll ads that load from separate ad hosts.
- **"0 latency" isn't a thing.** A VPN adds one RTT to the endpoint plus a few µs
  of crypto. Halo minimizes overhead (Rust, no gVisor, no Go runtime), so latency
  ≈ your normal ping to the WARP/Proton endpoint. Throughput is capped by the
  endpoint and your link, not by us.

---

## Architecture

```
App (Kotlin/Compose)  ──VpnService tun fd──►  halo-core (Rust)
   HaloScreen            JNI                   ├─ boringtun::Tunn  (WireGuard)
   HaloVpnService                              ├─ tun read/write loop (libc)
   ProfileStore                                ├─ DNS sinkhole (UDP/53)
   BlocklistUpdater                            └─ UDP socket ↔ endpoint
```

Design choice: because we own a real tun (Android `VpnService`, Linux `/dev/net/tun`)
we **do not need** a userspace SOCKS5 stack or gVisor. Packets are encapsulated
1:1, which is why the core is small and fast. DNS is intercepted in the tun reader
before it is ever encrypted.

## Build

CI (`.github/workflows/android.yml`) is the source of truth: it builds the Rust
core with `cargo-ndk` for arm64/armv7/x86_64, runs `cargo test`, and assembles
the APK. Add signing secrets for a signed build:

- `HALO_KEYSTORE_BASE64`, `HALO_KEYSTORE_PASSWORD`, `HALO_KEY_ALIAS`, `HALO_KEY_PASSWORD`

Without secrets it builds an installable debug APK.

Local (Termux/Linux): `cargo test --manifest-path rust/Cargo.toml` → the datapath
and DNS engine are covered. Full APK builds belong in CI (no Android SDK here).

### Release an APK

```bash
git tag v0.1.0
git push origin v0.1.0
```

The `release` workflow attaches the APK to a GitHub Release automatically.

## Layout

```
app/                      Kotlin + Compose app
  .../ui/HaloScreen.kt    Orb screen + details sheet (WARP button, stats, adblock)
  .../vpn/HaloVpnService.kt  VpnService + foreground notification
  .../vpn/ProfileStore.kt    WireGuard import + WARP profile persistence
  .../vpn/BlocklistUpdater.kt  hosts-list download/cache
  .../core/Native.kt      JNI bridge (safe fallback if .so missing)
  assets/blocklist.txt    offline starter list
rust/                     Native core
  src/tunnel.rs           packet loop: tun ↔ boringtun ↔ udp, DNS sinkhole
  src/dns.rs              DNS parse/sinkhole + IPv4/UDP synthesis (tested)
  src/tun.rs              tun fd I/O (Android fd / Linux /dev/net/tun)
  src/warp.rs             WARP registration (wgcf flow)
  src/lib.rs              JNI exports
```

## Roadmap

1. Split tunnel + kill switch UI (`addDisallowedApplication`, `onRevoke`).
2. Per-app stats and a connection log.
3. macOS `utun` and Windows `wintun` backends for `halo-core`.
4. ABI/`cargo` size trims and a benchmark harness (see `BENCHMARKS.md`).

## License

MIT — see `LICENSE`. Visual direction inspired by ProtonVPN's GPLv3 client, but
no GPL source is included. WARP registration uses your own device only.
