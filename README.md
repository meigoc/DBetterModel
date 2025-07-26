<div align="center">

<img width="145" height="157" alt="dbettermodel1" src="https://github.com/user-attachments/assets/ed2022f5-3a9d-44d2-90ec-e855702fc400" />

  
**DBetterModel 3.1.0: Adds interop between BetterModel and Denizen!**

[![CodeFactor](https://www.codefactor.io/repository/github/meigoc/DBetterModel/badge?style=flat-square)](https://www.codefactor.io/repository/github/meigoc/DBetterModel)
[![Total line](https://tokei.rs/b1/github/meigoc/DBetterModel?category=code&style=flat-square)](https://github.com/meigoc/DBetterModel)
[![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/meigoc/DBetterModel?style=flat-square&logo=github)](https://github.com/meigoc/DBetterModel/issues)

</div>


* * *

# Demo:

https://github.com/user-attachments/assets/09d5f071-731a-4ad8-9b12-a571dd41f008

https://github.com/user-attachments/assets/f6b71e51-7977-44ac-b19f-adf491fd4687

* * *


# Supported:
Supports BetterModel v1.10.1, Minecraft 1.21.4, Denizen 1.3.1 (7144)
| DBetterModel Version | BetterModel Version Supported |
|----------------------|-------------------------------|
| [3.1.0](https://github.com/meigoc/DBetterModel/releases/tag/v3.1.0) | 1.10.1 |
| [3.0.0](https://github.com/meigoc/DBetterModel/releases/tag/v3.0.0) | 1.10.0–1.10.1 |
| [2.1.0](https://github.com/meigoc/DBetterModel/releases/tag/v2.1.0) | 1.9.0–1.9.3 |
| [2.0.1](https://github.com/meigoc/DBetterModel/releases/tag/v2.0.1) | 1.8.0–1.8.1 |
| [2.0.0](https://github.com/meigoc/DBetterModel/releases/tag/v2.0)   | 1.9.0        |
| [1.1.0](https://github.com/Ignaacioo/DBetterModel/releases/tag/v1.1.0) | 1.5.5        |
| [1.0.0](https://github.com/Ignaacioo/DBetterModel/releases/tag/v1.0.0) | 1.5.1        |


# Docs (3.1.0)

-----

## Commands

### BMModel
Adds or removes a model from an entity. This is necessary for entities that can have multiple models.

- **Syntax**: `bmmodel entity:<entity> model:<model> (remove)`
- **Examples**:
  - To add a model:  
    `bmmodel entity:<context.entity> model:my_model`
  - To remove a model:  
    `bmmodel entity:<context.entity> model:my_model remove`

### BMState
Plays or stops a layered animation state on a specific model on an entity, optionally limited to specific bones. This command supports multiple concurrent animations by allowing you to apply animations to specific parts of the model.

- **Syntax**: `bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_frames:<#>) (remove)`
- **Arguments**:
  - `bones:<list>`: An optional list of bone names to which this animation should be applied. If not provided, the animation applies to the entire model.
  - `loop`: Sets the playback mode. It can be:
    - `once` (plays one time),
    - `loop` (repeats indefinitely),
    - `hold` (plays once and freezes on the final frame).
  - `remove`: A flag to stop the specified animation.
- **Examples**:
  - To play a looping 'walk' animation on specific leg bones:  
    `bmstate entity:<context.entity> model:robot state:walk bones:left_leg|right_leg loop:loop`
  - To play a one-shot 'shoot' animation on the arm without stopping other animations:  
    `bmstate entity:<context.entity> model:robot state:shoot bones:right_arm`

### BMBillboard
Applies a billboard effect to a specific bone of a model, making it always face the player.

- **Syntax**: `bmboard entity:<entity> model:<model> bone:<bone> type:<fixed|vertical|horizontal|center>`
- **Types (`type`)**:
  - `fixed`: Disables the billboard effect.
  - `vertical`: The bone rotates on the Y-axis only.
  - `horizontal`: The bone rotates on the X and Z axes.
  - `center`: The bone rotates on all axes to face the player.
- **Example**:
  - To make a 'head' bone always face the player:  
    `bmboard entity:<context.entity> model:my_mob bone:head type:center`

### BMLimb
Plays a player-specific animation. These animations are sourced from models in the `players` folder. To stop a looping or held animation, you can play another animation over it.

- **Syntax**: `bmlimb target:<player> model:<model_animator> animation:<animation_name> (loop:<once|loop|hold>)`
- **Modes (`loop`)**:
  - `once`: Plays the animation a single time (default).
  - `loop`: Repeats the animation indefinitely.
  - `hold`: Plays the animation once and freezes on the final frame.
- **Examples**:
  - To make a player perform a 'roll' animation:  
    `bmlimb target:<player> model:steve animation:roll`
  - To start a repeating 'dance' animation:  
    `bmlimb target:<player> model:player_gestures animation:dance loop:loop`
  - To make a player strike a pose and hold it:  
    `bmlimb target:<player> model:player_poses animation:heroic_pose loop:hold`

### BMPart

Applies a player's skin part (e.g., head, cape, body) to a specific bone of a model.

* **Syntax**: `bmpart entity:<entity> model:<model> bone:<bone> part:<part_name> from:<player>`
* **Arguments**:

  * `entity:<entity>`: Target entity to apply the skin part to.
  * `model:<model>`: Name of the BetterModel model attached to the entity.
  * `bone:<bone>`: Bone name within the model to map the skin part onto.
  * `part:<part_name>`: One of the `PlayerLimb` values (e.g., `head`, `body`, `left_arm`, `cape`).
  * `from:<player>`: The player whose skin will be used.
* **Description**:

  * Dynamically maps the specified bone to a part of the player's skin. Uses BetterModel's skin manager and bone item mapper to handle fetching, caching, and rendering.

* **Example**:

  * Apply Player's head to the statue's head bone:
    `bmpart entity:<[statue_entity]> model:statue_model bone:head part:head from:<player>`

## Events

### bm starts reload
- **Event**: `bm starts reload`
- **Triggers**: This event triggers when BetterModel starts reloading its configuration and models.

### bm finishes reload
- **Event**: `bm finishes reload`
- **Triggers**: This event triggers when BetterModel finishes reloading its configuration and models.
- **Context**:
  - `<context.result>`: Returns whether the reload was a 'Success', 'Failure', or 'OnReload'.

## Objects and Tags

### BMEntityTag
Represents an entity that has one or more BetterModel models attached to it.

- **Prefix**: `bmentity`
- **Format**: The identity format is the UUID of the base entity (e.g., `bmentity@dfc67056-b15d-45dd-b239-482d92e482e5`).
- **Tags**:
  - `<EntityTag.bm_entity>`: Returns the `BMEntityTag` of the entity, if it has any BetterModel models. It returns null if the entity has no models.
  - `<BMEntityTag.base_entity>`: Returns the base Bukkit entity as an `EntityTag`.
  - `<BMEntityTag.model[(<model_name>)]>`: Returns the `BMModelTag` for the specified model name on the entity. If no name is provided, it returns the first model loaded on the entity.

### BMModelTag
Represents a specific model instance attached to an entity.

- **Prefix**: `bmmodel`
- **Format**: The identity format is `<uuid>,<model_name>` (e.g., `bmmodel@dfc67056-b15d-45dd-b239-482d92e482e5,dummy`).
- **Tags**:
  - `<BMModelTag.name>`: Returns the name of the model.
  - `<BMModelTag.bm_entity>`: Returns the parent `BMEntityTag` of this model.
  - `<BMModelTag.bones>`: Returns a `MapTag` of all bones in the model, with the bone name as the key and the `BMBoneTag` as the value.
  - `<BMModelTag.bone[<name>]>`: Returns the `BMBoneTag` for the bone with the specified name from the model.

### BMBoneTag
Represents a single bone within a specific model instance on an entity.

- **Prefix**: `bmbone`
- **Format**: The identity format is `<uuid>,<model_name>,<bone_name>` (e.g., `bmbone@dfc67056-b15d-45dd-b239-482d92e482e5,dummy,head`).
- **Tags**:
  - `<BMBoneTag.name>`: Returns the name of the bone.
  - `<BMBoneTag.global_position>`: Returns the bone's current position in the world as a `LocationTag`.
  - `<BMBoneTag.is_visible>`: Returns an `ElementTag(Boolean)` indicating whether the bone is currently visible.
  - `<BMBoneTag.bm_model>`: Returns the parent `BMModelTag` of this bone.
- **Mechanisms**:
  - `tint:<color>`: Applies a color tint to the bone's item. The color is specified as a single integer representing the RGB value (e.g., red is 16711680). https://www.mathsisfun.com/hexadecimal-decimal-colors.html
  - **visible** `<ElementTag(Boolean)>` or `<ListTag>`
      - Sets the visibility of the bone for everyone (`Boolean`) or for specific players (`ListTag` with first element boolean and subsequent PlayerTags).
  - **item** `<ListTag>`
      - Sets the item displayed by this bone. First element must be an `ItemTag`; optional second element is a `LocationTag` for local offset.
 
