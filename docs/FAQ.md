# FAQ

Common questions about DBetterModel 6.0. For the full surface, see [commands.md](commands.md), [events.md](events.md), [objects-and-tags.md](objects-and-tags.md), [compatibility.md](compatibility.md) and [EXPERIMENTAL.md](EXPERIMENTAL.md).

## Editor & tooling

### My editor shows "Unknown object return type bmmodeltag" (or red squiggles) on DBetterModel tags

That comes from the Denizen Script Checker (the VS Code Denizen extension), not from the server — the tag resolves and runs in-game. The checker only knows Denizen's built-in meta unless you point it at DBetterModel's.

Open VS Code Settings (`Ctrl+,`), search `denizenscript`, find **Denizenscript: Extra Sources**, and add:

```
https://github.com/meigoc/dbettermodel/archive/refs/heads/main.zip
```

Reload the window. DBetterModel 6.0 ships ObjectType meta for `bmentity@`, `bmmodel@` and `bmbone@`, so once that source is loaded the return types resolve cleanly.

## Animations

### Can I layer two animations — an 'idle' on the whole model plus another on just a couple of bones — without them resetting each other?

Yes. That is what the `bones:` argument on `bmstate` is for. Play the base animation on the whole model, then play the second one limited to specific bones — the bone-filtered animation takes over only those bones while the base keeps playing on the rest.

```
- bmstate entity:<[e]> model:demon_knight state:idle loop:loop
- bmstate entity:<[e]> model:demon_knight state:guard bones:right_arm|left_arm loop:loop
```

Caveat: on BetterModel 2.0.x/2.1.x, `bones:` filters apply to the whole model (the `bone_filter_animate` capability needs BM 1.15.x or 2.2.0+/3.x). DBetterModel logs a startup warning in that reduced mode. This is BetterModel's own limit — real per-bone override arrived in 2.2.0.

### My animations aren't smooth — should I set lerp_duration?

`lerp_duration` sets the transition (interpolation) time when switching animation states; a small value like 1–2 ticks smooths the blend between states. It defaults to 1 tick. It helps abrupt state changes, but it is not a fix for choppiness caused by low server TPS, distance, or an entity that isn't being ticked or viewed — check those too. Do not set it very high, or the model visibly floats between poses.

### I made an animation and want to run script logic at a specific keyframe

This is the signal system. Put an instruction keyframe `denizen:name{key=value}` on the animation's Instructions (Effects) track; it fires the `bm animation signal` event with `<context.name>`, `<context.metadata>` (a MapTag) and `<context.model>`. See [events.md](events.md).

```yaml
strike:
    type: world
    events:
        after bm animation signal name:hammer_hit:
            - hurt <context.metadata.get[damage]> <context.model.bm_entity.base_entity.location.find_entities.within[3].first||null>
```

If several keyframes on the Instructions track share a name prefix, BetterModel currently keeps only the last one — put those triggers in the Particle track's "Script" field instead, where they fire in sequence. To start a whole task script straight from a keyframe, see the experimental keyframe script bridge in [EXPERIMENTAL.md](EXPERIMENTAL.md).

## Hitboxes & interaction

### I have a BetterModel entity but player.target returns an armor stand / the display entity — how do I detect a click or gaze?

A model is rendered on a base entity, and its bones carry their own hitboxes; a raytrace or target hits those, not the logical entity. For clicks and attacks on the model, use the `bm hitbox interacted` and `bm hitbox damaged` events — both give you `<context.model>` — rather than `player.target`.

If you need the carrier entity, resolve it from the model:

```
- define carrier <context.model.bm_entity.base_entity>
```

There is no built-in "is looking at" tag. For gaze, raytrace against the base entity yourself.

## Bones & effects

### I want a bone effect: when an animation plays, read the bone's scale, and if it isn't zero, spawn an effect at the bone

Two things matter here. First, `<BMBoneTag.scale>` reflects the scale DBetterModel last set through a mechanism, not the scale an animation drives frame by frame — so polling it never sees animation-driven values. Second, the clean way to fire "at the right frame" is a signal keyframe at that exact moment: put `denizen:effect{...}` on the Instructions track, then in `bm animation signal` read the bone's world position and play your effect there.

```yaml
bone_effect:
    type: world
    events:
        after bm animation signal name:effect:
            - playeffect effect:flame at:<context.model.bone[head].world_location> quantity:5
```

This ties the effect to the animation timeline precisely, with no polling.

### Can I make only the head follow the player's look direction, independent of the body?

Not through a built-in option. BetterModel currently locks a model's head yaw to its body yaw; there is no engine setting to rotate body and head independently (a known BetterModel limitation), and no DBetterModel command that decouples them.

As a workaround you can drive a head bone yourself each tick with the `rotate` mechanism (a `QuaternionTag`) based on the player's yaw and pitch, though it competes with the engine's own rotation.

## Player models

### Can I set a view range for a player (limb) animation model?

`view_range` is a bone mechanism, and player limb models expose their bones through `<PlayerTag.limb_bones[<model>]>` (bone name → BMBoneTag). Get the bones and adjust `view_range` on them:

```
- foreach <player.limb_bones[steve]> as:bone:
    - adjust <[bone]> view_range:64
```

## Mounting

### bmmount says the bone is not a seat / has no hitbox

To be mountable, a bone must be a seat with a hitbox. In Blockbench, give the bone a leading `p` (for example `p_seat`) so BetterModel treats it as a seat and gives it a hitbox, then pass that bone to `bmmount`. A bone without the `p` tag cannot carry a passenger.

## Skins

### How does BetterModel put a player-skin texture on a model — MineSkin, a core shader, or something else?

It basically works using 'tint indices'. The resource pack contains blank JSON 3D models for all 12 body parts. Inside those JSON files, instead of a normal texture, each polygon is assigned a specific tint index number.

On the server side, the DynamicUV library reads the player's skin image, extracts the exact RGB colors for those specific pixels, and packs them into an array. It then applies that color array directly to the item using the new 1.21.4 item data components. The client simply takes that array and 'paints by numbers' onto the tint indices of the model.

(btw the resource pack is still required for this. The server only sends the color array, so without the pack providing those blank .json models with the tint indices, your client won't know what shape to paint and you'll just get a missing texture.)

DBetterModel exposes this through the `bmpart` command and the `skin` bone mechanism — you never touch the skin pipeline directly:

```
- adjust <[bone]> skin:[part=head;from=<player>]
```

### I applied a skin part but the bone shows a default/wrong skin, not the player's real one

DBetterModel resolves the source player through BetterModel's profile supplier, which is the hook SkinsRestorer plugs into — so a SkinsRestorer-set skin is used even on an offline-mode server. If you still get a default skin, the profile genuinely resolved to nothing: the player has no SkinsRestorer skin set and no real Mojang profile (pure offline with no skin plugin), or the lookup failed.

Fixes: install SkinsRestorer and set a skin for the source player (`/skin`), or run the server in online mode so real Mojang profiles are available.
