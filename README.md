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
    - blocks in blue
  - Example output:
    - `notifier: entity minecraft:horse at 120 66 -41 dist=7.2`
    - `notifier: block minecraft:diamond_ore at 126 10 -38`
- `/notifier detect <kind> <id> <enabled>`
  - Example entity: `/notifier detect entity minecraft:horse true`
  - Example block: `/notifier detect block minecraft:diamond_ore true`
- `/notifier detect radius <kind> <id> <value>`
  - Example: `/notifier detect radius entity minecraft:horse 24`
- `/notifier detect interval <kind> <id> <value>`
  - Example: `/notifier detect interval block minecraft:diamond_ore 40`
- `/notifier detect cooldown <kind> <id> <value>`
  - Example: `/notifier detect cooldown entity minecraft:horse 200`

`<kind>` must be `entity` or `block`.  
`<id>` must be a valid Minecraft identifier.

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
- Scan outlines from `/notifier detect scan` are temporary and fade automatically after roughly 6 seconds (`120` ticks).
- Block scans are heavier than entity scans, so block defaults use a slower interval and smaller radius.

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
