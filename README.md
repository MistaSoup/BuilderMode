# BuilderMode Plugin

A Minecraft plugin for 1.21.x that temporarily increases player render distance with movement restrictions. Fully compatible with both Paper and Folia servers.

## 🎯 Features

- **Temporary Render Distance Increase**: Players can temporarily boost their render distance for a configurable duration
- **Per-Dimension Configuration**: Different settings for Overworld, Nether, and The End
- **Movement Restrictions**: Prevents riding entities (except minecarts) and using elytras while active
- **Automatic Elytra Removal**: Equipped elytras are automatically moved to inventory or dropped
- **Elytra Warning System**: Double-confirmation required when wearing an elytra
- **Cooldown System**: Prevents spam usage with configurable cooldowns per dimension
- **Safety Check System**: Automatically resets render distance at configurable intervals to prevent bypasses
- **Folia Compatible**: Works seamlessly on both Paper and Folia servers
- **Tab Completion**: Easy-to-use tab completion for all commands
- **Fully Customizable**: All messages and timings configurable in config.yml
- **Enable/Disable Toggle**: Turn the entire plugin on or off via config

## 📝 Commands

### Main Commands
- `/buildermode` or `/bm` or `/builder` - Shows available subcommands
- `/buildermode on` - Activate BuilderMode
- `/buildermode off` - Manually deactivate BuilderMode
- `/buildermode info` - Check time remaining (active or cooldown status)

### Admin Commands
- `/bmr` - Reload the configuration without restarting (requires `buildermode.reload` permission)

### Tab Completion
Type `/buildermode` and press TAB to see available options: `on`, `off`, `info`

## 🔑 Permissions

- `buildermode.reload` - Allows reloading the plugin configuration (default: op)
- `buildermode.*` - Grants all BuilderMode permissions (default: op)

**Note**: No permission is required to use BuilderMode by default - all players can use it!

## ⚙️ Configuration

The `config.yml` file allows you to customize all aspects of the plugin:

```yaml
# Enable or disable the entire plugin
enabled: true

# Default render distance to restore after BuilderMode expires
default-render-distance: 10

# How often (in seconds) to check and reset render distance for players not using BuilderMode
# This is a safety feature to prevent render distance bypasses or glitches
safety-check-interval: 300

# Dimension-specific settings
dimensions:
  overworld:
    render-distance: 32    # Chunks
    duration: 60           # Seconds
    cooldown: 300          # Seconds
  
  nether:
    render-distance: 24
    duration: 60
    cooldown: 300
  
  the_end:
    render-distance: 32
    duration: 60
    cooldown: 300

# Customizable Messages
messages:
  no-permission: "&cYou don't have permission to use this command!"
  players-only: "&cOnly players can use this command!"
  config-reloaded: "&aBuildderMode configuration reloaded!"
  already-active: "&cBuilderMode is already active!"
  on-cooldown: "&cYou must wait {time} seconds before using BuilderMode again!"
  activated: "&aBuilderMode activated! Render distance set to {distance} chunks for {duration} seconds."
  not-active: "&cBuilderMode is not currently active!"
  disabled-manually: "&eBuilderMode has been disabled manually."
  expired: "&eBuilderMode has expired. Render distance restored."
  info-active: "&aBuilderMode is active! Time remaining: &e{time} seconds"
  info-cooldown: "&eBuilderMode is on cooldown. Time remaining: &c{time} seconds"
  info-ready: "&aBuilderMode is ready to use!"
  invalid-usage: "&cUsage: /buildermode [on|off|info]"
  elytra-warning: "&e&lWARNING: &eYou are wearing an elytra! It will be removed if you activate BuilderMode. Use &6/buildermode on &eagain to confirm."
  elytra-removed-inventory: "&eYour elytra has been moved to your inventory."
  elytra-removed-ground: "&eYour elytra has been dropped on the ground (inventory full)."
  cannot-ride-entity: "&cYou cannot ride entities while BuilderMode is active (except minecarts)!"
  cannot-use-elytra: "&cYou cannot use elytra while BuilderMode is active!"
  cannot-equip-elytra: "&cYou cannot equip elytra while BuilderMode is active!"
  plugin-disabled: "&cBuilderMode is currently disabled!"
```

