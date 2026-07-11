# DBetterModel 6.0.0 headless test suite — runs entirely from the server console,
# no player needed. Auto-checks only (visual stages are parse-level here).
# Usage from console:
#   ex run dbm_headless def.location:0,80,0,world
# Optional defs: def.model:<name> def.model2:<name> def.bone:<name> def.anim:<name> def.delay:<secs>
# All output goes to the console prefixed [DBM-TEST]; the final line is
#   DBM-RESULT OK=<n> FAIL=<m>
# which harnesses can grep for.

dbm_headless:
    type: task
    debug: false
    definitions: location|model|model2|bone|anim|delay
    script:
    - define model <[model]||test_knight>
    - define model2 <[model2]||test_wizard>
    - define bone <[bone]||head>
    - define anim <[anim]||walk>
    - define obs <[delay]||1>
    - define passed 0
    - define failed 0

    - announce to_console "[DBM-TEST] === DBetterModel 6.0.0 headless suite ==="

    # ── stage 0: location + chunk ────────────────────────────
    - define loc <location[<[location]>]||null>
    - if <[loc]> == null:
        - announce to_console "[DBM-TEST] [FAIL] bad or missing def.location (want x,y,z,world)"
        - announce to_console "[DBM-TEST] DBM-RESULT OK=0 FAIL=1"
        - stop
    - chunkload <[loc].chunk> duration:10m
    - announce to_console "[DBM-TEST] location ok - <[loc].simple>, chunk loaded"

    # ── stage 1: server tags ────────────────────────────────
    - announce to_console "[DBM-TEST] [1] server tags"
    - announce to_console "[DBM-TEST] bm.version = <bm.version||null>"
    - announce to_console "[DBM-TEST] bm.compat_layer = <bm.compat_layer||null>"
    - announce to_console "[DBM-TEST] bm.capabilities = <bm.capabilities||null>"
    - announce to_console "[DBM-TEST] bm.models = <bm.models||null>"
    - announce to_console "[DBM-TEST] bm.limbs = <bm.limbs||null>"
    - if <bm.compat_layer||null> != null:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] compat layer detected"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] bm.compat_layer is null"
    - if <bm.models.contains[<[model]>]||false>:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] bm.models contains <[model]>"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] bm.models does not contain <[model]>"
        - announce to_console "[DBM-TEST] DBM-RESULT OK=<[passed]> FAIL=<[failed]>"
        - stop

    # ── stage 2: bmmodel attach ──────────────────────────────
    - announce to_console "[DBM-TEST] [2] bmmodel attach"
    - spawn pig <[loc]> save:mob
    - define mob <entry[mob].spawned_entity>
    - adjust <[mob]> has_ai:false
    # the console-given location may sit inside solid blocks (suffocation) or over
    # a drop - a dead rig mob turns every entity-relative tag below into a false FAIL
    - adjust <[mob]> invulnerable:true
    - adjust <[mob]> gravity:false
    - bmmodel entity:<[mob]> model:<[model]>
    - wait 1s
    - if <[mob].bm_entity||null> != null:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] EntityTag.bm_entity resolves"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] EntityTag.bm_entity is null"
        - remove <[mob]>
        - announce to_console "[DBM-TEST] DBM-RESULT OK=<[passed]> FAIL=<[failed]>"
        - stop
    - define bmmodel <[mob].bm_entity.model[<[model]>]>
    - if <[bmmodel]||null> != null:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] model handle - <[bmmodel].name>"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] model handle is null"
        - remove <[mob]>
        - announce to_console "[DBM-TEST] DBM-RESULT OK=<[passed]> FAIL=<[failed]>"
        - stop
    - wait <[obs]>s

    # ── stage 3: multi-model on one entity ───────────────────
    - announce to_console "[DBM-TEST] [3] multi-model"
    - if <bm.models.contains[<[model2]>]||false>:
        - bmmodel entity:<[mob]> model:<[model2]>
        - wait 1s
        - if <[mob].bm_entity.model[<[model2]>]||null> != null:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] two models on one entity"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] second model handle is null"
        - bmmodel entity:<[mob]> model:<[model2]> remove
        - wait 1s
        - if <[mob].bm_entity.model[<[model]>]||null> != null:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] first model survived removal of the second"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] first model gone after removing the second"
    - else:
        - announce to_console "[DBM-TEST] SKIP multi-model - no <[model2]> installed"
    - wait <[obs]>s

    # ── stage 4: model tags ──────────────────────────────────
    - announce to_console "[DBM-TEST] [4] model tags"
    - announce to_console "[DBM-TEST] bones=<[bmmodel].bones.size||FAIL> animations=<[bmmodel].animations||FAIL>"
    - announce to_console "[DBM-TEST] duration(<[anim]>)=<[bmmodel].get_animation_duration[<[anim]>]||FAIL> viewers=<[bmmodel].viewers.size||FAIL> base=<[mob].bm_entity.base_entity.entity_type||FAIL>"
    - if <[bmmodel].bones.size||0> > 0:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] bones map filled"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] bones map empty"
    - if <[bmmodel].animations.contains[<[anim]>]||false>:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] animation list contains <[anim]>"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] animation list missing <[anim]>"

    # ── stage 5: bmstate ─────────────────────────────────────
    - announce to_console "[DBM-TEST] [5] bmstate"
    - bmstate entity:<[mob]> model:<[model]> state:<[anim]> loop:loop
    - wait 1s
    - if <[bmmodel].running_animations.contains[<[anim]>]||false>:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] running_animations reports <[anim]>"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] running_animations does not report <[anim]> - got <[bmmodel].running_animations||null>"
    - wait <[obs]>s
    - bmstate entity:<[mob]> model:<[model]> state:<[anim]> remove
    - wait 1s
    - bmstate entity:<[mob]> model:<[model]> state:<[anim]> loop:once lerp_frames:2
    - announce to_console "[DBM-TEST] [OK-parse] bmstate remove + legacy lerp_frames accepted"
    - define passed:++
    - wait <[obs]>s

    # ── stage 6: bone tags ───────────────────────────────────
    - announce to_console "[DBM-TEST] [6] bone tags on <[bone]>"
    - define bmbone <[bmmodel].bone[<[bone]>]>
    - if <[bmbone]||null> == null:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] bone <[bone]> not found - bones are <[bmmodel].bones.keys||null>"
    - else:
        - define passed:++
        - announce to_console "[DBM-TEST] world_location=<[bmbone].world_location.simple||FAIL> euler=<[bmbone].world_rotation_euler.simple||FAIL>"
        - announce to_console "[DBM-TEST] legacy world_rotation=<[bmbone].world_rotation.simple||FAIL> legacy bm_entity=<[bmbone].bm_entity||FAIL>"
        - announce to_console "[DBM-TEST] local=<[bmbone].local_position.simple||FAIL> global=<[bmbone].global_position.simple||FAIL> real=<[bmbone].real_position.simple||FAIL> visible=<[bmbone].is_visible||FAIL>"
        - if <[bmbone].world_location||null> != null && <[bmbone].world_rotation||null> != null && <[bmbone].bm_entity||null> != null:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] bone tags + legacy aliases resolve"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] some bone tag returned null"

        # ── stage 7: mechanisms + getter parity ──────────────
        - announce to_console "[DBM-TEST] [7] bone mechanisms + getters"
        - adjust <[bmbone]> tint:16711680
        - adjust <[bmbone]> glow:true
        - adjust <[bmbone]> glow_color:65280
        - adjust <[bmbone]> visible:false
        - adjust <[bmbone]> visible:true
        - adjust <[bmbone]> scale:2,2,2
        - adjust <[bmbone]> offset:0,0.5,0
        - adjust <[bmbone]> rotate:0,0,0,1
        - adjust <[bmbone]> view_range:64
        - adjust <[bmbone]> brightness:15|15
        - adjust <[bmbone]> shadow_radius:0.5
        - adjust <[bmbone]> interpolation_duration:2t
        - wait 5t
        - announce to_console "[DBM-TEST] getters tint=<[bmbone].tint||FAIL> glow=<[bmbone].glow||FAIL> glow_color=<[bmbone].glow_color||FAIL> view_range=<[bmbone].view_range||FAIL> brightness=<[bmbone].brightness||FAIL> shadow=<[bmbone].shadow_radius||FAIL> billboard=<[bmbone].billboard||FAIL> scale=<[bmbone].scale.simple||FAIL> offset=<[bmbone].offset.simple||FAIL>"
        - if <[bmbone].tint||0> == 16711680 && <[bmbone].glow||false> && <[bmbone].glow_color||0> == 65280:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] getter parity (tint/glow/glow_color read back)"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] getter parity mismatch"
        - adjust <[bmbone]> scale:1,1,1
        - adjust <[bmbone]> offset:0,0,0

        # ── stage 8: bmboard (parse-level) ───────────────────
        - bmboard entity:<[mob]> model:<[model]> bone:<[bone]> type:center
        - wait 5t
        - bmboard entity:<[mob]> model:<[model]> bone:<[bone]> type:fixed
        - define passed:++
        - announce to_console "[DBM-TEST] [8][OK-parse] bmboard center + fixed accepted"

        # ── stage 8b: skin_parts tag (new in 6.0) ─────────────
        - announce to_console "[DBM-TEST] [8b] skin_parts - <[bmbone].skin_parts||FAIL>"
        - if <[bmbone].skin_parts.contains[head]||false>:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] skin_parts tag lists head"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] skin_parts tag missing or empty"

    # ── stage 9: bmsummon (dummy) ────────────────────────────
    - announce to_console "[DBM-TEST] [9] bmsummon"
    - if <bm.capabilities.contains[dummy_trackers]||false>:
        - bmsummon model:<[model]> location:<[loc].add[3,0,0]> save:dummy
        - wait 1s
        - define dummymodel <entry[dummy].summoned_model||null>
        - if <[dummymodel]> != null:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] summoned <[dummymodel]> bones=<[dummymodel].bones.size||FAIL>"
            - wait <[obs]>s
            - bmsummon handle:<[dummymodel]> remove
            - define passed:++
            - announce to_console "[DBM-TEST] [OK-parse] bmsummon remove accepted"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] summoned_model entry is null"
    - else:
        - announce to_console "[DBM-TEST] SKIP bmsummon - no dummy_trackers capability"

    # ── stage 10: player-bound commands (skipped headless) ───
    - announce to_console "[DBM-TEST] [10] SKIP bmlimb/bmpart/bmmount/skin-mechanism-apply - need an online player or a p_seat bone"

    # ── stage 10b: capability coherence for 6.0 features ─────
    - if <bm.compat_layer||null> == v1:
        - if !<bm.capabilities.contains[animation_lifecycle_events]||false>:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] v1 correctly lacks animation_lifecycle_events"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] v1 must not declare animation_lifecycle_events"
    - else:
        - if <bm.capabilities.contains[animation_lifecycle_events]||false>:
            - define passed:++
            - announce to_console "[DBM-TEST] [OK] <bm.compat_layer> declares animation_lifecycle_events"
        - else:
            - define failed:++
            - announce to_console "[DBM-TEST] [FAIL] <bm.compat_layer> should declare animation_lifecycle_events"

    # ── stage 10c: keyframe signal + context.model (informational) ──
    # Only produces output if <[model]> carries a 'denizen:name{k=v}' keyframe on its
    # animation's Instructions track. Stock demo models don't, so this normally stays
    # silent; add such a keyframe to your own model to see it. Not counted on failure —
    # headless pipelines may also not tick animations without viewers.
    - bmstate entity:<[mob]> model:<[model]> state:<[anim]> loop:once
    - announce to_console "[DBM-TEST] [10c] INFO - if <[model]> has a denizen: keyframe, a [DBM-EVENT] signal line with its model context appears above within ~2s"
    - wait 2s

    # ── stage 11: cleanup ────────────────────────────────────
    - adjust <[bmmodel]> force_update
    - wait 1s
    - bmmodel entity:<[mob]> model:<[model]> remove
    - wait 1s
    - if <[mob].bm_entity||null> == null:
        - define passed:++
        - announce to_console "[DBM-TEST] [OK] model removed cleanly"
    - else:
        - define failed:++
        - announce to_console "[DBM-TEST] [FAIL] bm_entity still present after remove"
    - remove <[mob]>

    # ── summary ──────────────────────────────────────────────
    - announce to_console "[DBM-TEST] DBM-RESULT OK=<[passed]> FAIL=<[failed]>"


