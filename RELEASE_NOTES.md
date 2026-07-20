<div align="center">

**DBetterModel 6.1.0: BetterModel 3.3.0 support.**

</div>

A maintenance release that compiles the v3 compat layer against **BetterModel 3.3.0** while keeping the single universal jar working on every earlier BM line. No script, command, tag, or mechanism changed — every 6.0 script runs unchanged.

| BetterModel | DBetterModel |
|---|---|
| 1.15.x / 2.0.x–2.2.x / 3.0.x–3.3.x | 6.1.0 (this jar) |

**Breaking changes: none.**

## What changed

- **Build against BetterModel 3.3.0** — `bettermodel.v3.version` bumped 3.2.0 → 3.3.0, so the v3 layer compiles against the current API.
- **Migrated off the deprecated manager getters** — BM 3.3.0 refactored the per-manager accessors (`modelManager()`, `scriptManager()`, `skinManager()`, …) into a single `BetterModelPlatform#manager(Class)` and marked the old getters `@Deprecated` (removal candidates in a future major). The v3 layer now goes through the unified accessor.

## Why it needed a runtime fork, not a swap

BM 3.3.0 didn't just rename methods — it split the API:

- **3.2.0 and below**: only the named getters exist. There is no `Manager` supertype and no `manager(Class)` method.
- **3.3.0+**: `manager(Class)` is the real accessor; the named getters became deprecated delegates, slated for removal in 4.x.

DBetterModel ships **one jar** that selects a compat layer at startup and must keep running on 3.0.x–3.3.x (and best-effort 4.x). A static call to `manager(Class)` would `NoSuchMethodError` on a 3.2.0 server; relying only on the getters would break on 4.x once they're removed. So the new `V3Managers` helper resolves the accessor reflectively at load time — preferring `manager(Class)` on 3.3.0+ and falling back to the named getter on 3.2.0 — the same idiom the v2 layer already uses to bridge a BM API break (`V2HitboxLegacy`).

## Compatibility

- **BetterModel 3.3.0** — new, via the unified `manager(Class)` accessor.
- **BetterModel 3.0.x–3.2.x** — unchanged; served through the legacy-getter fallback.
- **BetterModel 1.15.x / 2.x** — untouched (v1/v2 layers).

## Testing

Verified on a **live in-game headless stand against BetterModel 3.3.0**, plus the unit suite, all green.

**Automated** — `tests/minecraft/test-headless.dsc` (console-driven, spawns its own rig, `DBM-RESULT OK/FAIL` line):

| BM | Layer | Server / Java | Result |
|---|---|---|---|
| 3.3.0 | v3 | Paper 26.2 / Java 25 | 19/19 |

Run against a real BetterModel 3.3.0 server with Denizen 1.3.3-SNAPSHOT (build 7290-DEV) and the stock `demon_knight` / `blue_wizard` / `steve` models. The console reported `Detected BetterModel 3.3.0 — using compat layer …V3Platform` — which prints only after the `V3Platform` constructor (and thus the migrated `manager(Class)` path) succeeds — followed by `DBM-RESULT OK=19 FAIL=0`, with no `NoSuchMethodError` or manager exceptions in the log.

**Unit** — `LayerSelectorTest`, `VersionsTest` green, including a new assertion that 3.3.0 resolves to the v3 layer. `./gradlew build importBan` passes: the v3 layer compiles against `bettermodel-*-api:3.3.0`, and the compat boundary check (`:core` must not touch a BM class) still holds.

**Unchanged lines** — the v1 (1.15.x) and v2 (2.x) layers are byte-for-byte unchanged from 6.0.0, and the v3 path for 3.0.x–3.2.x now runs through the legacy-getter fallback; their 6.0.0 headless results (19/19 each on 3.2.0, 3.0.1, 2.2.0, 2.0.1, 1.15.2) carry over.

## Requirements

Java 21+ (Java 25 only when running BetterModel 3.x — BM 3.x itself requires it) · Paper 1.21.x+ · Denizen 1.3.1+.

Docs: [wiki.meigo.pw/docs/dbettermodel](https://wiki.meigo.pw/docs/dbettermodel) and the in-repo [`docs/`](https://github.com/meigoc/DBetterModel/tree/main/docs) folder.
