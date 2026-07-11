# DBetterModel 6.0.0 live test suite.
# Usage: put a model into BetterModel, adjust the defines below, then run:
#   /ex run dbm_test
# Observation delay (seconds to look at each visual check, default 2):
#   /ex run dbm_test def.delay:10
# Mirror all output to the server console:
#   /ex run dbm_test def.delay:10 def.to_console:true
# The script spawns its own test entity and walks through every command,
# tag and mechanism, announcing what to look at. Auto-checkable things are
# counted as [OK]/[FAIL]; visual things are announced for the observer.

dbm_msg:
    type: task
    debug: false
    definitions: text
    script:
    - narrate <[text]>
    - if <server.flag[dbm_test_console]||false>:
        - announce to_console "[DBM-TEST] <[text].strip_color>"

dbm_test:
    type: task
    debug: false
    definitions: delay|to_console
    script:
    # ── config ──────────────────────────────────────────────
    - define model demon_knight
    - define model2 blue_wizard
    - define bone head
    - define anim walk
    - define seat_bone seat
    - define limb_model steve
    - define limb_anim roll
    - define obs <[delay]||2>
    - flag server dbm_test_console:<[to_console]||false>
    - define passed 0
    - define failed 0

    - run dbm_msg "def.text:<gold>=== DBetterModel 6.0.0 test suite (observation delay <[obs]>s) ==="

    # ── stage 1: server tags ────────────────────────────────
    - run dbm_msg "def.text:<yellow>[1] Server tags"
    - run dbm_msg "def.text:bm.version = <bm.version||null>"
    - run dbm_msg "def.text:bm.compat_layer = <bm.compat_layer||null>"
    - run dbm_msg "def.text:bm.capabilities = <bm.capabilities||null>"
    - run dbm_msg "def.text:bm.limbs = <bm.limbs||null>"
    - if <bm.compat_layer||null> != null:
        - define passed:++
        - run dbm_msg "def.text:<green>[OK] compat layer detected"
    - else:
        - define failed:++
        - run dbm_msg "def.text:<red>[FAIL] bm.compat_layer is null"
    - if <bm.models.contains[<[model]>]||false>:
        - define passed:++
        - run dbm_msg "def.text:<green>[OK] bm.models contains <[model]>"
    - else:
        - define failed:++
        - run dbm_msg "def.text:<red>[FAIL] bm.models does not contain <[model]> - fix the model define and rerun"
        - stop
    - wait <[obs]>s

    # ── stage 2: attach model (bmmodel) ─────────────────────
    - run dbm_msg "def.text:<yellow>[2] bmmodel - spawning a pig and attaching <[model]>"
    - spawn pig <player.location.forward[4]> save:mob
    - define mob <entry[mob].spawned_entity>
    - adjust <[mob]> has_ai:false
    - bmmodel entity:<[mob]> model:<[model]>
    - wait 1s
    - run dbm_msg "def.text:<gray>EXPECTED - the model appeared on the pig, plus tracker created event"
    - if <[mob].bm_entity||null> != null:
        - define passed:++
        - run dbm_msg "def.text:<green>[OK] EntityTag.bm_entity resolves"
    - else:
        - define failed:++
        - run dbm_msg "def.text:<red>[FAIL] EntityTag.bm_entity is null"
        - stop
    - define bmmodel <[mob].bm_entity.model[<[model]>]>
    - if <[bmmodel]||null> != null:
        - define passed:++
        - run dbm_msg "def.text:<green>[OK] model handle - <[bmmodel].name>"
    - else:
        - define failed:++
        - run dbm_msg "def.text:<red>[FAIL] model handle is null"
        - stop
    - wait <[obs]>s

    # ── stage 3: multi-model — second model on the SAME entity ──
    - run dbm_msg "def.text:<yellow>[3] multi-model - attaching <[model2]> to the same pig"
    - if <bm.models.contains[<[model2]>]||false>:
        - bmmodel entity:<[mob]> model:<[model2]>
        - wait 1s
        - run dbm_msg "def.text:<gray>EXPECTED - BOTH models visible on one pig (they overlap), plus tracker created event for <[model2]>"
        - define model2handle <[mob].bm_entity.model[<[model2]>]||null>
        - if <[model2handle]> != null:
            - define passed:++
            - run dbm_msg "def.text:<green>[OK] two models on one entity - <[bmmodel].name> + <[model2handle].name>"
        - else:
            - define failed:++
            - run dbm_msg "def.text:<red>[FAIL] second model handle is null"
        - wait <[obs]>s
        - bmmodel entity:<[mob]> model:<[model2]> remove
        - run dbm_msg "def.text:<gray>EXPECTED - <[model2]> removed, <[model]> still on the pig"
        - if <[mob].bm_entity.model[<[model]>]||null> != null:
            - define passed:++
            - run dbm_msg "def.text:<green>[OK] first model survived the removal of the second"
        - else:
            - define failed:++
            - run dbm_msg "def.text:<red>[FAIL] first model disappeared after removing the second"
        - wait <[obs]>s
    - else:
        - run dbm_msg "def.text:<gray>SKIP - no <[model2]> model installed (set the model2 define)"

    # ── stage 4: model tags ──────────────────────────────────
    - run dbm_msg "def.text:<yellow>[4] BMModelTag tags"
    - run dbm_msg "def.text:bones <[bmmodel].bones.size||FAIL> | animations <[bmmodel].animations||FAIL>"
    - run dbm_msg "def.text:duration of <[anim]> - <[bmmodel].get_animation_duration[<[anim]>]||FAIL>"
    - run dbm_msg "def.text:viewers - <[bmmodel].viewers||FAIL> (expected you)"
    - run dbm_msg "def.text:base_entity - <[mob].bm_entity.base_entity.entity_type||FAIL> (expected PIG)"
    - if <[bmmodel].bones.size||0> > 0:
        - define passed:++
        - run dbm_msg "def.text:<green>[OK] bones map filled"
    - else:
        - define failed:++
        - run dbm_msg "def.text:<red>[FAIL] bones map empty"
    - wait <[obs]>s

    # ── stage 5: bmstate ─────────────────────────────────────
    - run dbm_msg "def.text:<yellow>[5] bmstate - playing <[anim]> (loop), watch it for <[obs].mul[2]>s"
    - bmstate entity:<[mob]> model:<[model]> state:<[anim]> loop:loop
    - wait 1s
    - run dbm_msg "def.text:running_animations - <[bmmodel].running_animations||FAIL> (expected <[anim]>)"
    - if <[bmmodel].running_animations.contains[<[anim]>]||false>:
        - define passed:++
        - run dbm_msg "def.text:<green>[OK] animation is running"
    - else:
        - define failed:++
        - run dbm_msg "def.text:<red>[FAIL] running_animations does not report <[anim]>"
    - wait <[obs].mul[2]>s
    - bmstate entity:<[mob]> model:<[model]> state:<[anim]> remove
    - run dbm_msg "def.text:<gray>EXPECTED - animation stopped"
    - wait <[obs]>s
    - run dbm_msg "def.text:<gray>legacy alias check - bmstate with lerp_frames 2 (pre-4.0 syntax), plays <[anim]> once"
    - bmstate entity:<[mob]> model:<[model]> state:<[anim]> loop:once lerp_frames:2
    - wait <[obs]>s

    # ── stage 5b: per-player animation events (new in 6.0) ───
    # bm animation starts/ends fire ONLY for per-player playback (for_players:/bmlimb),
    # not for a whole-server bmstate. This stage triggers them for you specifically.
    - run dbm_msg "def.text:<yellow>[5b] per-player animation events - watch for [EVENT] animation starts/ends (BM 2.0.1+)"
    - if <bm.capabilities.contains[animation_lifecycle_events]||false>:
        - bmstate entity:<[mob]> model:<[model]> state:<[anim]> loop:once for_players:<player>
        - run dbm_msg "def.text:<gray>EXPECTED - [EVENT] animation starts, then [EVENT] animation ends (~<[bmmodel].get_animation_duration[<[anim]>]||1.5s> later), both for you"
        - wait <[obs].mul[2]>s
    - else:
        - run dbm_msg "def.text:<gray>SKIP - layer lacks animation_lifecycle_events (BM 1.15.x)"

    # ── stage 6: bone tags ───────────────────────────────────
    - run dbm_msg "def.text:<yellow>[6] BMBoneTag tags on bone <[bone]>"
    - define bmbone <[bmmodel].bone[<[bone]>]>
    - if <[bmbone]||null> == null:
        - define failed:++
        - run dbm_msg "def.text:<red>[FAIL] bone <[bone]> not found - fix the bone define, skipping bone stages"
    - else:
        - define passed:++
        - run dbm_msg "def.text:world_location - <[bmbone].world_location||FAIL>"
        - run dbm_msg "def.text:world_rotation_euler - <[bmbone].world_rotation_euler||FAIL>"
        - run dbm_msg "def.text:world_rotation (legacy alias) - <[bmbone].world_rotation||FAIL>"
        - run dbm_msg "def.text:local/global/real - <[bmbone].local_position||FAIL> / <[bmbone].global_position||FAIL> / <[bmbone].real_position||FAIL>"
        - run dbm_msg "def.text:is_visible - <[bmbone].is_visible||FAIL> | bm_entity (legacy alias) - <[bmbone].bm_entity||FAIL>"
        - wait <[obs]>s

        # ── stage 7: bone mechanisms + getter parity ─────────
        - run dbm_msg "def.text:<yellow>[7] Bone mechanisms - watch the <[bone]> bone"
        - run dbm_msg "def.text:<gray>EXPECTED - bone turns red"
        - adjust <[bmbone]> tint:16711680
        - wait <[obs]>s
        - run dbm_msg "def.text:<gray>EXPECTED - bone glows green"
        - adjust <[bmbone]> glow:true
        - adjust <[bmbone]> glow_color:65280
        - wait <[obs]>s
        - run dbm_msg "def.text:getter parity - tint=<[bmbone].tint||FAIL> (16711680) glow=<[bmbone].glow||FAIL> (true) glow_color=<[bmbone].glow_color||FAIL> (65280)"
        - if <[bmbone].glow||false>:
            - define passed:++
            - run dbm_msg "def.text:<green>[OK] getter tags read back written values"
        - else:
            - define failed:++
            - run dbm_msg "def.text:<red>[FAIL] glow getter did not read back"
        - run dbm_msg "def.text:<gray>EXPECTED - bone disappears, then returns"
        - adjust <[bmbone]> visible:false
        - wait <[obs]>s
        - adjust <[bmbone]> visible:true
        - run dbm_msg "def.text:<gray>EXPECTED - bone grows to double size, then back"
        - adjust <[bmbone]> scale:2,2,2
        - wait <[obs]>s
        - adjust <[bmbone]> scale:1,1,1
        - run dbm_msg "def.text:<gray>EXPECTED - bone shifts up half a block, then back"
        - adjust <[bmbone]> offset:0,0.5,0
        - wait <[obs]>s
        - adjust <[bmbone]> offset:0,0,0
        - run dbm_msg "def.text:<gray>parse-only - rotate/view_range/brightness/shadow_radius/interpolation_duration"
        - adjust <[bmbone]> rotate:0,0,0,1
        - adjust <[bmbone]> view_range:64
        - adjust <[bmbone]> brightness:15|15
        - adjust <[bmbone]> shadow_radius:0.5
        - adjust <[bmbone]> interpolation_duration:2t
        - run dbm_msg "def.text:getters - view_range=<[bmbone].view_range||FAIL> brightness=<[bmbone].brightness||FAIL> shadow_radius=<[bmbone].shadow_radius||FAIL> billboard=<[bmbone].billboard||FAIL> scale=<[bmbone].scale||FAIL> offset=<[bmbone].offset||FAIL>"
        - adjust <[bmbone]> brightness:0|15
        - run dbm_msg "def.text:<gray>EXPECTED - bone returns to normal color (tint/glow cleared)"
        - adjust <[bmbone]> tint:16777215
        - adjust <[bmbone]> glow:false
        - wait <[obs]>s

        # ── stage 8: bmboard ─────────────────────────────────
        - run dbm_msg "def.text:<yellow>[8] bmboard - walk around the pig, the <[bone]> bone should face you for <[obs].mul[3]>s"
        - bmboard entity:<[mob]> model:<[model]> bone:<[bone]> type:center
        - wait <[obs].mul[3]>s
        - bmboard entity:<[mob]> model:<[model]> bone:<[bone]> type:fixed
        - run dbm_msg "def.text:<gray>EXPECTED - bone stopped tracking you"
        - wait <[obs]>s

        # ── stage 9: bmpart ──────────────────────────────────
        - run dbm_msg "def.text:<yellow>[9] bmpart - your head skin onto bone <[bone]>"
        - bmpart entity:<[mob]> model:<[model]> bone:<[bone]> part:head from:<player>
        - run dbm_msg "def.text:<gray>EXPECTED - the bone shows your head (async skin load, give it a moment)"
        - wait <[obs].mul[2]>s

        # ── stage 9b: skin mechanism (new in 6.0) ─────────────
        - run dbm_msg "def.text:<yellow>[9b] skin mechanism - modern bmpart as a bone adjust"
        - run dbm_msg "def.text:skin_parts - <[bmbone].skin_parts||FAIL>"
        - if <[bmbone].skin_parts.contains[head]||false>:
            - define passed:++
            - run dbm_msg "def.text:<green>[OK] skin_parts tag lists head"
        - else:
            - define failed:++
            - run dbm_msg "def.text:<red>[FAIL] skin_parts tag missing or empty"
        - adjust <[bmbone]> skin:[part=head;from=<player>]
        - run dbm_msg "def.text:<gray>EXPECTED - the bone shows your head again, applied via adjust (async)"
        - wait <[obs].mul[2]>s

    # ── stage 10: bmmount ────────────────────────────────────
    - run dbm_msg "def.text:<yellow>[10] bmmount"
    - if <[bmmodel].bones.keys.contains[<[seat_bone]>]||false>:
        - spawn chicken <player.location.forward[2]> save:rider
        - define rider <entry[rider].spawned_entity>
        - adjust <[rider]> has_ai:false
        - bmmount <[rider]> on:<[bmmodel].bone[<[seat_bone]>]>
        - run dbm_msg "def.text:<gray>EXPECTED - chicken sits on bone <[seat_bone]>, plus model mounted event"
        - wait <[obs]>s
        - bmmount <[rider]> on:<[bmmodel].bone[<[seat_bone]>]> dismount
        - run dbm_msg "def.text:<gray>EXPECTED - chicken dismounted, plus model dismounted event"
        - wait <[obs]>s
        - remove <[rider]>
    - else:
        - run dbm_msg "def.text:<gray>SKIP - model has no <[seat_bone]> bone. Mounting needs a seat bone (name starting with p, e.g. p_seat) in the model file. Set the seat_bone define to test."

    # ── stage 11: bmsummon (dummy trackers) ──────────────────
    - run dbm_msg "def.text:<yellow>[11] bmsummon - model without an entity"
    - if <bm.capabilities.contains[dummy_trackers]||false>:
        - bmsummon model:<[model]> location:<player.location.forward[7]> save:dummy
        - wait 1s
        - define dummymodel <entry[dummy].summoned_model||null>
        - if <[dummymodel]> != null:
            - define passed:++
            - run dbm_msg "def.text:<green>[OK] summoned - <[dummymodel]> (bones <[dummymodel].bones.size||FAIL>)"
            - run dbm_msg "def.text:<gray>EXPECTED - a second model standing 7 blocks ahead, no entity under it"
            - wait <[obs].mul[2]>s
            - bmsummon handle:<[dummymodel]> remove
            - run dbm_msg "def.text:<gray>EXPECTED - it vanished, plus tracker closed event"
        - else:
            - define failed:++
            - run dbm_msg "def.text:<red>[FAIL] summoned_model save entry is null"
    - else:
        - run dbm_msg "def.text:<gray>SKIP - layer has no dummy_trackers capability"
    - wait <[obs]>s

    # ── stage 12: bmlimb ─────────────────────────────────────
    - run dbm_msg "def.text:<yellow>[12] bmlimb - player animation <[limb_anim]> from model <[limb_model]>"
    - if <bm.limbs.contains[<[limb_model]>]||false>:
        - bmlimb target:<player> model:<[limb_model]> animation:<[limb_anim]>
        - run dbm_msg "def.text:<gray>EXPECTED - YOUR player model performs <[limb_anim]> (press F5)"
        - wait 5t
        - run dbm_msg "def.text:player.limb - <player.limb[<[limb_model]>]||FAIL>"
        - wait <[obs].mul[2]>s
    - else:
        - run dbm_msg "def.text:<gray>SKIP - no <[limb_model]> in the players folder (installed limbs <bm.limbs||none>)"

    # ── stage 12b: keyframe signal note ──────────────────────
    # Signals need a 'denizen:name{k=v}' keyframe inside the model's animation.
    # If your test model carries one in the walk animation, the [EVENT] signal
    # lines (with the model context) appeared during stage 5 already.
    - run dbm_msg "def.text:<yellow>[12b] keyframe signal - if <[model]>'s walk has a denizen keyframe, [EVENT] signal lines with model context showed during stage 5"

    # ── stage 13: cleanup + force_update ─────────────────────
    - run dbm_msg "def.text:<yellow>[13] cleanup"
    - adjust <[bmmodel]> force_update
    - wait 1s
    - bmmodel entity:<[mob]> model:<[model]> remove
    - run dbm_msg "def.text:<gray>EXPECTED - model gone (pig visible again), plus tracker closed event"
    - wait <[obs]>s
    - remove <[mob]>
    - run dbm_msg "def.text:<gray>NOTE - bm animation starts/ends fire only for per-player playback (stage 5b), not for a whole-server bmstate like stage 5. bm animation signal fires from denizen: keyframes on any playback."

    # ── summary ──────────────────────────────────────────────
    - run dbm_msg "def.text:<gold>=== auto-checks <green><[passed]> OK<gold> / <red><[failed]> FAIL<gold> - visual stages confirm manually ==="
    - flag server dbm_test_console:!


