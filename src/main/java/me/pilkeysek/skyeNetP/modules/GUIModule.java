package me.pilkeysek.skyeNetP.modules;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class GUIModule implements Listener {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, ConfigurationSection> loadedGUIs = new HashMap<>();
    private final Map<Player, String> openGUIs = new HashMap<>();
    private String guiPrefix = "<gold>[<aqua>SkyeGUIs<gold>] ";

    public GUIModule(JavaPlugin plugin) {
        this.plugin = plugin;
        loadGUIs();
    }

    public void loadGUIs() {
        loadedGUIs.clear();
        
        // Create modules-guis.yml in plugin data folder if it doesn't exist
        File guisFile = new File(plugin.getDataFolder(), "modules-guis.yml");
        if (!guisFile.exists()) {
            plugin.saveResource("modules-guis.yml", false);
            plugin.getLogger().info("Created default GUIs configuration file: modules-guis.yml");
        }

        YamlConfiguration guisConfig = YamlConfiguration.loadConfiguration(guisFile);
        for (String key : guisConfig.getKeys(false)) {
            ConfigurationSection section = guisConfig.getConfigurationSection(key);
            if (section != null) {
                loadedGUIs.put(key, section);
                plugin.getLogger().info("Loaded GUI: " + key);
            }
        }

        // Load prefix from config
        if (plugin.getConfig().getConfigurationSection("modules.GUIs") != null) {
            guiPrefix = plugin.getConfig().getString("modules.GUIs.prefix", guiPrefix);
        }
        
        plugin.getLogger().info("GUIModule loaded with " + loadedGUIs.size() + " GUIs");
        
        // Log available GUIs for debugging
        if (!loadedGUIs.isEmpty()) {
            plugin.getLogger().info("Available GUIs:");
            for (String guiName : loadedGUIs.keySet()) {
                ConfigurationSection gui = loadedGUIs.get(guiName);
                String command = gui.getString("command", guiName.toLowerCase());
                plugin.getLogger().info("  - " + guiName + " (/" + command + ")");
            }
        }
    }

    public void reloadGUIs() {
        loadGUIs();
        registerGUICommands();
        plugin.getLogger().info("GUIModule reloaded with " + loadedGUIs.size() + " GUIs");
    }

    public int getGUICount() {
        return loadedGUIs.size();
    }

    public void registerManagementCommand() {
        new dev.jorel.commandapi.CommandAPICommand("skyeguis")
            .withPermission("skyenetp.gui.admin")
            .withSubcommand(new dev.jorel.commandapi.CommandAPICommand("list")
                .executes((sender, args) -> {
                    sender.sendMessage(miniMessage.deserialize(guiPrefix + "<yellow>Available GUIs:"));
                    for (String guiName : loadedGUIs.keySet()) {
                        ConfigurationSection gui = loadedGUIs.get(guiName);
                        String command = gui.getString("command", guiName.toLowerCase());
                        sender.sendMessage(miniMessage.deserialize(
                            "<gray>- <aqua>" + guiName + "</aqua> (Command: <white>/" + command + "</white>)"
                        ));
                    }
                })
            )
            .withSubcommand(new dev.jorel.commandapi.CommandAPICommand("open")
                .withArguments(new dev.jorel.commandapi.arguments.StringArgument("gui"))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        String guiName = (String) args.get("gui");
                        openGUI(player, guiName);
                    } else {
                        sender.sendMessage(miniMessage.deserialize(guiPrefix + "<red>This command can only be used by players!"));
                    }
                })
            )
            .withSubcommand(new dev.jorel.commandapi.CommandAPICommand("reload")
                .executes((sender, args) -> {
                    reloadGUIs();
                    sender.sendMessage(miniMessage.deserialize(guiPrefix + "<green>GUIs reloaded! (" + getGUICount() + " GUIs loaded)"));
                })
            )
            .register();
    }

    public void registerGUICommands() {
        for (String guiName : loadedGUIs.keySet()) {
            ConfigurationSection gui = loadedGUIs.get(guiName);
            String command = gui.getString("command", guiName.toLowerCase());
            String permission = gui.getString("permission", "skyenetp.gui." + guiName.toLowerCase());
            
            try {
                new dev.jorel.commandapi.CommandAPICommand(command)
                    .withPermission(permission)
                    .executes((sender, args) -> {
                        if (sender instanceof Player player) {
                            openGUI(player, guiName);
                        } else {
                            sender.sendMessage(miniMessage.deserialize(guiPrefix + "<red>This command can only be used by players!"));
                        }
                    })
                    .register();
                
                plugin.getLogger().info("Registered GUI command: /" + command + " for GUI: " + guiName);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to register command /" + command + " for GUI " + guiName + ": " + e.getMessage());
            }
        }
    }

    public void openGUI(Player player, String guiName) {
        if (!loadedGUIs.containsKey(guiName)) {
            player.sendMessage(miniMessage.deserialize(guiPrefix + "<red>GUI not found: " + guiName));
            plugin.getLogger().warning("Attempted to open non-existent GUI: " + guiName);
            return;
        }

        ConfigurationSection gui = loadedGUIs.get(guiName);
        String title = gui.getString("title", "GUI");
        int size = gui.getInt("size", 27);

        plugin.getLogger().info("Opening GUI '" + guiName + "' for player " + player.getName());

        try {
            Inventory inv = Bukkit.createInventory(null, size, miniMessage.deserialize(title));
            ConfigurationSection items = gui.getConfigurationSection("items");
            
            if (items != null) {
                for (String slotStr : items.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        if (slot >= size) {
                            plugin.getLogger().warning("Invalid slot " + slot + " in GUI " + guiName + " (size: " + size + ")");
                            continue;
                        }

                        ConfigurationSection itemSec = items.getConfigurationSection(slotStr);
                        if (itemSec == null) continue;

                        Material mat = Material.matchMaterial(itemSec.getString("material", "STONE"));
                        if (mat == null) {
                            plugin.getLogger().warning("Invalid material in GUI " + guiName + " slot " + slot);
                            mat = Material.STONE;
                        }

                        ItemStack item = new ItemStack(mat);
                        ItemMeta meta = item.getItemMeta();

                        if (itemSec.contains("name")) {
                            meta.displayName(miniMessage.deserialize(itemSec.getString("name")));
                        }

                        if (itemSec.contains("lore")) {
                            List<String> lore = itemSec.getStringList("lore");
                            List<Component> loreComp = new ArrayList<>();
                            for (String l : lore) {
                                loreComp.add(miniMessage.deserialize(l));
                            }
                            meta.lore(loreComp);
                        }

                        // Support for enchantments (simplified to avoid deprecation warnings)
                        if (itemSec.contains("enchantments")) {
                            ConfigurationSection enchants = itemSec.getConfigurationSection("enchantments");
                            if (enchants != null) {
                                for (String enchantName : enchants.getKeys(false)) {
                                    try {
                                        // Use reflection or just skip enchantments for now to avoid deprecation warnings
                                        plugin.getLogger().info("Enchantment " + enchantName + " configured but skipped due to API changes");
                                    } catch (Exception e) {
                                        plugin.getLogger().warning("Error with enchantment " + enchantName + " in GUI " + guiName + ": " + e.getMessage());
                                    }
                                }
                            }
                        }

                        item.setItemMeta(meta);
                        inv.setItem(slot, item);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid slot number in GUI " + guiName + ": " + slotStr);
                    }
                }
            }

            player.openInventory(inv);
            openGUIs.put(player, guiName);
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating GUI " + guiName + ": " + e.getMessage());
            player.sendMessage(miniMessage.deserialize(guiPrefix + "<red>Error creating GUI!"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openGUIs.containsKey(player)) return;

        String guiName = openGUIs.get(player);
        ConfigurationSection gui = loadedGUIs.get(guiName);
        if (gui == null) return;

        event.setCancelled(true);
        
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

        ConfigurationSection items = gui.getConfigurationSection("items");
        if (items == null) return;

        String slotStr = String.valueOf(event.getSlot());
        ConfigurationSection itemSec = items.getConfigurationSection(slotStr);
        if (itemSec == null) return;

        // Execute commands
        if (itemSec.contains("commands")) {
            for (String cmd : itemSec.getStringList("commands")) {
                String finalCmd = cmd.replace("%player%", player.getName());
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(
                    itemSec.getBoolean("console", false) ? Bukkit.getConsoleSender() : player,
                    finalCmd
                ));
            }
        }

        // Close inventory if specified
        if (itemSec.getBoolean("close", false)) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openGUIs.remove(player);
        }
    }
}
