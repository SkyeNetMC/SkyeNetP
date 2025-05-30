# SkyeNetP Plugin

**Version:** ### `/creative`
- Opens the gamemode menu for quick gamemode switching.
- Permission: `skyenetp.command.creativemenu`

### `/examplegui`
- Opens the example GUI with diamond and emerald buttons.
- Permission: `skyenetp.gui.example`

### `/stafftools`
- Opens the staff tools GUI with admin utilities.
- Permission: `skyenetp.gui.staff`

### `/gmmenu`
- Opens a gamemode selection GUI.
- Permission: `skyenetp.gui.gamemode`

### `/skyeguis`
- Main GUI management command.
- Permission: `skyenetp.gui.admin`
- Subcommands:
  - `list` - List all available GUIs
  - `open <gui>` - Open a specific GUI by name
  - `reload` - Reload all GUI configurations

### `/fly`
- Toggles flight mode for the player.
- Permission: `skyenetp.fly`

### `/datapacks`
- Lists all active datapacks on the server.
- Permission: `skyenetp.datapacks`

### `/datapacklist`
- Enables `/datapack list` but stopping /datapack enable/disable from being used. - used on our SMP server
- Permission: `skyenetp.datapacklist`

### `/skyenetp`
- Main plugin management command.
- Permission: `skyenetp.command.reload`
- Subcommands:
  - `reload [target]` - Reload configurations (all, config, guis, chatfilter)
  - `version` - Show plugin version informationmprehensive Minecraft Paper plugin for SkyeNetwork servers, providing advanced chat filtering, utility commands, and GUI management features.

## Features

- **Chat Filter System**: Advanced chat filtering with wordlist and regex pattern support
- **Flight Management**: Toggle flight mode with proper permissions
- **Datapack Management**: List and manage server datapacks
- **Creative Menu**: Quick gamemode switching interface
- **Dynamic Configuration**: Hot-reload configuration files without server restart
- **Permission-Based Access**: Granular permission system for all features

## Installation

1. Download the latest SkyeNetP-1.1.0.jar from the releases
2. Place the jar file in your server's `plugins/` directory
3. Restart your server
4. Configure the plugin using the generated configuration files in `plugins/SkyeNetP/`

## Requirements

- **Minecraft Version**: 1.21.4+
- **Server Software**: PaperMC
- **Java Version**: 21+
- **Dependencies**: CommandAPI (included)


## Commands

### `/fly`
- Toggles flight mode for the player.
- Permission: `skyenetp.fly`

### `/datapacks`
- Lists all active datapacks on the server.
- Permission: `skyenetp.datapacks`

### `/datapacklist`
- Enables `/datapack list` but stopping /datapack enable/disable from being used. - used on our SMP server
- Permission: `skyenetp.datapacklist`



### `/chatfilter reload`
- Reloads the chat filter configuration files (`wordlist.yml` and `regex.yml`).
- Permission: `skyenetp.chatfilter.reload`


<details>
<summary>### Chat Filter Info & Configuration</summary>

### Chat Filter Module
The Chat Filter Module provides advanced chat filtering capabilities for your Minecraft server. It includes the following features:

1. **Dynamic Wordlist Filtering**:
   - Filters chat messages based on a configurable list of blocked words.
   - The blocked words are dynamically loaded from `wordlist.yml`.
   - Debug logs are available to verify the `blockedWords` list and message content.

2. **Regex-Based Filtering**:
   - Filters chat messages using configurable regex patterns.
   - Regex patterns are dynamically loaded and compiled from `regex.yml`.
   - Debug logs are available to verify regex patterns and their matches.

3. **Bypass Permissions**:
   - Players can bypass wordlist and regex filtering if they have the appropriate permissions.
   - Bypass permissions are dynamically fetched from the configuration files (`wordlist.yml` and `regex.yml`).
   - Default permissions:
     - Wordlist Bypass: `skyenetp.wordlist.bypass`
     - Regex Bypass: `skyenetp.regex.bypass`

4. **Configuration Options**:
   - Enable or disable wordlist and regex filtering via the main configuration file.
   - Customize blocked words and regex patterns in their respective configuration files.

