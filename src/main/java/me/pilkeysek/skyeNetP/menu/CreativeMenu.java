package me.pilkeysek.skyeNetP.menu;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CreativeMenu implements Listener {
    private static final String MENU_TITLE = "Gamemode Menu";
    private static final int WOOL_SLOT = 4; // Center slot of the first row

    public static void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, MENU_TITLE); // 1 row, still the correct method
        updateWoolState(inventory, player);
        player.openInventory(inventory);
    }

    private static void updateWoolState(Inventory inventory, Player player) {
        ItemStack wool = new ItemStack(player.getGameMode() == GameMode.CREATIVE ? Material.RED_WOOL : Material.LIME_WOOL);
        ItemMeta meta = wool.getItemMeta();
        // setDisplayName is still present, but may be marked deprecated in some IDEs; it is not removed. Use as is.
        meta.setDisplayName(player.getGameMode() == GameMode.CREATIVE ? 
            "§cClick to set Adventure Mode" : 
            "§aClick to set Creative Mode");
        wool.setItemMeta(meta);
        inventory.setItem(WOOL_SLOT, wool);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // getTitle is still present, but may be marked deprecated in some IDEs; it is not removed. Use as is.
        if (!event.getView().getTitle().equals(MENU_TITLE)) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getSlot() != WOOL_SLOT) return;

        if (player.getGameMode() == GameMode.CREATIVE) {
            player.setGameMode(GameMode.ADVENTURE);
        } else {
            player.setGameMode(GameMode.CREATIVE);
        }
        
        updateWoolState(event.getInventory(), player);
    }
}