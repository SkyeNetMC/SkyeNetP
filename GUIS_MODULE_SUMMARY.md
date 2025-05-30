# GUIs Module Implementation Summary

## What We've Done

### ✅ Fixed Module Loading
- Fixed the file path issue where GUIModule was looking in the wrong directory
- Now properly loads `modules-guis.yml` from the plugin data folder
- Automatic creation of default configuration if it doesn't exist

### ✅ Enhanced Configuration
- Added command and permission settings per GUI
- Expanded example configuration with 3 complete GUIs:
  - **Example GUI** (`/examplegui`) - Basic demonstration
  - **Staff Tools** (`/stafftools`) - Admin utilities  
  - **Gamemode Menu** (`/gmmenu`) - Quick gamemode switching

### ✅ Dynamic Command Registration
- Automatic command registration based on GUI configuration
- Each GUI can have its own custom command and permission
- Error handling for command registration conflicts

### ✅ Management Commands
- Added `/skyeguis` management command with subcommands:
  - `list` - Shows all available GUIs
  - `open <gui>` - Opens a specific GUI by name
  - `reload` - Reloads all GUI configurations

### ✅ Integration with Main Plugin
- Proper module initialization in main plugin class
- Integration with `/skyenetp reload guis` command
- Module enable/disable through main config

### ✅ Inventory Click Handling
- Complete click event handling with command execution
- Support for player placeholders (`%player%`)
- Console command execution option
- GUI close functionality

### ✅ Item Customization
- Material, name, and lore support
- MiniMessage formatting for colors and styling
- Enchantment framework (placeholder for future enhancement)

## How It Works

1. **Configuration Loading**: On startup, the module reads `modules-guis.yml`
2. **Command Registration**: For each GUI, registers the specified command
3. **GUI Opening**: When a command is executed, opens the corresponding inventory
4. **Click Handling**: Processes clicks and executes configured commands
5. **Dynamic Reloading**: Can reload configurations without server restart

## Example Usage

```yaml
# modules-guis.yml
my_custom_gui:
  title: "<gold>My Custom GUI"
  size: 27
  command: "mycustomgui"
  permission: "server.gui.custom"
  items:
    13:
      material: DIAMOND
      name: "<blue>Special Item"
      lore:
        - "<gray>Click for rewards!"
      commands:
        - "give %player% diamond 5"
        - "tell %player% You received diamonds!"
      close: true
```

## Commands Added

- `/examplegui` - Opens example GUI
- `/stafftools` - Opens staff tools GUI (requires staff permission)
- `/gmmenu` - Opens gamemode menu
- `/skyeguis list` - Lists all GUIs
- `/skyeguis open <gui>` - Opens specific GUI
- `/skyeguis reload` - Reloads GUI configs

## Module Status: ✅ FULLY FUNCTIONAL

The GUIs module is now complete and working! Users can:
- Create custom GUIs through configuration
- Use dynamic commands and permissions
- Execute commands on item clicks
- Manage GUIs through admin commands
- Hot-reload configurations without server restart

The module provides a powerful alternative to CommandPanels with full integration into the SkyeNetP plugin ecosystem.
