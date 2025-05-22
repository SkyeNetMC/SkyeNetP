package me.pilkeysek.skyeNetP;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.pilkeysek.skyeNetP.commands.DatapacksCommand;
import me.pilkeysek.skyeNetP.commands.FlyCommand;
import me.pilkeysek.skyeNetP.commands.GamemodeMenuCommand;
import me.pilkeysek.skyeNetP.commands.SudoCommand;
import me.pilkeysek.skyeNetP.menu.CreativeMenu;
import me.pilkeysek.skyeNetP.modules.GUIModule;
import me.pilkeysek.skyeNetP.modules.ChatFilterModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@SuppressWarnings("UnstableApiUsage")
public final class SkyeNetP extends JavaPlugin {
    public static FileConfiguration config;
    private YamlConfiguration messagesConfig;
    private MiniMessage miniMessage = MiniMessage.miniMessage();
    private GUIModule guiModule;
    private ChatFilterModule chatFilterModule;

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getRawMessage(String key) {
        return messagesConfig.getString(key, "");
    }

    public Component getMessage(String key) {
        String msg = getRawMessage(key);
        String prefix = messagesConfig.getString("prefix", "");
        msg = msg.replace("<prefix>", prefix);
        msg = msg.replace("<version>", getDescription().getVersion());
        return miniMessage.deserialize(msg);
    }

    @Override
    public void onLoad() {
        // Initialize CommandAPI
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        // Ensure modules and subfolders exist
        File modulesFolder = new File(getDataFolder().getParentFile(), "modules");
        if (!modulesFolder.exists()) modulesFolder.mkdirs();
        File guisFolder = new File(modulesFolder, "guis");
        if (!guisFolder.exists()) guisFolder.mkdirs();
        File chatfilterFolder = new File(modulesFolder, "chatfilter");
        if (!chatfilterFolder.exists()) chatfilterFolder.mkdirs();

        File guisFile = new File(modulesFolder, "modules-guis.yml");
        if (!guisFile.exists()) {
            try (java.io.FileWriter writer = new java.io.FileWriter(guisFile)) {
                writer.write("# SkyeGUIs - Example GUI panel (CommandPanels style)\nexample:\n  title: \"<gold>Example GUI\"\n  size: 27\n  items:\n    11:\n      material: DIAMOND\n      name: \"<aqua>Diamond Button\"\n      lore:\n        - \"<gray>Click to get a diamond!\"\n      commands:\n        - \"give %player% diamond 1\"\n      close: true\n    15:\n      material: EMERALD\n      name: \"<green>Emerald Button\"\n      lore:\n        - \"<gray>Click to get an emerald!\"\n      commands:\n        - \"give %player% emerald 1\"\n      close: true\n");
            } catch (Exception ignored) {}
        }
        File regexFile = new File(modulesFolder, "regex.txt");
        if (!regexFile.exists()) {
            try (java.io.FileWriter writer = new java.io.FileWriter(regexFile)) {
                writer.write("# Each line is a regex pattern to block in chat\n# Example: block IP addresses\n\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b\n# Example: block repeated characters\n(.)\\1{4,}\n");
            } catch (Exception ignored) {}
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = this.getConfig();
        loadMessages();

        // Initialize modules
        guiModule = new GUIModule(this);
        chatFilterModule = new ChatFilterModule(this);

        // Register creative menu command and listener
        this.getCommand("creative").setExecutor(new GamemodeMenuCommand());
        getServer().getPluginManager().registerEvents(new CreativeMenu(), this);

        // Register ChatFilter if enabled
        if (config.getConfigurationSection("modules.ChatFilter") != null &&
            config.getBoolean("modules.ChatFilter.enabled", false)) {
            getServer().getPluginManager().registerEvents(chatFilterModule, this);
        }

        // Register GUI listener if enabled
        if (config.getConfigurationSection("modules.GUIs") != null &&
            config.getBoolean("modules.GUIs.enabled", false)) {
            getServer().getPluginManager().registerEvents(guiModule, this);
            guiModule.registerGUICommands();
        }

        // Register Brigadier commands
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            SudoCommand.register(commands);
            FlyCommand.register(commands);
            DatapacksCommand.register(commands);
        });

        // Register CommandAPI /skyenetp reload command
        new CommandAPICommand("skyenetp")
            .withSubcommand(new CommandAPICommand("reload")
                .withOptionalArguments(new StringArgument("target"))
                .executes((sender, args) -> {
                    File configFileReload = new File(getDataFolder(), "config.yml");
                    if (!configFileReload.exists()) {
                        saveDefaultConfig();
                    }
                    this.reloadConfig();
                    config = this.getConfig();
                    loadMessages();
                    sender.sendMessage(getMessage("reloaded"));
                })
            )
            .withSubcommand(new CommandAPICommand("version")
            .executes((sender, args) -> {
                sender.sendMessage(getMessage("version"));
            })
            )
            .register();
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        // Plugin shutdown logic
    }
}