5. **Debugging and Logging**:
   - Extensive debug logs to assist in verifying the functionality of wordlist and regex filtering.

6. **Dynamic Reloading**:
   - Configuration files can be reloaded dynamically without restarting the server.

</details>

## GUIs Module

The GUIs Module provides a powerful and flexible GUI system similar to CommandPanels, allowing you to create custom inventory-based interfaces with configurable commands and permissions.

### Features

- **Custom GUI Creation**: Create unlimited custom GUIs with configurable titles, sizes, and items
- **Command Integration**: Execute commands when players click GUI items
- **Permission Control**: Set individual permissions for each GUI and command
- **Dynamic Loading**: Hot-reload GUI configurations without server restart
- **Item Customization**: Full control over item materials, names, lore, and enchantments
- **Player Placeholders**: Use `%player%` placeholder in commands
- **Console Commands**: Execute commands as console for admin functions

### Configuration

GUIs are configured in the `modules-guis.yml` file. Each GUI supports the following options:

```yaml
gui_name:
  title: "<gold>GUI Title"           # GUI window title (supports MiniMessage)
  size: 27                           # Inventory size (9, 18, 27, 36, 45, 54)
  command: "customcommand"           # Command to open this GUI
  permission: "skyenetp.gui.custom"  # Permission required to use the command
  items:
    slot_number:                     # Slot position (0-53)
      material: DIAMOND              # Item material
      name: "<aqua>Item Name"        # Item display name
      lore:                          # Item lore (list)
        - "<gray>Line 1"
        - "<yellow>Line 2"
      enchantments:                  # Optional enchantments
        sharpness: 5
        unbreaking: 3
      commands:                      # Commands to execute on click
        - "give %player% diamond 1"
        - "tell %player% You got a diamond!"
      console: false                 # Execute as console (default: false)
      close: true                    # Close GUI after click (default: false)
```

### Example GUIs

The plugin comes with several example GUIs:

1. **Example GUI** (`/examplegui`): Basic demonstration with diamond and emerald buttons
2. **Staff Tools** (`/stafftools`): Admin utilities including teleport and kit commands
3. **Gamemode Menu** (`/gmmenu`): Quick gamemode switching interface

### Management Commands

- `/skyeguis list` - List all available GUIs
- `/skyeguis open <gui>` - Open a specific GUI by name
- `/skyeguis reload` - Reload all GUI configurations

<details>
<summary>### Chat Filter Info & Configuration</summary>

## Configuration Files

### `wordlist.yml`
- Contains the list of blocked words.
- Example:
  ```yaml
  blocked-words:
    - badword1
    - badword2
  bypass-permission: skyenetp.wordlist.bypass
  ```

### `regex.yml`
- Contains the list of regex patterns for filtering chat messages.
- Example:
  ```yaml
  regex-patterns:
    - ".*badregex.*"
    - "^forbidden.*"
  bypass-permission: skyenetp.regex.bypass
  ```



</details>

## Permissions

### Chat Filter Permissions
- `skyenetp.wordlist.bypass`: Allows bypassing wordlist filtering.
- `skyenetp.regex.bypass`: Allows bypassing regex filtering.
- `skyenetp.chatfilter.reload`: Allows reloading the chat filter configuration.

### GUI Module Permissions
- `skyenetp.gui.admin`: Allows access to GUI management commands (`/skyeguis`)
- `skyenetp.gui.example`: Allows access to the example GUI (`/examplegui`)
- `skyenetp.gui.staff`: Allows access to staff tools GUI (`/stafftools`)
- `skyenetp.gui.gamemode`: Allows access to gamemode menu (`/gmmenu`)

### Command Permissions
- `skyenetp.command.creativemenu`: Allows access to the creative gamemode menu
- `skyenetp.command.reload`: Allows reloading the plugin configuration
- `skyenetp.fly`: Allows toggling flight mode
- `skyenetp.datapacks`: Allows listing datapacks
- `skyenetp.datapacklist`: Allows using the datapack list command

## Debugging

Enable debug mode in the main configuration file to view detailed logs for chat filtering operations.