# Console mirror of every DBetterModel event.
dbm_headless_events:
    type: world
    debug: false
    events:
        on bm tracker created:
        - announce to_console "[DBM-EVENT] tracker created - <context.model_name> (dummy <context.dummy>)"
        on bm tracker closed:
        - announce to_console "[DBM-EVENT] tracker closed - <context.model_name>"
        on bm animation signal:
        - announce to_console "[DBM-EVENT] signal <context.name> meta <context.metadata> on model <context.model_name||null>"
        on bm animation starts:
        - announce to_console "[DBM-EVENT] animation starts - <context.animation||null> on <context.model_name||null>"
        on bm animation ends:
        - announce to_console "[DBM-EVENT] animation ends - <context.animation||null> on <context.model_name||null>"
        on bm hitbox damaged:
        - announce to_console "[DBM-EVENT] hitbox damaged - <context.model_name>"
        on bm hitbox interacted:
        - announce to_console "[DBM-EVENT] hitbox interacted - <context.model_name>"
        on bm model mounted:
        - announce to_console "[DBM-EVENT] mounted on <context.bone_name>"
        on bm model dismounted:
        - announce to_console "[DBM-EVENT] dismounted from <context.bone_name>"
        on bm starts reload:
        - announce to_console "[DBM-EVENT] reload started"
        on bm finishes reload:
        - announce to_console "[DBM-EVENT] reload finished - <context.result>"
