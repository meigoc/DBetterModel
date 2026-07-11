# Commands

All seven commands live in the `DBetterModel` group. When the active compat layer cannot do something, the command reports a clean error in the Denizen debug log — never a stacktrace, never silence. See [compatibility.md](compatibility.md) for what each BetterModel line supports.

## BMModel

Adds or removes a model from an entity. This is necessary for entities that can have multiple models.

- **Syntax**: `bmmodel entity:<entity> model:<model> (remove)`
- **Arguments**:

| Argument | Description |
|---|---|
| `entity:<entity>` | Target entity. |
| `model:<model>` | Model name. |
| `remove` | Removes the model instead of adding it. |

- **Examples**:
  - To add a model:
    `bmmodel entity:<context.entity> model:demon_knight`
  - To remove a model:
    `bmmodel entity:<context.entity> model:demon_knight remove`

## BMState

Plays or stops a layered animation state on a specific model on an entity, optionally limited to specific bones. This command supports multiple concurrent animations by allowing you to apply animations to specific parts of the model.

- **Syntax**: `bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_duration:<duration>) (lerp_frames:<#>) (for_players:<list_of_players>) (remove)`
- **Arguments**:

| Argument | Description |
|---|---|
| `bones:<list>` | Optional list of bone names to which this animation should be applied. If not provided, the animation applies to the entire model. This is the key to layering animations. |
| `loop` | Playback mode: `once` (plays one time, default), `loop` (repeats indefinitely), `hold` (plays once and freezes on the final frame). |
| `speed:<#.#>` | Playback speed multiplier. |
| `lerp_duration:<duration>` | Transition duration between animation states. |
| `lerp_frames:<#>` | Deprecated pre-4.0.0 alias of `lerp_duration`, interpreted as a tick count. When both are given, `lerp_duration` wins. |
| `for_players:<list_of_players>` | Only shows the animation to the specified players (official BetterModel per-player animation API). |
| `remove` | Stops the specified animation on the specified bones/players. |

- **Capability note**: on BetterModel 2.0.x/2.1.x, `bones:` filters apply to the whole model (`BONE_FILTER_ANIMATE` needs BM 1.15.x or 2.2.0+/3.x). DBM logs a startup warning when running in that reduced mode.
- **Examples**:
  - To play a looping 'walk' animation on specific leg bones:
    `bmstate entity:<context.entity> model:demon_knight state:walk bones:left_leg|right_leg loop:loop`
  - To play a one-shot 'hammer_attack_1' animation on the arm without stopping other animations:
    `bmstate entity:<context.entity> model:demon_knight state:hammer_attack_1 bones:right_arm`
  - To make the knight guard, but only for two players:
    `bmstate entity:<context.entity> model:demon_knight state:guard for_players:<[player_1]>|<[player_2]>`

## BMBillboard

Applies a billboard effect to a specific bone of a model, making it always face the player.

- **Syntax**: `bmboard entity:<entity> model:<model> bone:<bone> type:<fixed|vertical|horizontal|center>`
- **Types (`type`)**:

| Type | Behavior |
|---|---|
| `fixed` | Disables the billboard effect. |
| `vertical` | The bone rotates on the Y-axis only. |
| `horizontal` | The bone rotates on the X and Z axes. |
| `center` | The bone rotates on all axes to face the player. |

- The command automatically sends an update packet, so the change is visible instantly.
- **Example**:
  - To make a 'head' bone always face the player:
    `bmboard entity:<context.entity> model:demon_knight bone:head type:center`

## BMLimb

Plays a player-specific animation. These animations are sourced from models in the `player-animations` folder. To stop a looping or held animation, play another animation over it.

- **Syntax**: `bmlimb target:<player> model:<model_animator> animation:<animation_name> (loop:<once|loop|hold>) (hide:<player>)`
- **Arguments**:

| Argument | Description |
|---|---|
| `target:<player>` | Player to animate. |
| `model:<model_animator>` | Limb animator model (from `player-animations`). |
| `animation:<animation_name>` | Animation to play. |
| `loop` | `once` (default), `loop`, or `hold`. |
| `hide:<player>` | Makes all the target player's models invisible to this observer. The animation still plays for everyone else. |

