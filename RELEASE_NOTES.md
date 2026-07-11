<div align="center">

<img src="https://cdn.meigo.pw/dbettermodel-omni.png" alt="DBetterModel Omni" width="600" />

**DBetterModel 6.0 "Omni": one jar for every BetterModel (1.15.x, 2.x, 3.x).**

</div>

One jar for every BetterModel. 6.0 detects the installed BetterModel at startup and selects the matching compat layer — the per-BM-version release matrix of 5.x is gone.

| BetterModel | DBetterModel |
|---|---|
| 1.15.x / 2.0.x–2.2.x / 3.x | 6.0.0 (this jar) |

**Breaking changes: none.** Every 5.0 script runs unchanged.

## Highlights

- **Universal compat core** — three statically compiled layers (v1/v2/v3), selected by BetterModel version at startup. Public BM API only, no private-field reflection. Future BM 4.x gets a best-effort probe instead of a crash.
- **Capability system** — scripts query what the running BetterModel supports via `<bm.capabilities>`; unavailable features report clean errors, never stacktraces.
- **Blockbench keyframe signals** — `denizen:name{key=value}` instruction keyframes fire the `bm animation signal` event on all supported BM lines, including 1.15.x and 2.x.
- **`bmsummon`** — location-bound models (dummy trackers) for props and cutscenes.
- **Read/write bone API** — 5.x write-only display state (glow, tint, billboard, view range, brightness, scale, offset, item) now reads back.
- **Legacy aliases restored** — `world_rotation`, bone `bm_entity`, `lerp_frames` — pre-5.0 scripts work again.

## New in the Denizen surface

- **Command**: `bmsummon [model:<model> location:<location>] / [handle:<bmmodel> remove]` (with `save`-entry `summoned_model`).
- **Server tags**: `<bm.version>`, `<bm.compat_layer>`, `<bm.capabilities>`, `<bm.models>`, `<bm.limbs>`.
- **Events** (joining the existing `bm starts/finishes reload`):
  `bm animation signal`, `bm player animation signal`, `bm animation starts`, `bm animation ends`, `bm model spawns for player`, `bm model despawns for player`, `bm tracker created`, `bm tracker closed`, `bm hitbox damaged`, `bm hitbox interacted`, `bm model mounted`, `bm model dismounted`.
- **Mechanism**: `skin` on BMBoneTag — `- adjust <[bone]> skin:[part=head;from=<player>]`, the modern take on `bmpart` (which stays supported).
- **Tags**: bone getters (`glow`, `glow_color`, `tint`, `billboard`, `view_range`, `brightness`, `shadow_radius`, `scale`, `offset`, `item`), `<BMBoneTag.skin_parts>`, `<BMModelTag.viewers>`, `<BMModelTag.running_animations>`, `<context.model>` on signal events, `bmmodel@dummy:<id>,<model>` identity form for summoned models.

## Experimental (see [docs/EXPERIMENTAL.md](https://github.com/meigoc/DBetterModel/blob/main/docs/EXPERIMENTAL.md))

- **Keyframe script bridge** — `denizen:run{script=knight_strike;damage=8}` on a Blockbench keyframe starts the named task script with the metadata as definitions. Toggle: `experimental.keyframe-script-bridge`.
- **Folia readiness** — all internals are region-safe and the jar declares `folia-supported: true`. Waiting on Denizen's own Folia support to matter on vanilla Folia; works today on force-load forks.

## Fixed 5.x bugs

- Bone mechanisms fired their fulfill twice when applied via BMModelTag — now exactly once.
- `rotate` mechanism accumulated rotation modifiers on repeated writes — now replaces.
- Debug errors could be swallowed when the debug flag was off — errors always reach the console now.
- Update checker rewritten: proper JSON parsing of `/releases/latest`, failures logged instead of ignored.

## DBetterModel vs denizen-utilities (BetterModel bridge)

Both bridge BetterModel into Denizen. Verified against denizen-utilities 2.8.x (BetterModel bridge, compiled against `bettermodel-bukkit-api:3.0.1`):

| | DBetterModel 6.0 | denizen-utilities |
|---|---|---|
| BetterModel support | 1.15.x + 2.x + 3.x, one jar | 3.x only |
| BM API access | public API only | reflection into a BM private field (`RenderedBone.itemStack` via `getDeclaredField`/`setAccessible`) |
| Off-thread work | Bukkit/region schedulers | a raw `new Thread` with `Thread.sleep(100)` |
| Keyframe signals (`bm animation signal`) | fire on all BM lines, incl. 1.15/2.x | 3.x only |
| Location-bound models (dummy trackers) | yes — `bmsummon` | no |
| Skin part → bone | `bmpart` command **and** `skin` mechanism, resolved via BetterModel's tracked profile | `.skin` mechanism (their `bmpart` is deprecated) |
| Mount / billboard | commands (`bmmount`, `bmboard`) | mechanisms (`.mount`, `.billboard`) |
| Compat safety | build fails if `:core` touches a BM class; per-version layers | single build against one BM API |

denizen-utilities is a capable, actively documented bridge; the table lists only differences that are verifiable in its source, not a quality judgement.

## Testing

Verified on **five automated headless stands + one manual in-game session**, all green.

**Automated** — `tests/minecraft/test-headless.dsc` (console-driven, spawns its own rig, `DBM-RESULT OK/FAIL` line):

| BM | Layer | Server / Java | Result |
|---|---|---|---|
| 3.2.0 | v3 | Leaf 26.2 / Java 25 | 19/19 |
| 3.0.1 | v3 | Leaf 26.1.2 / Java 25 | 19/19 |
| 2.2.0 | v2 | Paper 1.21.10 / Java 21 | 19/19 |
| 2.0.1 | v2 (reduced) | Paper 1.21.4 / Java 21 | 19/19 |
| 1.15.2 | v1 | Paper 1.21.10 / Java 21 | 19/19 |

**Manual** — `tests/minecraft/test.dsc` (in-game, visual + event checks): BetterModel 3.2.0 · Denizen 1.3.3-SNAPSHOT (build 7290-DEV) · SkinsRestorer 15.12.4 · Leaf 26.2 (build 17) · DBetterModel 6.0.0. All commands, tags, mechanisms, per-player animation events and keyframe signals confirmed.

## Requirements

Java 21+ (Java 25 only when running BetterModel 3.x — BM 3.x itself requires it) · Paper 1.21.x+ · Denizen 1.3.1+.

Docs: [wiki.meigo.pw/docs/dbettermodel](https://wiki.meigo.pw/docs/dbettermodel) and the in-repo [`docs/`](https://github.com/meigoc/DBetterModel/tree/main/docs) folder.
