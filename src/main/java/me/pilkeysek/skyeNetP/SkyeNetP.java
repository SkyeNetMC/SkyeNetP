package me.pilkeysek.skyeNetP;

import me.pilkeysek.skyeNetP.commands.GamemodeMenuCommand;
import me.pilkeysek.skyeNetP.menu.GamemodeMenu;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyeNetP extends JavaPlugin {
    public static FileConfiguration config;

    @Override
    public void onEnable() {
        config = this.getConfig();
        saveDefaultConfig();
        
        // Register commands
        this.getCommand("gamemodemenu").setExecutor(new GamemodeMenuCommand());
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new GamemodeMenu(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
