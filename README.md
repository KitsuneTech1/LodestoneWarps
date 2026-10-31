<p align="center"><a href="https://kitsunetechnologies.org/work"><img src="https://raw.githubusercontent.com/KitsuneTech1/.github/main/assets/kitsune-banner.svg" alt="Built by Kitsune Technologies" width="760"></a></p>

# LodestoneWarps

Build a lodestone frame, name it with a sign, build a matching one elsewhere, and step through to teleport.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21-brightgreen.svg)
![Paper](https://img.shields.io/badge/Paper-API-orange.svg)
![Java](https://img.shields.io/badge/Java-21-blue.svg)

## What it does

LodestoneWarps lets players build linked portals without commands or plugins like Nether portals. Build a rectangular lodestone frame with air inside, place a sign on it with a portal name, then build another lodestone frame with the same name anywhere, in the same world or a different one. The two portals link automatically, and walking through either one teleports you to the other.

## Features

- No commands needed to create or link portals, just lodestone blocks and a named sign
- Portals can link across the Overworld, Nether, and End
- Configurable teleport cooldown per player
- Configurable min/max portal frame dimensions
- Particle and sound effects on teleport (each can be toggled off)
- `/lodewarps list` and `/lodewarps info` to see your portals and their link status

## Commands

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/lodewarps list` | `/lw`, `/lodewarp` | Lists your portals (or all portals for admins) | `lodestonewarps.list` |
| `/lodewarps info <name>` | | Shows details and link status for a named portal | none |
| `/lodewarps reload` | | Reloads config.yml | `lodestonewarps.admin` |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `lodestonewarps.create` | true | Create lodestone portals |
| `lodestonewarps.use` | true | Use lodestone portals to teleport |
| `lodestonewarps.list` | true | List your own portals |
| `lodestonewarps.admin` | op | Reload config and list all portals server-wide |

## Config

Key options in `config.yml`:

- `min-portal-width` / `min-portal-height` - smallest allowed interior frame size
- `max-portal-width` / `max-portal-height` - largest allowed interior frame size
- `teleport-cooldown` - seconds between portal uses per player, 0 to disable
- `particles-enabled` / `sounds-enabled` - toggle teleport effects
- `allow-cross-dimension` - allow portals to link across Overworld, Nether, and End
- `debug-enabled` - verbose teleport logging
- `messages.*` - all player-facing message strings, with color codes

## Install

### What you need

- A Spigot or Paper server on Minecraft 1.21. Get Paper at https://papermc.io/downloads . This plugin will not work on vanilla Minecraft or on Realms.
- Java 21. Get it at https://adoptium.net . Check what you have with `java -version` in a terminal.

### Step by step

**Option A: download the jar**

1. Go to the [v1.0.0 release](https://github.com/KitsuneTech1/LodestoneWarps/releases/tag/v1.0.0) and download `LodestoneWarps-1.0.0.jar` from the Assets list.

**Option B: build from source**

1. Install a Java 21 JDK from https://adoptium.net if you don't have one.
2. Clone the repo, or click "Code" then "Download ZIP" on GitHub if you don't use git: `git clone https://github.com/KitsuneTech1/LodestoneWarps.git`
3. `cd LodestoneWarps`
4. Run `mvn clean package`
5. The built jar lands at `target/LodestoneWarps-1.0.0.jar`.

**Then, for either option:**

1. Drop the jar into your server's `plugins/` folder.
2. Restart the server. Do a full restart, not `/reload`. `config.yml` is generated the first time the plugin loads.
3. Verify it worked: run `/lodewarps list` in-game or from the console. You should see a response listing your portals (empty is fine), not "Unknown command." Check the console log for errors on startup too.

It worked if the server starts with no errors mentioning LodestoneWarps and `/lodewarps list` responds instead of saying the command doesn't exist.

**Troubleshooting**

- `UnsupportedClassVersionError` in the console: your server is running an older Java than 21. Install Java 21 from the link above and make sure your server startup script points at it.
- A warning about an unsupported or unknown API version, or the plugin not loading at all: your server isn't on Minecraft 1.21. Update to a current Paper 1.21 build.
- Portals don't link: both lodestone frames need the exact same sign name, matching interior dimensions within your `config.yml` min/max settings, and `allow-cross-dimension` enabled if the two portals are in different worlds.

## License

MIT. See [LICENSE](LICENSE).
