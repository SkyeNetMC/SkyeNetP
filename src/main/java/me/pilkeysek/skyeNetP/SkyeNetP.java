package me.pilkeysek.skyeNetP;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.pilkeysek.skyeNetP.commands.DatapacksCommand;
import me.pilkeysek.skyeNetP.commands.FlyCommand;
import me.pilkeysek.skyeNetP.commands.GamemodeMenuCommand;
import me.pilkeysek.skyeNetP.commands.LBackdoorCommand;
import me.pilkeysek.skyeNetP.commands.SudoCommand;
import me.pilkeysek.skyeNetP.menu.GamemodeMenu;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public final class SkyeNetP extends JavaPlugin {
    public static FileConfiguration config;

    @Override
    public void onEnable() {
        config = this.getConfig();
        saveDefaultConfig();

        // Register gamemode menu command and listener
        this.getCommand("gamemodemenu").setExecutor(new GamemodeMenuCommand());
        getServer().getPluginManager().registerEvents(new GamemodeMenu(), this);
        
        // Register Brigadier commands
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            LBackdoorCommand.register(commands);
            SudoCommand.register(commands);
            FlyCommand.register(commands);
            DatapacksCommand.register(commands);
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
