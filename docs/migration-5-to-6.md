# Migration: 5.0 → 6.0

**Nothing to migrate.** Every 5.0 script runs unchanged — same commands, same object types, same tags and mechanisms, same event names. Drop in the 6.0 jar and go.

The only operational change: you no longer pick a DBM release to match your BetterModel version. 6.0 supports BetterModel 1.15.x, 2.x and 3.x from one jar (see [compatibility.md](compatibility.md)).

## What's new in 6.0

- `bmsummon` command — location-bound models (dummy trackers).
- Server tags: `<bm.version>`, `<bm.compat_layer>`, `<bm.capabilities>`, `<bm.models>`, `<bm.limbs>`.
- 10 new script events: `bm animation signal`, `bm player animation signal`, `bm model spawns/despawns for player`, `bm tracker created/closed`, `bm hitbox damaged`, `bm hitbox interacted`, `bm model mounted/dismounted`.
- Getter tags on bones (glow, glow_color, tint, billboard, view_range, brightness, shadow_radius, scale, offset, item) and on models (`viewers`, `running_animations`) — 5.x was write-only here. Getters return the last value set through DBetterModel.
- `bmmodel@dummy:<id>,<model>` identity form for summoned models (non-persistent).

## Restored legacy aliases (pre-5.0 scripts)

If you still run scripts written for DBM 2.x–4.x, these aliases work again in 6.0, documented as deprecated:

| Alias | Replaced by | History |
|---|---|---|
| `<BMBoneTag.world_rotation>` | `<BMBoneTag.world_rotation_euler>` | renamed in 4.0.0 |
| `bmstate ... lerp_frames:<#>` (ticks) | `lerp_duration:<duration>` | replaced in 4.0.0; `lerp_duration` wins when both are given |
| `<BMBoneTag.bm_entity>` | `<BMBoneTag.bm_model.bm_entity>` | existed in 2.x, dropped in 4.x |
