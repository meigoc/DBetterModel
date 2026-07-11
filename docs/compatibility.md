# Compatibility

DBetterModel 6.0 ships one jar containing three compat layers. At startup it reads the installed BetterModel version and selects the matching layer:

| BetterModel | Layer | Notes |
|---|---|---|
| < 1.15.0 | — | Unsupported: clear log message, plugin disables. Use a pre-6.0 release (see README table). |
| 1.15.x | `v1` | Bukkit-event based. |
| 2.0.1 – 2.2.x | `v2` | BM event bus. 2.0.x/2.1.x run in a reduced-capability mode (see below). |
| 3.x | `v3` | BM event bus; requires a Java 25 JVM (BetterModel 3.x itself requires Java 25). |
| 4.x+ (future) | best-effort | DBM reflectively probes the key 3.x API surface; if the probes pass, the v3 layer is enabled with a loud warning, otherwise DBM disables gracefully. Never a crash, never silent misbehavior. |

Check the active layer at runtime with `<bm.compat_layer>` and the BetterModel version with `<bm.version>`.

## Capabilities

Each layer declares what the running BetterModel can do. Scripts query the set with `<bm.capabilities>` (lowercase names). Anything a layer cannot do results in a clean, documented error from the command/tag, or the related event simply not firing — never a stacktrace.

| Capability | v1 (1.15.x) | v2 (2.x) | v3 (3.x) | Controls |
|---|---|---|---|---|
| `event_bus` | — | ✔ | ✔ | Internal delivery detail (v1 uses Bukkit events; same script events either way). |
| `animation_signals` | ✔ | ✔ | ✔ | `bm animation signal` from `denizen:` keyframes. |
| `player_animation_events` | ✔ | ✔ | ✔ | `bm player animation signal`. |
| `dummy_trackers` | ✔ | ✔ | ✔ | `bmsummon`. |
| `animation_priority` | — | — | ✔ | BM 3.x `AnimationModifier.priority`. |
| `per_player_animation` | ✔ | ✔ | ✔ | `bmstate ... for_players:`. |
| `hitbox_damage_events` | ✔ | ✔ | ✔ | `bm hitbox damaged`. |
| `spawn_events` | ✔ | ✔ | ✔ | `bm model spawns/despawns for player`. |
| `skin_api` | ✔ | ✔ | ✔ | `bmpart`. |
| `mount_events` | ✔ | ✔ | ✔ | `bm model mounted/dismounted`. |
| `hitbox_interact_event` | ✔ | ✔ | — | Non-positional interact event (v3 uses the at-variant instead — `bm hitbox interacted` fires everywhere). |
| `hitbox_interact_at_event` | ✔ | ✔ | ✔ | Positional interact event. |
| `tracker_animation_api` | — | ✔ (2.2+) | ✔ | BM TrackerAnimation API. |
| `bone_filter_animate` | ✔ | ✔ (2.2+, emulated per bone) | ✔ (emulated per bone) | `bmstate ... bones:` restricted to a bone subset. |
| `player_head_item` | ✔ | ✔ | — | BM player-head item helper (removed in BM 3.x). |
| `animation_lifecycle_events` | — | ✔ | ✔ | `bm animation starts/ends`. |

### BetterModel 2.0.x / 2.1.x reduced mode

The 2.x line drifted internally: the TrackerAnimation API and per-bone animation override arrived in 2.2.0. On 2.0.x/2.1.x servers, DBM detects the sub-line at startup and logs a warning: `bones:` filters on `bmstate` apply to the whole model. Update BetterModel to 2.2.0+ for full support. Everything else works.

## Java requirements

| Setup | Java |
|---|---|
| BetterModel 1.15.x or 2.x | 21+ |
| BetterModel 3.x | 25 (mandated by BetterModel 3.x itself) |

The 6.0 jar intentionally mixes classfile versions: the v3 layer is compiled for Java 25 and is only ever classloaded on servers running BM 3.x — on Java 21 servers those classes are never touched.

Server: Paper 1.21.x+. Denizen: 1.3.1+.
