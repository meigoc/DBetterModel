DBetterModel 3.0.0
---------

**DBetterModel 3.0.0: Adds interop between BetterModel and Denizen!**

# **The documentation is outdated; use the comments in the source code as documentation.**

# Docs

Supports BetterModel v1.10.1, Minecraft 1.21.4, Denizen 1.3.1 (7144)

-----



## Commands

### bmboard
**Syntax:** `bmboard entity:<entity> model:<model> bone:<bone> type:<fixed|vertical|horizontal|center>`  
**Required:** 4  
**Short:** Applies a billboard effect to a specific bone of a model.  
**Group:** DBetterModel  

**Description:**  
Makes a specific bone on a model always face the player.  
- `fixed`: Disables billboard effect.  
- `vertical`: The bone rotates on the Y-axis only.  
- `horizontal`: The bone rotates on the X and Z axes.  
- `center`: The bone rotates on all axes to face the player.  

Automatically sends an update packet so the change is visible instantly.

---

### bmlimb
**Syntax:** `bmlimb target:<player> model:<model_animator> animation:<animation_name> (loop:<once|loop|hold>)`  
**Required:** 3  
**Short:** Plays a player-specific animation.  
**Group:** DBetterModel  

**Description:**  
Plays a player animation from a model in the `player-animations` folder.  
The `loop` argument controls playback mode:  
- `once`: Plays the animation a single time (default).  
- `loop`: Repeats the animation indefinitely.  
- `hold`: Plays once and freezes on the final frame.  

To stop a looping or held animation, play another animation over it.

---

### bmmodel
**Syntax:** `bmmodel entity:<entity> model:<model> (remove)`  
**Required:** 2  
**Short:** Adds or removes a model from an entity.  
**Group:** DBetterModel  

**Description:**  
Adds or removes a specific model from an entity. Necessary for entities that can have multiple models.

**Tags:**  
- `<EntityTag.bm_entity>`  
- `<BMEntityTag.model[<name>]>`

---

### bmstate
**Syntax:** `bmstate entity:<entity> model:<model> state:<animation> (bones:<list>) (loop:<once|loop|hold>) (speed:<#.#>) (lerp_frames:<#>) (remove)`  
**Required:** 3  
**Short:** Plays or stops a layered animation state on a specific model on an entity.  
**Group:** DBetterModel  

**Description:**  
Plays or stops an animation state on a model attached to an entity. Supports layering by targeting specific bones.  
- `bones`: Optional list of bone names (pipe-separated).  
- If `bones` not provided, applies to entire model.  

## Events

### bm starts reload
**Group:** DBetterModel  
**Cancellable:** false  
**Triggers when** BetterModel starts reloading its configuration and models.

---

### bm finishes reload
**Group:** DBetterModel  
**Cancellable:** false  
**Triggers when** BetterModel finishes reloading its configuration and models.  

**Context:**  
- `<context.result>` returns whether the reload was `Success`, `Failure`, or `OnReload`.

## Object Types

### BMEntityTag
**Prefix:** `bmentity`  
**Base:** ElementTag  
**Format:** `bmentity@<uuid>`  
**Plugin:** DBetterModel  
**Description:** Represents an entity with one or more BetterModel models attached.

**Tags:**  
- `<BMEntityTag.base_entity>` → EntityTag (Returns the base Bukkit entity)  
- `<BMEntityTag.model[(<model_name>)]>` → BMModelTag (Returns the specified or first model on the entity)

---

### BMModelTag
**Prefix:** `bmmodel`  
**Base:** ElementTag  
**Format:** `bmmodel@<uuid>,<model_name>`  
**Plugin:** DBetterModel  
**Description:** Represents a specific model instance attached to an entity.

**Tags:**  
- `<BMModelTag.bm_entity>` → BMEntityTag (Returns the parent entity)  
- `<BMModelTag.name>` → ElementTag (Returns the model’s name)  
- `<BMModelTag.bones>` → MapTag (Map of bone names to BMBoneTag)  
- `<BMModelTag.bone[<name>]>` → BMBoneTag (Returns the specified bone)

## Properties

### `<EntityTag.bm_entity>`
**Returns:** BMEntityTag  
**Plugin:** DBetterModel  
**Description:** Returns the BMEntityTag of the entity, if it has any BetterModel models. Null if none.
