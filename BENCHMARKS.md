# Benchmarks

No numbers are published here yet — fabricated benchmarks are worse than none.
This file defines *how* we measure, so results are reproducible and comparable.

## Targets to compare against

- **WireGuard official app** (same endpoint) — baseline for protocol overhead.
- **wireproxy** — closest architectural match (Go + gVisor userspace proxy).
- **AdGuard DNS** (system resolver) — adblock correctness + lookup latency.
- **Proton VPN / WARP official app** — connect time and memory footprint.

## Metrics

| Metric | How |
|---|---|
| APK size | `ls -l app/build/outputs/apk/release/*.apk` |
| Idle RSS | `adb shell dumpsys meminfo dev.lurexh.halo` |
| Connect time | timestamp in `halo-timer` first handshake vs. orb tap |
| Throughput | `iperf3 -c <server>` through the tunnel, 30 s, 3 runs, median |
| Added RTT | `ping -c 20` direct vs. through tunnel, report delta |
| DNS block latency | `dig @1.1.1.1 blocked.example` vs. allowed, p50/p95 |
| Block rate | curated 200-domain ad/tracker sample, count sinkholed |
| Battery | Android Battery Historian over a 1 h idle session |

## Rules

- Same device, same network, same endpoint for A/B.
- Report median + p95, not best-case.
- Disclose CPU (crypto is CPU-bound on phones).
- Never claim "0 latency" — report the added RTT honestly.

## Harness (WIP)

The DNS sinkhole path is already unit-tested (`cargo test -p halo-core`). A
throughput/latency harness will live in `scripts/bench.sh` once the desktop tun
backend lands, so the same core can be measured on Linux against `wireproxy`.
