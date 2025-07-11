DBetterModel
---------

**DBetterModel: Adds interop between BetterModel and Denizen!**

# Docs

Supports BetterModel v1.9.0

-----

## Commands

### BMModel

  * **Name:** `BMModel`
  * **Syntax:** `bmmodel [entity:<entity>] [model:<model>] (remove)`
  * **Short:** Adds or removes a model from an entity.
  * **Required:** 2
  * **Group:** DBetterModel
  * **Usage:**
      * To add a model to an entity:
        ```yaml
        - bmmodel entity:<context.entity> model:my_model
        ```
      * To remove a model from an entity:
        ```yaml
        - bmmodel entity:<context.entity> model:my_model remove
        ```

### BMState

  * **Name:** `BMState`
  * **Syntax:** `bmstate [entity:<entity>] [state:<animation>] (loop:<once|loop|hold>) (speed:<#.#>) (remove)`
  * **Short:** Plays a state on a bmentity.
  * **Description:** Plays a state on a bmentity. If multiple models are on the entity, it will affect the first one loaded.
  * **Required:** 2
  * **Group:** DBetterModel
  * **Usage:**
      * To play an animation on an entity's model:
        ```yaml
        - bmstate entity:<context.entity> state:walk loop:loop speed:1.5
        ```
      * To stop an animation:
        ```yaml
        - bmstate entity:<context.entity> state:walk remove
        ```

-----

## Events

### bm finishes reload

  * **Events:** `bm finishes reload`
  * **Group:** DBetterModel
  * **Cancellable:** false
  * **Triggers:** when a BetterModel finishes reloading.
  * **Context:**
      * `<context.result>`: Returns the result of the reload.

### bm starts reload

  * **Events:** `bm starts reload`
  * **Group:** DBetterModel
  * **Cancellable:** false
  * **Triggers:** when a BetterModel starts reloading.

-----

## Object Types

### BMBoneTag

  * **Name:** `BMBoneTag`
  * **Prefix:** `bmbone`
  * **Base:** `ElementTag`
  * **Format:** The identity format is `<uuid>|<model_name>|<bone_id>`. For example: `bmbone@dfc67056-b15d-45dd-b239-482d92e482e5,dummy,head`.
  * **Description:** Represents a bone in a BMModel.

### BMEntityTag

  * **Name:** `BMEntityTag`
  * **Prefix:** `bmentity`
  * **Base:** `ElementTag`
  * **Format:** The identity format is the UUID of the base entity. For example: `bmentity@dfc67056-b15d-45dd-b239-482d92e482e5`.
  * **Description:** Represents an entity that has one or more models on it.

### BMModelTag

  * **Name:** `BMModelTag`
  * **Prefix:** `bmmodel`
  * **Base:** `ElementTag`
  * **Format:** The identity format is `<uuid>,<model_name>`. For example: `bmmodel@dfc67056-b15d-45dd-b239-482d92e482e5,dummy`.
  * **Description:** Represents a model that is attached to an entity.

-----

## Tags

  * **`<EntityTag.bm_entity>`**

      * **Returns:** `BMEntityTag`
      * **Description:** Returns the BMEntity of the entity, if any.

  * **`<BMEntityTag.base_entity>`**

      * **Returns:** `EntityTag`
      * **Description:** Returns the base Bukkit entity.

  * **`<BMEntityTag.model[<model_name>]>`**

      * **Returns:** `BMModelTag`
      * **Description:** Returns the model with the specified name on the entity.

  * **`<BMModelTag.bm_entity>`**

      * **Returns:** `BMEntityTag`
      * **Description:** Returns the bmentity of the model.

  * **`<BMModelTag.name>`**

      * **Returns:** `ElementTag`
      * **Description:** Returns the name of the model.

  * **`<BMModelTag.bones>`**

      * **Returns:** `MapTag(BMBoneTag)`
      * **Description:** Returns a map of all the bones of the model, with the bone ID as the key and the bone object as the value.

  * **`<BMModelTag.bone[<id>]>`**

      * **Returns:** `BMBoneTag`
      * **Description:** Returns the bone with the specified ID of the model.

-----

## Mechanisms

### item

  * **Object:** `BMBoneTag`
  * **Input:** `ListTag`
  * **Description:** Sets the item that the bone uses. Input is a `ListTag` containing an `ItemTag`, and optionally a `LocationTag` for local offset.
  * **Example:**
    ```yaml
    - adjust <[bone]> item:<[stick|l@0,0.5,0]>
    ```