- **Examples**:
  - To make a player perform a 'roll' animation:
    `bmlimb target:<player> model:steve animation:roll`
  - To start a repeating 'roll' animation:
    `bmlimb target:<player> model:steve animation:roll loop:loop`
  - To animate player_1, but hide their model from player_2:
    `bmlimb target:<[player_1]> model:steve animation:roll hide:<[player_2]>`

## BMPart

Applies a player's skin part (e.g., head, cape, body) to a specific bone of a model.

- **Syntax**: `bmpart entity:<entity> model:<model> bone:<bone> part:<part_name> from:<player>`
- **Arguments**:

| Argument | Description |
|---|---|
| `entity:<entity>` | Target entity to apply the skin part to. |
| `model:<model>` | Name of the BetterModel model attached to the entity. |
| `bone:<bone>` | Bone name within the model to map the skin part onto. |
| `part:<part_name>` | One of the `PlayerLimb` values (e.g., `head`, `body`, `left_arm`, `cape`). |
| `from:<player>` | The player whose skin will be used. |

- Dynamically maps the specified bone to a part of the player's skin. Uses BetterModel's skin manager and bone item mapper to handle fetching, caching, and rendering. Skin fetch failures are logged with the reason (5.x could fail silently here).
- See also the `skin` mechanism on BMBoneTag — the same operation as a bone adjust: `- adjust <[bone]> skin:[part=head;from=<player>]`.
- **Example**:
  - Apply a player's head to the knight's head bone:
    `bmpart entity:<[knight_entity]> model:demon_knight bone:head part:head from:<player>`

## BMMount

Mounts or dismounts an entity from a model's bone.

- **Syntax**: `bmmount [<entity_to_mount>] on:<bmbone> (dismount) (dismount_all)`
- **Arguments**:

| Argument | Description |
|---|---|
| `<entity_to_mount>` | Entity to mount (optional for `dismount_all`). |
| `on:<bmbone>` | Target bone (must be configured as a seat in the model file, e.g. tagged with 'p'). |
| `dismount` | Dismounts the specified entity from the bone. |
| `dismount_all` | Dismounts all entities from the bone. |

- **Examples**:
  - To make a player ride on the 'seat' bone of a model on an armor stand:
    `bmmount <player> on:<[stand].bm_entity.model[demon_knight].bone[seat]>`
  - To dismount all entities from the seat:
    `bmmount on:<[stand].bm_entity.model[demon_knight].bone[seat]> dismount_all`

## BMSummon (new in 6.0)

Summons a location-bound model (a BetterModel dummy tracker), or removes one. Useful for props and cutscenes — no carrier entity needed.

- **Syntax**: `bmsummon [model:<model> location:<location>] / [handle:<bmmodel> remove]`
- **Arguments**:

| Argument | Description |
|---|---|
| `model:<model>` | Model to summon. |
| `location:<location>` | Where to summon it. |
| `handle:<bmmodel>` | A previously saved summoned model, for the remove form. |
| `remove` | Removes the summoned model given by `handle:`. |

- **Save entry**: `<entry[saveName].summoned_model>` returns the BMModelTag of the summoned dummy tracker.
- **Capability**: requires `DUMMY_TRACKERS` — available on all three BetterModel lines (1.15.x, 2.x, 3.x).
- **Identity note**: dummy trackers have no source entity, so their BMModelTag identity uses the form `bmmodel@dummy:<id>,<model>`, where `<id>` is an internal registry id valid until the tracker is removed, BetterModel reloads, or the server restarts. Do not persist it in flags or files.
- **Examples**:
  - Summon a model and save its handle:
    ```yaml
    - bmsummon model:blue_wizard location:<player.location> save:prop
    - define prop <entry[prop].summoned_model>
    ```
  - Remove the summoned model later:
    `bmsummon handle:<[prop]> remove`
