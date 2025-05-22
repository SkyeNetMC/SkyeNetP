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
        File modulesFolder = new File(plugin.getDataFolder().getParentFile(), "modules/guis");
        if (!modulesFolder.exists()) {
            modulesFolder.mkdirs();
        }

        File guisFile = new File(modulesFolder, "modules-guis.yml");
        if (!guisFile.exists()) {
            plugin.getLogger().warning("GUIs configuration file not found!");
            return;
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
    }

    public void registerGUICommands() {
        for (String guiName : loadedGUIs.keySet()) {
            ConfigurationSection gui = loadedGUIs.get(guiName);
            String command = gui.getString("command", guiName.toLowerCase());
            String permission = gui.getString("permission", "skyenetp.gui." + guiName.toLowerCase());
            
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
        }
    }

    public void openGUI(Player player, String guiName) {
        if (!loadedGUIs.containsKey(guiName)) {
            player.sendMessage(miniMessage.deserialize(guiPrefix + "<red>GUI not found: " + guiName));
            return;
        }

        ConfigurationSection gui = loadedGUIs.get(guiName);
        String title = gui.getString("title", "GUI");
        int size = gui.getInt("size", 27);

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

                        // Support for enchantments
                        if (itemSec.contains("enchantments")) {
                            ConfigurationSection enchants = itemSec.getConfigurationSection("enchantments");
                            if (enchants != null) {
                                for (String enchantName : enchants.getKeys(false)) {
                                    try {
                                        org.bukkit.enchantments.Enchantment enchant = null;
                                        
                                        // Try to match enchantment by name
                                        for (org.bukkit.enchantments.Enchantment e : org.bukkit.enchantments.Enchantment.values()) {
                                            if (e.getKey().getKey().equalsIgnoreCase(enchantName)) {
                                                enchant = e;
                                                break;
                                            }
                                        }
                                        
                                        if (enchant != null) {
                                            meta.addEnchant(enchant, enchants.getInt(enchantName), true);
                                        } else {
                                            plugin.getLogger().warning("Invalid enchantment " + enchantName + " in GUI " + guiName);
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().warning("Error adding enchantment " + enchantName + " in GUI " + guiName + ": " + e.getMessage());
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
