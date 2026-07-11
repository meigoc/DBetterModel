# Objects, Tags & Mechanisms

## Server tags: `<bm.*>` (new in 6.0)

| Tag | Returns | Description |
|---|---|---|
| `<bm.version>` | ElementTag | Version string of the installed BetterModel plugin. |
| `<bm.compat_layer>` | ElementTag | Active DBetterModel compat layer: `v1` (BetterModel 1.15.x), `v2` (2.x), or `v3` (3.x+). |
| `<bm.capabilities>` | ListTag | Capabilities the active layer provides on the running BetterModel, e.g. `dummy_trackers`, `animation_signals`. See [compatibility.md](compatibility.md). |
| `<bm.models>` | ListTag | Names of all models registered in BetterModel. |
| `<bm.limbs>` | ListTag | Names of all player limb models registered in BetterModel. |

Guard capability-dependent scripts like this:

```yaml
- if <bm.capabilities.contains[dummy_trackers]>:
    - bmsummon model:blue_wizard location:<[loc]>
```

## EntityTag / PlayerTag extensions

| Tag | Returns | Description |
|---|---|---|
| `<EntityTag.bm_entity>` | BMEntityTag | The BMEntityTag of the entity, if it has any BetterModel models. Null if none. |
| `<EntityTag.bm_model[<name>]>` | BMModelTag | The named model on the entity. The name is required. |
| `<PlayerTag.limb[(<model_name>)]>` | MapTag | The player's currently active "limb" animation (from `bmlimb`). Keys: `animation_name`, `loop_mode`, and `default_loop_mode` when the model defines one. Without a model name, uses the first-found active limb animator — specify the name for script stability. Null if nothing is playing. |
| `<PlayerTag.limb_bones[<model_name>]>` | MapTag | All bones of a specific player limb model: bone name → BMBoneTag. |

## BMEntityTag

Represents an entity that has one or more BetterModel models attached to it.

- **Prefix**: `bmentity`
- **Format**: the UUID of the base entity (e.g. `bmentity@dfc67056-b15d-45dd-b239-482d92e482e5`).

| Tag | Returns | Description |
|---|---|---|
| `<BMEntityTag.base_entity>` | EntityTag | The base Bukkit entity. |
| `<BMEntityTag.model[(<model_name>)]>` | BMModelTag | The named model on the entity; without a name, the first model loaded. |

## BMModelTag

Represents a specific model instance (tracker) attached to an entity — or, new in 6.0, bound to a location.

- **Prefix**: `bmmodel`
- **Format**: `<uuid>,<model_name>` (e.g. `bmmodel@dfc67056-...-482d92e482e5,demon_knight`).
- **Dummy identity form (new in 6.0)**: models summoned with `bmsummon` have no source entity, so they identify as `bmmodel@dummy:<id>,<model>`. The `<id>` is an internal registry id — valid until the tracker is removed, BetterModel reloads, or the server restarts. **Non-persistent**: do not store it in flags or files; re-acquire the handle from the `bmsummon` save entry.

| Tag | Returns | Description |
|---|---|---|
| `<BMModelTag.name>` | ElementTag | Name of the model. |
| `<BMModelTag.bm_entity>` | BMEntityTag | Parent BMEntityTag (null for dummies). |
| `<BMModelTag.bones>` | MapTag | All bones: name → BMBoneTag. |
| `<BMModelTag.bone[<name>]>` | BMBoneTag | The named bone. |
| `<BMModelTag.animations>` | ListTag | All animation names of the model. |
| `<BMModelTag.get_animation_duration[<name>]>` | DurationTag | Total duration of the animation. |
| `<BMModelTag.viewers>` | ListTag(PlayerTag) | Players currently seeing this model. *(new in 6.0)* |
| `<BMModelTag.running_animations>` | ListTag | Names of animations currently playing on the tracker pipeline. The compat api exposes at most one pipeline animation, so the list has zero or one entries. *(new in 6.0)* |

**Mechanisms**:

- `force_update` — forces an immediate visual update of the model for all viewers.
- Every BMBoneTag mechanism (below) also works on a BMModelTag, applying to all bones of the model at once.

## BMBoneTag

Represents a single bone within a specific model instance on an entity.

- **Prefix**: `bmbone`
- **Format**: `<uuid>,<model_name>,<bone_name>` (e.g. `bmbone@dfc67056-...-482d92e482e5,demon_knight,head`).

