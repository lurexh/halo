# Halo VPN

A free, open-source, zero-account VPN + DNS adblocker for Android.

Halo v1 combines two engines behind one switch:

- **WARP / WireGuard** for reliable zero-config connectivity.
- **OpenVPN** with a live, auto-updating public relay catalog for country
  selection.

The UI lists every ISO country. Countries with a public relay are connectable;
countries without one are shown honestly as unavailable.

> Halo is not affiliated with Cloudflare, Proton, AdGuard, VPN Gate or any relay
> operator. Public relays are operated by volunteers and can observe metadata.

## What works

| Capability | Status | Notes |
|---|---|---|
| WARP registration + WireGuard tunnel | Implemented | Rust `boringtun` core |
| OpenVPN country relays | Implemented | ics-openvpn engine |
| Live relay catalog | Implemented | VPN Gate API, refreshed every 12h |
| Auto-update | Implemented | WorkManager + boot + app open |
| Every-country browser | Implemented | Unavailable countries are disabled |
| DNS adblock | Implemented | WARP sinkhole + filtering DNS for OpenVPN |
| Auto-connect after reboot | Implemented | Uses last route or WARP |
| Quick Settings tile | Implemented | Smart connect / disconnect |
| Live transfer stats | Implemented | WARP JNI + OpenVPN byte listener |
| Kill switch / split tunnel | Not yet | Do not claim otherwise |

### Honest limits

- **Free relays are not private by default.** A relay can see your source IP,
  destinations, timing and volume. Encryption hides content, not metadata. Use
  self-hosted or audited paid servers when privacy is critical.
- **“Every country” does not mean every country has a free relay.** The catalog
  currently covers the countries VPN Gate volunteers provide. Halo shows the
  rest as unavailable rather than faking coverage.
- **Free public relays are often slower than 50 Mbps.** WARP is usually the
  fastest free option. VPN Gate speed varies by volunteer, time of day and
  route.
- **DNS adblock cannot block YouTube/streaming ads** stitched into the same CDN
  stream. It blocks separate ad/tracker domains.
- **A VPN adds latency.** Halo keeps the datapath small, but it cannot beat the
  speed of the underlying relay.

## Engines

### WARP

`ProfileStore.warp()` registers a WARP device with Cloudflare, stores a
WireGuard profile, and starts `HaloVpnService`. The Rust core handles the tun
read/write loop, WireGuard encryption and DNS sinkholing.

### OpenVPN

`CatalogRepository` downloads the VPN Gate catalog, decodes the embedded
OpenVPN profiles, and stores the last good catalog on disk. `OpenVpnEngine`
parses a selected profile with ics-openvpn, sets a full-tunnel default route,
chooses filtering or standard DNS, saves the profile, and launches the
ics-openvpn service.

The two engines are mutually exclusive. Starting one stops the other.

## Build

CI (`.github/workflows/android.yml`) is the source of truth. It:

1. Downloads `app/libs/icsopenvpn.aar` and verifies its SHA-256.
2. Builds the Rust core for `arm64-v8a`, `armeabi-v7a` and `x86_64` with the Android NDK.
3. Runs Rust unit tests.
4. Assembles a debug APK and publishes the nightly release.

```bash
# Local Rust tests (no Android SDK needed)
cargo test --manifest-path rust/Cargo.toml
```

A local APK build requires Android SDK 35, NDK 27.2.12479018, Gradle 8.13 and
JDK 17+, plus the AAR at `app/libs/icsopenvpn.aar`.

## Layout

```
app/
  src/main/java/dev/lurexh/halo/
    MainActivity.kt          catalog + connect orchestration
    ui/HaloRoot.kt           globe, countries, settings, dialogs
    vpn/CatalogRepository.kt VPN Gate fetch/parse/cache
    vpn/CatalogWorker.kt     12h WorkManager refresh
    vpn/OpenVpnEngine.kt     ics-openvpn bridge
    vpn/VpnStarter.kt        tile/boot smart connect
    vpn/HaloVpnService.kt    WARP VpnService
    vpn/ProfileStore.kt      WARP/WireGuard profiles + prefs
    vpn/BlocklistUpdater.kt  hosts-list cache
    core/Native.kt           JNI bridge
rust/
  src/tunnel.rs              WARP datapath + DNS sinkhole
  src/warp.rs                WARP registration
  src/dns.rs                 DNS parsing/sinkhole
```

## License

GPLv2. See `LICENSE` and `THIRD_PARTY_NOTICES.md`.

The ics-openvpn engine is GPLv2. Halo is therefore distributed under GPLv2.