### Message Placeholders
- `{time}` - Time in seconds
- `{distance}` - Render distance in chunks
- `{duration}` - Duration in seconds

### Color Codes
Use `&` for Minecraft color codes (e.g., `&a` = green, `&c` = red, `&e` = yellow, `&6` = gold)

## 🚀 Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart your server or use a plugin manager
4. Configure settings in `plugins/BuilderMode/config.yml`
5. Use `/bmr` to reload changes without restarting

## 🔨 Building from Source

### Prerequisites
- Java 21 or higher
- Maven

### Build Steps

1. Clone the repository
2. Navigate to the project directory
3. Run `mvn clean package`
4. The compiled JAR will be in the `target` folder

## 📁 Project Structure

```
src/main/java/com/yourname/buildermode/
├── BuilderMode.java                    # Main plugin class
├── ConfigManager.java                  # Configuration handler
├── RenderDistanceManager.java          # Render distance logic
└── MovementRestrictionManager.java     # Movement and elytra restrictions

src/main/resources/
├── plugin.yml                          # Plugin metadata
└── config.yml                          # Default configuration
```

## 🎮 How It Works

1. Player executes `/buildermode on`
2. **Elytra Check**: If wearing elytra, shows warning and requires second confirmation
3. **Permission Check**: Verifies plugin is enabled in config
4. **Cooldown Check**: Ensures cooldown has expired
5. Current render distance is saved
6. Render distance is increased to configured value
7. Movement restrictions are applied:
   - Cannot ride entities (except all minecart types)
   - Cannot use or equip elytra
8. After configured duration, render distance is automatically restored
9. Cooldown timer starts

### Safety Features

- **Automatic Render Distance Reset**: Every X seconds (configurable), the plugin checks all players not using BuilderMode and resets their render distance to default
- **No Bypasses**: All attempts to extend render distance are blocked
- **Folia Thread Safety**: Uses proper region scheduling on Folia servers

## 🚫 Restrictions While Active

### Entity Riding
Players cannot ride any entities **except**:
- Minecart
- Chest Minecart
- Furnace Minecart
- Hopper Minecart
- TNT Minecart
- Command Block Minecart
- Spawner Minecart

### Elytra
- Cannot equip elytra (all methods blocked)
- Cannot activate elytra flight
- Any equipped elytra is automatically removed and moved to inventory
- If inventory is full, elytra is dropped on the ground
- Double-confirmation required when activating BuilderMode while wearing elytra

## 💡 Usage Examples

### Basic Usage
```
/buildermode on          # Activate BuilderMode
/buildermode info        # Check time remaining
/buildermode off         # Manually deactivate
```

### Admin Usage
```
/bmr                     # Reload configuration
```

### Checking Status
The `/buildermode info` command shows different messages based on state:
- **Active**: "BuilderMode is active! Time remaining: 45 seconds"
- **On Cooldown**: "BuilderMode is on cooldown. Time remaining: 120 seconds"
- **Ready**: "BuilderMode is ready to use!"

### Disabling the Plugin
To temporarily disable BuilderMode:
1. Edit `config.yml` and set `enabled: false`
2. Run `/bmr` to reload
3. All players will see "BuilderMode is currently disabled!" when trying to use it

## 🔧 Compatibility

- **Minecraft Version**: 1.21.x
- **Server Software**: Paper, Folia
- **Java Version**: 21+
- **Dependencies**: None (standalone plugin)

## 🛡️ Technical Details

- Uses Paper's native `setViewDistance()` API
- Folia-aware scheduling for multi-threaded regions
- Automatic detection of server type (Paper vs Folia)
- Thread-safe for Folia's regionized threading
- No external dependencies required
- Efficient event handling and task scheduling

## 📋 Support

For issues, suggestions, or contributions, please contact the plugin developer.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/MistaSoup/BuilderMode/blob/main/LICENSE.txt) file for details.

---

**Made for Minecraft 1.21.x | Paper & Folia Compatible**
