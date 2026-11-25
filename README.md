# Halo VPN

Private, personal Android VPN client. Rust core, original Compose UI inspired by
Proton's design. Not affiliated with Proton, Cloudflare, or AdGuard.

Status: **scaffold**. UI + VpnService + WARP registration + DNS-adblock engine are
in place; the packet datapath (boringtun/smoltcp loop) is the next milestone.

## What it does

- One app, multiple profiles — `WARP` (Cloudflare, register in-app) and any
  **WireGuard** profile you own (e.g. Proton's official WG configs).
- **DNS adblocker** built into the tunnel (hosts / AdGuard / ABP lists → sinkhole).
- Kill switch, per-app split tunnel, live rx/tx stats (wired next).
- Rust `cdylib` over JNI; ships as a few MB instead of tens.

## Layout

```
app/                     Android app (Kotlin + Compose)
  .../ui/HaloScreen.kt   Orb screen + details bottom-sheet
  .../vpn/HaloVpnService.kt  VpnService, foreground service
  .../vpn/ProfileStore.kt    WireGuard config import
  .../core/Native.kt         JNI bridge
rust/                    Native core (Rust)
  src/lib.rs             JNI exports
  src/tunnel.rs          datapath (stub — next milestone)
  src/dns.rs             DNS sinkhole + blocklist (tested)
  src/warp.rs            WARP device registration (wgcf flow)
.github/workflows/       signed APK build
```

## Build

CI is the source of truth (`.github/workflows/android.yml`) — it builds the Rust
core with `cargo-ndk`, runs Rust tests, and assembles the APK. Signing comes from
repo secrets:

- `HALO_KEYSTORE_BASE64`, `HALO_KEYSTORE_PASSWORD`, `HALO_KEY_ALIAS`, `HALO_KEY_PASSWORD`

Without secrets it still builds an unsigned release APK.

Local dev in Termux: `cargo test --manifest-path rust/Cargo.toml` works today.
Full APK builds belong in CI (no Android SDK here).

## Milestones

1. **Datapath** — boringtun handshake + smoltcp netstack on the tun fd; UDP/53
   diverted to `dns::maybe_sinkhole`. This is the wireproxy replacement.
2. **WARP wiring** — call `warp::register`, map to a `Profile`, persist.
3. **Blocklists** — download + cache hosts lists, hot-reload.
4. **Stats/notifications** — rx/tx counters over JNI; foreground notification.
5. **Split tunnel + kill switch** UI.

## Legal / attribution

Original code, all rights reserved (private, unpublished). Visual direction is
inspired by ProtonVPN's GPLv3 Android client, but **no GPL source is copied** —
this repo stays closed-source. Proton, Cloudflare WARP and AdGuard are trademarks
of their owners; Halo is unaffiliated. WARP registration uses your own device only.