# Announces every DBetterModel event as it fires. Remove or comment out
# after testing — spawn/despawn fire on every render-state change.
dbm_test_events:
    type: world
    debug: false
    events:
        on bm tracker created:
        - announce "<gold>[EVENT]<white> tracker created - <context.model_name> (dummy <context.dummy>)"
        on bm tracker closed:
        - announce "<gold>[EVENT]<white> tracker closed - <context.model_name>"
        on bm model spawns for player:
        - announce "<gold>[EVENT]<white> model <context.model_name> spawned for <player.name>"
        on bm model despawns for player:
        - announce "<gold>[EVENT]<white> model <context.model_name> despawned for <player.name>"
        on bm animation signal:
        - announce "<gold>[EVENT]<white> signal <context.name> meta <context.metadata> on model <context.model_name||null>"
        on bm player animation signal:
        - announce "<gold>[EVENT]<white> player signal <context.name> for <player.name>"
        on bm animation starts:
        - announce "<gold>[EVENT]<white> animation starts - <context.animation||null> on <context.model_name||null> for <player.name>"
        on bm animation ends:
        - announce "<gold>[EVENT]<white> animation ends - <context.animation||null> on <context.model_name||null> for <player.name>"
        on bm hitbox damaged:
        - announce "<gold>[EVENT]<white> hitbox damaged - <context.model_name> (damage <context.damage>)"
        on bm hitbox interacted:
        - announce "<gold>[EVENT]<white> hitbox interacted - <context.model_name> by <player.name> (<context.hand>)"
        on bm model mounted:
        - announce "<gold>[EVENT]<white> mounted - <context.passenger.entity_type||unknown> on bone <context.bone_name>"
        on bm model dismounted:
        - announce "<gold>[EVENT]<white> dismounted from bone <context.bone_name>"
        on bm starts reload:
        - announce "<gold>[EVENT]<white> BetterModel reload started"
        on bm finishes reload:
        - announce "<gold>[EVENT]<white> BetterModel reload finished - <context.result>"
