# Script Events

14 events, none cancellable. Events that carry a tracker share the `model:<name>` switch and the base context set: `<context.model>` (BMModelTag), `<context.model_name>`, `<context.entity>` (source EntityTag, if entity-bound), and on the tracker lifecycle events also `<context.dummy>` and `<context.location>`.

Capability gating: an event only fires if the active compat layer declares the listed capability (query with `<bm.capabilities>`). The table below shows which BetterModel lines fire each event.

| Event | Capability | BM 1.15.x | BM 2.x | BM 3.x |
|---|---|---|---|---|
| `bm animation signal` | `ANIMATION_SIGNALS` | ✔ | ✔ | ✔ |
| `bm player animation signal` | `PLAYER_ANIMATION_EVENTS` | ✔ | ✔ | ✔ |
| `bm animation starts` | `ANIMATION_LIFECYCLE_EVENTS` | ✖ | ✔ | ✔ |
| `bm animation ends` | `ANIMATION_LIFECYCLE_EVENTS` | ✖ | ✔ | ✔ |
| `bm model spawns for player` | `SPAWN_EVENTS` | ✔ | ✔ | ✔ |
| `bm model despawns for player` | `SPAWN_EVENTS` | ✔ | ✔ | ✔ |
| `bm tracker created` | — (always) | ✔ | ✔ | ✔ |
| `bm tracker closed` | — (always) | ✔ | ✔ | ✔ |
| `bm hitbox damaged` | `HITBOX_DAMAGE_EVENTS` | ✔ | ✔ | ✔ |
| `bm hitbox interacted` | `HITBOX_INTERACT_EVENT` / `HITBOX_INTERACT_AT_EVENT` | ✔ | ✔ | ✔ (at-variant only) |
| `bm model mounted` | `MOUNT_EVENTS` | ✔ | ✔ | ✔ |
| `bm model dismounted` | `MOUNT_EVENTS` | ✔ | ✔ | ✔ |
| `bm starts reload` | — (always) | ✔ | ✔ | ✔ |
| `bm finishes reload` | — (always) | ✔ | ✔ | ✔ |

## bm animation signal

Triggers when a Blockbench `denizen:` instruction keyframe or a per-player animation signal fires. Works on **all** supported BetterModel lines — 1.15.x and 2.x included, not just 3.x.

- **Switch**: `name:<name>` — only process the event if the signal name matches.
- **Context**:
  - `<context.name>` — the name of the signal.
  - `<context.metadata>` — MapTag of the `key=value` metadata of the instruction keyframe (empty for per-player signals).
  - `<context.player>` — the PlayerTag the signal targets, if it is a per-player signal.
  - `<context.model>` / `<context.model_name>` — the BMModelTag that emitted the signal. Available for `denizen:` keyframe signals on every BetterModel line; null for per-player signals (no BetterModel line attaches the emitting model to those).
- **Player**: linked when the signal is per-player.

### Keyframe format

Put an instruction keyframe on your Blockbench timeline with:

```
denizen:signal_name{key=value;key2=value2}
```

The part after `denizen:` is the signal name; the optional `{...}` block is parsed into `<context.metadata>`.

```yaml
footstep_sounds:
    type: world
    events:
        after bm animation signal name:footstep:
            - playsound <context.metadata.get[sound]> ...
```

### Known BetterModel quirk

If the Instructions track has multiple script keys whose names share an identical prefix, BetterModel may drop all but the last keyframe. Workaround: put your triggers in the "Script" field of keyframes on the **Particle** track instead — those are delivered reliably.

## bm player animation signal

Triggers when an animation signal targets a specific player (BetterModel's built-in `signal:` keyframes on player limb animations).

- **Switch**: `name:<name>`.
- **Context**:
  - `<context.name>` — the name of the signal.
  - `<context.metadata>` — MapTag of the signal metadata, if any.
  - `<context.model>` / `<context.model_name>` — always null here (see `bm animation signal`).
- **Player**: always.

## bm animation starts / bm animation ends

Trigger when an animation sequence starts or finishes playing on a model for a player. Requires BetterModel 2.0.1+ (`ANIMATION_LIFECYCLE_EVENTS` capability); never fires on the 1.15.x line.

- **Switches**: `model:<name>`, `animation:<name>`.
- **Context**:
  - `<context.model>` / `<context.model_name>` — the model whose animation started or ended.
  - `<context.animation>` — the animation name, when resolvable. BetterModel does not attach the name to the underlying event, so it is resolved best-effort from the tracker's running animation and can be null (the `animation:` switch then does not match) for animations started outside DBetterModel.
  - `<context.player>` — the PlayerTag the animation plays for.
- **Player**: always. BetterModel emits these only for **per-player** animation playback — `bmstate ... for_players:<list>` and the inherently per-player `bmlimb`. A plain `bmstate` that plays for everyone does **not** fire them (it isn't tracked per player), so don't expect these events from a normal whole-model animation.

```yaml
strike_damage:
    type: world
    events:
        after bm animation ends model:demon_knight animation:hammer_attack_1:
            - hurt 8 <player.location.find_entities.within[3].first||null>
```

## bm model spawns for player / bm model despawns for player

Trigger when a model becomes visible to (spawns for) or invisible to (despawns for) a player. Note: BetterModel has no global spawn event in any version — spawning is inherently per-player.

- **Switch**: `model:<name>`.
- **Context**: base tracker context, plus `<context.player>` — the PlayerTag the model spawned/despawned for.
- **Player**: always.

## bm tracker created / bm tracker closed

Trigger when a BetterModel tracker (an active model instance, entity-bound or dummy) is created or closed/removed.

- **Switch**: `model:<name>`.
- **Context**:
  - `<context.model>`, `<context.model_name>` — the tracker.
  - `<context.dummy>` — whether the tracker is a location-bound dummy (no source entity).
  - `<context.entity>` — the source EntityTag, if entity-bound.
  - `<context.location>` — the source entity's LocationTag, if entity-bound (the compat api exposes no tracker location, so dummies report null here).

## bm hitbox damaged

Triggers when a model hitbox takes damage. Informational only: the compat api callback is not cancellable and does not carry the bone or the damager.

- **Switch**: `model:<name>`.
- **Context**: base tracker context, plus `<context.damage>` — the damage amount.

## bm hitbox interacted

Triggers when a player interacts with a model hitbox.

- **Switch**: `model:<name>`.
- **Context**:
  - `<context.model>` — the interacted BMModelTag (may be missing when the layer cannot resolve the tracker).
  - `<context.model_name>`, `<context.entity>` — as usual, when resolved.
  - `<context.hand>` — the hand used (HAND/OFF_HAND).
- **Player**: always.
- The compat api callback does not carry the bone, so there is no bone context.

## bm model mounted / bm model dismounted

Trigger when an entity mounts or dismounts a model bone hitbox (seat).

- **Switch**: `model:<name>`.
- **Context**: base tracker context, plus:
  - `<context.bone>` — the seat BMBoneTag (entity-bound trackers only).
  - `<context.bone_name>` — the name of the seat bone.
  - `<context.passenger>` — the EntityTag that mounted/dismounted.

## bm starts reload

Triggers when BetterModel starts reloading its configuration and models.

## bm finishes reload

Triggers when BetterModel finishes reloading its configuration and models.

- **Context**:
  - `<context.result>` — 'Success', 'Failure', or 'OnReload' (same strings as 5.x).
