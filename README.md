# Notifier

Notifier is a Fabric client-side mod for Minecraft `1.21.x` that sends proximity alerts when configured entities or blocks are nearby.

## What It Does

- Scans around the local player for configured detection targets.
- Supports both entity targets (for example `minecraft:horse`) and block targets (for example `minecraft:diamond_ore`).
- Triggers alerts on edge transitions (when a target enters range), then applies a per-target cooldown.
- Supports on-demand scan reports in persistent local chat so results stay visible for review.
- Stores client preferences in `config/notifier-client.json`.

## Commands

All commands are client-side and only affect your local client.

- `/notifier detect list`
  - Lists configured targets and whether each one is enabled.
- `/notifier detect scan`
  - Runs an immediate scan for all enabled targets and writes results to your local chat history.
  - Also draws temporary world-space outlines for scan matches:
    - entities in red
  - blocks in ore-specific colors when the block is a known ore; otherwise default blue
  - Example output:
    - `notifier: entity minecraft:horse at 120 66 -41 dist=7.2`
    - `notifier: block minecraft:diamond_ore at 126 10 -38`
- Convenience presets:
  - `/notifier detect precious_ores [<true|false>]`
    - Enables block detection for `minecraft:diamond_ore` and `minecraft:ancient_debris`.
    - Use `false` to disable the same group (example: `/notifier detect precious_ores false`).
  - `/notifier detect essential_ores [<true|false>]`
    - Enables `precious_ores` plus `minecraft:iron_ore`, `minecraft:gold_ore`, `minecraft:redstone_ore`, and `minecraft:lapis_ore`.
    - Use `false` to disable the same group.
  - `/notifier detect all_ores [<true|false>]`
    - Enables all Minecraft ore-like block IDs (`*_ore` plus `ancient_debris`).
    - Use `false` to disable the same group.
- `/notifier detect highlightOnMatch <true|false>`
  - When enabled (default `true`), the detector draws capped world-space outlines immediately when a passive detection match triggers (for the triggering target only).
- `/notifier detect entity <id> <enabled>`
  - Example: `/notifier detect entity minecraft:horse true`
- `/notifier detect block <id> <enabled>`
  - Example: `/notifier detect block minecraft:diamond_ore true`
- `/notifier detect radius entity <id> <value>`
  - Example: `/notifier detect radius entity minecraft:horse 24`
- `/notifier detect radius block <id> <value>`
  - Example: `/notifier detect radius block minecraft:diamond_ore 24`
- `/notifier detect interval entity <id> <value>`
  - Example: `/notifier detect interval entity minecraft:horse 40`
- `/notifier detect interval block <id> <value>`
  - Example: `/notifier detect interval block minecraft:diamond_ore 40`
- `/notifier detect cooldown entity <id> <value>`
  - Example: `/notifier detect cooldown entity minecraft:horse 200`
- `/notifier detect cooldown block <id> <value>`
  - Example: `/notifier detect cooldown block minecraft:diamond_ore 200`

`entity` and `block` are explicit subcommands and tab-complete under `/notifier detect`.  
`<id>` must be a valid Minecraft identifier and is tab-completed by kind (`entity` IDs for entity commands, block IDs for block commands).

## How Detection Works

- Each target has:
  - `radius`
  - `checkIntervalTicks`
  - `cooldownTicks`
  - `messageTemplate`
- The detector keeps runtime state per target:
  - `wasNearby` for edge-trigger behavior
  - cooldown counter
  - interval counter
- Passive detections continue to use transient notifications; `/notifier detect scan` emits a persistent chat report.
- Scan outlines from `/notifier detect scan` are temporary and fade automatically after roughly 60 seconds (`1200` ticks).
- When `highlightOnMatch` is enabled, the detector also updates outlines on the passive edge-transition that triggers a notification (outlines are capped like the scan command and apply to the triggering target only).
- Block scans are heavier than entity scans, so block defaults use a slower interval and smaller radius.

### Highlight Color Overview

Current highlight color behavior:

- Entity highlights: fixed red.
  - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#ff2626;"></span> `entity (default)`
- Block highlights:
  - Default fallback blue: <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#3373ff;"></span> `block (default fallback)`
  - Ores use mapped colors:
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#40e6f2;"></span> `diamond_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#33f259;"></span> `emerald_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#ffd633;"></span> `gold_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#d69e73;"></span> `iron_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#e0804d;"></span> `copper_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#595959;"></span> `coal_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#f23333;"></span> `redstone_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#3373f2;"></span> `lapis_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#ebebeb;"></span> `nether_quartz_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#ffbf26;"></span> `nether_gold_ore`
    - <span style="display:inline-block;width:12px;height:12px;border:1px solid #888;border-radius:2px;background:#995940;"></span> `ancient_debris`
  - `deepslate_*` ores reuse the matching base ore color (for example `deepslate_redstone_ore` -> `redstone_ore` color).
  - Non-ore blocks fall back to default block blue.

## Logging and Troubleshooting

- Startup, command changes, and delivered notifications are logged at `info`.
- Scan completion and highlight totals are logged at `info`.
- Invalid command input and malformed config entries are logged at `warn`.
- Config write failures are logged at `error`.
- Verbose scan logs are controlled by `verboseLogging` in `config/notifier-client.json`.

If notifications do not appear:

1. Run `/notifier detect list` to verify your target is enabled.
2. Check the target identifier spelling (namespace + path).
3. Confirm you are within the configured radius.
4. Check logs for `warn`/`error` entries.

## Development

Requirements:

- Java `21`
- Minecraft target `1.21.x`
- Fabric Loader + Fabric API

Useful commands:

- `./gradlew test`
- `./gradlew build`

## License

This project is available under the CC0 license.
