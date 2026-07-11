# Experimental features

These features ship in 6.0 as experiments: they work in our testing, but their
shape may change based on feedback, and rough edges are possible. Nothing here
affects the stable surface — every command, tag, mechanism and event documented
elsewhere behaves the same whether these are on or off.

## Keyframe script bridge

**Config**: `experimental.keyframe-script-bridge` (default: `true`)

The signal system already lets a Blockbench keyframe fire the `bm animation signal`
event. The bridge goes one step further: a keyframe can start a Denizen task script
directly — logic authored right on the animation timeline, no world event needed.

Put an instruction keyframe on the model's animation:

```
denizen:run{script=knight_strike;damage=8;element=fire}
```

The reserved signal name `run` starts the task script named by the `script=` key.
Every other metadata entry becomes a definition, plus:

| Definition | Value |
|---|---|
| `<[model]>` | the BMModelTag whose animation fired the keyframe |
| `<[model_name]>` | its model name |
| `<[player]>` | the linked player, for per-player signals |
| `<[damage]>`, `<[element]>`, ... | your own metadata keys |

```yaml
knight_strike:
    type: task
    script:
    - hurt <[damage]> <[model].bm_entity.base_entity.location.find_entities.within[3].exclude[<[model].bm_entity.base_entity>]||<list>>
```

Notes:
- The `bm animation signal` event still fires for `run` signals — the bridge runs alongside, not instead.
- Works on every supported BetterModel line (the signal path exists everywhere).
- The player is linked on the queue for per-player signals, so `<player>` works inside the task.
- Known BetterModel quirk applies here too: multiple Instructions-track keys with an
  identical prefix may drop all but the last keyframe — use the Particle track's
  Script field for reliable delivery.

## Folia readiness

**Config**: none (always on) · **plugin.yml**: `folia-supported: true`

All DBetterModel internals are region-safe: cross-thread work goes through the
global region scheduler, entity-bound work (skin application) through the entity's
own scheduler, background work (update checker) through the async scheduler. On
plain Paper these delegate to the main thread — behavior is identical to 5.x.

The honest caveat: **Denizen itself does not support Folia yet.** Vanilla Folia
will not load Denizen, and DBetterModel depends on it — so today this matters on
Folia forks that force-load unsupported plugins (at your own risk), and it means
DBetterModel is ready on day one when Denizen adds official Folia support.