### Position/rotation tags

| Tag | Returns | Description |
|---|---|---|
| `<BMBoneTag.name>` | ElementTag | The bone's name. |
| `<BMBoneTag.world_location>` | LocationTag | The bone's world position (entity location + rotated model offset). |
| `<BMBoneTag.global_position>` | LocationTag | Alias of `world_location`. |
| `<BMBoneTag.local_position>` | LocationTag | The bone's position relative to the model, un-rotated. |
| `<BMBoneTag.real_position>` | LocationTag | World position with the bone's real view direction applied. |
| `<BMBoneTag.world_rotation_euler>` | LocationTag | The bone's world rotation as Euler angles (x/y/z). |
| `<BMBoneTag.world_rotation>` | LocationTag | **Deprecated** pre-4.0.0 alias of `world_rotation_euler`, restored in 6.0 for legacy scripts. |
| `<BMBoneTag.is_visible>` | ElementTag(Boolean) | Whether the bone is visible. |
| `<BMBoneTag.bm_model>` | BMModelTag | The parent model. |
| `<BMBoneTag.bm_entity>` | BMEntityTag | **Deprecated** 2.x alias (dropped in 4.x, restored in 6.0): the parent BMEntityTag. Null for bones of dummy trackers. Prefer `bm_model.bm_entity`. |

### Getter tags (new in 6.0)

BetterModel exposes no display-state reads, so these tags return the **last value set through DBetterModel** — or the documented default before the first write. Values set by other plugins or by the model file at runtime are not visible to them.

| Tag | Returns | Default before first write | Mechanism |
|---|---|---|---|
| `<BMBoneTag.glow>` | ElementTag(Boolean) | false | `glow` |
| `<BMBoneTag.glow_color>` | ElementTag(Number) | 16777215 (white) | `glow_color` |
| `<BMBoneTag.tint>` | ElementTag(Number) | 16777215 (neutral white) | `tint` |
| `<BMBoneTag.billboard>` | ElementTag | FIXED | `billboard` |
| `<BMBoneTag.view_range>` | ElementTag(Decimal) | 1.0 (display default) | `view_range` |
| `<BMBoneTag.brightness>` | ListTag | 0\|15 (block\|sky) | `brightness` |
| `<BMBoneTag.shadow_radius>` | ElementTag(Decimal) | 0 | `shadow_radius` |
| `<BMBoneTag.scale>` | LocationTag | the model's own item scale | `scale` |
| `<BMBoneTag.offset>` | LocationTag | the model's own item offset | `offset` |
| `<BMBoneTag.item>` | ItemTag | null | `item` |
| `<BMBoneTag.skin_parts>` | ListTag | valid part names for the `skin` mechanism | `skin` |

### Mechanisms

| Mechanism | Input | Description |
|---|---|---|
| `tint` | ElementTag(Integer) | RGB color tint of the bone's item (`<ColorTag.rgb_integer>`). |
| `visible` | ElementTag(Boolean) or ListTag | Visibility for all players, or `true\|<player>\|<player>...` for specific ones. |
| `item` | ItemTag | Item displayed by the bone. |
| `offset` | LocationTag | Local offset of the bone's item. |
| `scale` | LocationTag | Scale vector of the bone. |
| `rotate` | QuaternionTag | Additional rotation on the bone. Repeated writes replace the rotation instead of stacking (fixed 5.x accumulation bug). |
| `interpolation_duration` | DurationTag | Movement interpolation duration. |
| `glow` | ElementTag(Boolean) | Glowing on/off. |
| `glow_color` | ElementTag(Integer) | RGB glow color. |
| `brightness` | ElementTag(Integer) | Brightness level. |
| `view_range` | ElementTag(Decimal) | Render distance in blocks. |
| `shadow_radius` | ElementTag(Decimal) | Shadow radius. |
| `billboard` | ElementTag | Billboard mode (fixed/vertical/horizontal/center). |
| `skin` | MapTag | Applies a player's skin part to the bone: `skin:[part=head;from=<player>]`. Part names: `<BMBoneTag.skin_parts>`. The skin loads asynchronously, so the bone updates a moment later. Modern replacement for the `bmpart` command (which stays supported). Bone-only — not fanned out via BMModelTag. |

All bone mechanisms execute exactly once per adjust, even when fanned out over every bone of a model via BMModelTag (fixed 5.x double-fire bug).
