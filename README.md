![MCClock](https://files.catbox.moe/dwkqew.png)

---
# MCClock

A Minecraft plugin that renders an analog clock onto a map item, showing the real-world time in a configurable timezone.

## Requirements

- Paper/Spigot 1.13+
- Java 17+

## Installation

1. Drop the `.jar` into your `plugins/` folder.
2. Restart the server.
3. Edit `plugins/MCClock/config.yml` as needed.
4. Run `/mcclock get` to receive the clock map item.

## Configuration

`plugins/MCClock/config.yml`:

```yaml
# https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
timezone: "Asia/Ho_Chi_Minh"

# Clock face image file (placed in plugins/MCClock/ folder)
clock-face: "clock_face.png"

item:
  name: "&bClock"
  lore:
    - "&7An analog clock."

hands:
  # Thickness of the clock hands
  # Recommended values: 1-2
  thickness: 1

  # smooth: second hand moves continuously between ticks
  # tick: second hand jumps once per second
  mode: smooth

  # Compensation in milliseconds for network latency (try 500-1000 if clock appears behind)
  # Example: 1000 = 1 second
  latency-compensation-ms: 0

# DON'T CHANGE THIS
map-id: -1
```

You can replace `plugins/MCClock/clock_face.png` with a custom image to change the clock face. The plugin will use it on the next reload.

## Commands

| Command | Description |
|---|---|
| `/mcclock get [amount]` | Give yourself 1–64 clock map items |
| `/mcclock reload` | Reload config and re-attach the renderer |

Alias: `/mcc`

## Permissions

| Permission | Description | Default |
|---|---|---|
| `mcclock.admin` | Access to all `/mcclock` commands | OP |
