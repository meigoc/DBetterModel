<div align="center">

<img src="https://cdn.meigo.pw/dbettermodel-omni.png" alt="DBetterModel Omni" width="600" />

**DBetterModel 6.0 "Omni": one jar for every BetterModel (1.15.x, 2.x, 3.x).**

[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/plugin/dbettermodel)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/meigoc/dbettermodel)
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_vector.svg)](https://wiki.meigo.pw/docs/dbettermodel)

[![Lines of code](https://img.shields.io/endpoint?url=https%3A%2F%2Fghloc.vercel.app%2Fapi%2Fmeigoc%2Fdbettermodel%2Fbadge)](https://ghloc.vercel.app/meigoc/dbettermodel)

<a href="https://discord.gg/TKSU8HBTNX" rel="noopener nofollow ugc"><img src="https://wsrv.nl/?url=https%3A%2F%2Fdiscord.com%2Fapi%2Fguilds%2F1407721411237384222%2Fwidget.png%3Fstyle%3Dbanner2&amp;n=-1" alt=""></a>

</div>


* * *

# Demo:

https://github.com/user-attachments/assets/09d5f071-731a-4ad8-9b12-a571dd41f008

https://github.com/user-attachments/assets/f6b71e51-7977-44ac-b19f-adf491fd4687

* * *

# Why 6.0

Up to 5.0 I shipped one DBetterModel release per BetterModel version. BetterModel broke its API twice (1.x → 2.x → 3.x), so that matrix kept growing and every BM update stranded someone. 6.0 replaces it with a single universal jar: it detects the installed BetterModel at startup and picks the matching compat layer.

| BetterModel | DBetterModel |
|---|---|
| 1.15.x / 2.0.x–2.2.x / 3.x | **6.1.0 (single jar)** |

Historical table for pre-6.0 users:

| BetterModel Version | Latest Supported DBetterModel Version |
|----------------------|-------------------------------|
| 1.15.0 | [5.0.0](https://github.com/meigoc/DBetterModel/releases/tag/v5.0.0) |
| 1.14.0-1.14.2 | [4.3.0](https://github.com/meigoc/DBetterModel/releases/tag/v4.3.0) |
| 1.13.4 | [4.2.0](https://github.com/meigoc/DBetterModel/releases/tag/v4.2.0) |
| 1.13.0-1.13.1 | [4.1.0](https://github.com/meigoc/DBetterModel/releases/tag/v4.1.0) |
| 1.11.0 | [4.0.0](https://github.com/meigoc/DBetterModel/releases/tag/v4.0.0) |
| 1.10.3 | 3.5.0 (abandoned, will not be released) |
| 1.10.2 | [3.4.0](https://github.com/meigoc/DBetterModel/releases/tag/v3.4.0)  |
| 1.10.0-1.10.1 | [3.3.0](https://github.com/meigoc/DBetterModel/releases/tag/v3.3.0) |
| 1.9.0-1.9.3 | [2.1.0](https://github.com/meigoc/DBetterModel/releases/tag/v2.1.0) |
| 1.8.0-1.8.1 | [2.0.1](https://github.com/meigoc/DBetterModel/releases/tag/v2.0.1) |
| 1.5.5 | [1.1.0](https://github.com/Ignaacioo/DBetterModel/releases/tag/v1.1.0) |
| 1.5.1 | [1.0.0](https://github.com/Ignaacioo/DBetterModel/releases/tag/v1.0.0) |

# What's in 6.0

- **Universal compat core** — version-neutral API with three statically compiled layers (v1/v2/v3), selected at startup. No private-field reflection anywhere. Unknown future BM majors (4.x+) get a best-effort probe instead of a crash.
- **Capability system** — each layer declares what the running BetterModel can do; scripts query it with `<bm.capabilities>`. Anything unavailable reports a clean error instead of a stacktrace.
- **Blockbench keyframe signals** — `denizen:name{key=value}` instruction keyframes fire the `bm animation signal` Denizen event on **all** supported BM lines, including 1.15.x and 2.x.
- **`bmsummon`** — location-bound models (dummy trackers) for props and cutscenes, no carrier entity needed.
- **Full event suite** — 14 script events: signals, animation start/end, per-player spawn/despawn, tracker lifecycle, hitbox damage/interact, mount/dismount, reload.
- **Read/write bone API** — the tags that were write-only in 5.x (glow, tint, billboard, view range, brightness, scale, offset, item...) now read back too.
- **`skin` bone mechanism** — apply a player's skin part to a bone as an adjust (`skin:[part=head;from=<player>]`), the modern take on `bmpart`.
- **Legacy aliases restored** — `world_rotation`, bone `bm_entity`, `lerp_frames` work again, so 3.x/4.x-era scripts run as-is.
- **100% backward compatible** — every 5.0 script runs unchanged.
- **Experimental** — a keyframe-to-script bridge and Folia-ready internals; see [`docs/EXPERIMENTAL.md`](https://github.com/meigoc/DBetterModel/blob/main/docs/EXPERIMENTAL.md).

# DBetterModel vs denizen-utilities (BetterModel bridge)

Both bridge BetterModel into Denizen. Verified against denizen-utilities 2.8.x (compiled against `bettermodel-bukkit-api:3.0.1`); the table lists only source-verifiable differences, not a quality judgement.

| | DBetterModel 6.0 | denizen-utilities |
|---|---|---|
| BetterModel support | 1.15.x + 2.x + 3.x, one jar | 3.x only |
| BM API access | public API only | reflection into a BM private field (`RenderedBone.itemStack` via `setAccessible`) |
| Off-thread work | Bukkit/region schedulers | a raw `new Thread` + `Thread.sleep(100)` |
| Keyframe signals (`bm animation signal`) | fire on all BM lines, incl. 1.15/2.x | 3.x only |
| Location-bound models (dummy trackers) | yes — `bmsummon` | no |
| Skin part → bone | `bmpart` command **and** `skin` mechanism | `.skin` mechanism (their `bmpart` is deprecated) |
| Mount / billboard | commands (`bmmount`, `bmboard`) | mechanisms (`.mount`, `.billboard`) |

# Docs

- Wiki: [wiki.meigo.pw/docs/dbettermodel](https://wiki.meigo.pw/docs/dbettermodel)
- In-repo: the [`docs/`](https://github.com/meigoc/DBetterModel/tree/main/docs) folder mirrors the wiki (commands, events, objects & tags, compatibility, migration, experimental, FAQ).

# Editor setup (VS Code)

The Denizen extension's script checker doesn't know DBetterModel's object types out of the box, so it flags `bmentity@`/`bmmodel@`/`bmbone@` tags as unknown. Teach it: **Settings** (Ctrl+,) → search `denizenscript` → **Denizenscript: Extra Sources** → add

```
https://github.com/meigoc/dbettermodel/archive/refs/heads/main.zip
```

then reload the window. See the [FAQ](https://github.com/meigoc/DBetterModel/blob/main/docs/FAQ.md) for details.

# Requirements

| | |
|---|---|
| Java | 21+ (Java 25 only when running BetterModel 3.x — BM 3.x itself requires it) |
| Server | Paper 1.21.x+ |
| Denizen | 1.3.1+ |
| BetterModel | 1.15.x, 2.0.x–2.2.x, or 3.x |
