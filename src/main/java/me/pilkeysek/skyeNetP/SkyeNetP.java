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
import me.pilkeysek.skyeNetP.commands.ChatFilterCommand;
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
        msg = msg.replace("<version>", this.getName());
        return miniMessage.deserialize(msg);
    }

    public GUIModule getGUIModule() {
        return guiModule;
    }

    public ChatFilterModule getChatFilterModule() {
        return chatFilterModule;
    }

    @Override
    public void onLoad() {
        // Initialize CommandAPI
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        
        // Create config and data folders
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Save default config if it doesn't exist
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

        // Always register ChatFilter (it checks enabled state internally)
        getServer().getPluginManager().registerEvents(chatFilterModule, this);
        new ChatFilterCommand(this, chatFilterModule);

        // Register GUI listener if enabled
        if (config.getConfigurationSection("modules.GUIs") != null &&
            config.getBoolean("modules.GUIs.enabled", false)) {
            getServer().getPluginManager().registerEvents(guiModule, this);
            guiModule.registerGUICommands();
            guiModule.registerManagementCommand();
            getLogger().info("GUIModule enabled and registered with " + guiModule.getGUICount() + " GUIs");
        } else {
            getLogger().info("GUIModule is disabled in config");
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
                    String target = (String) args.getOrDefault("target", "all");
                    
                    switch (target.toLowerCase()) {
                        case "config":
                            this.reloadConfig();
                            config = this.getConfig();
                            loadMessages();
                            sender.sendMessage(miniMessage.deserialize(
                                config.getString("modules.GUIs.prefix", "<gold>[<aqua>SkyeGUIs<gold>] ") + 
                                "<green>Configuration reloaded!"));
                            break;
                        case "guis":
                            if (guiModule != null) {
                                guiModule.reloadGUIs();
                                sender.sendMessage(miniMessage.deserialize(
                                    config.getString("modules.GUIs.prefix", "<gold>[<aqua>SkyeGUIs<gold>] ") + 
                                    "<green>GUIs reloaded! (" + guiModule.getGUICount() + " GUIs loaded)"));
                            } else {
                                sender.sendMessage(miniMessage.deserialize(
                                    config.getString("modules.GUIs.prefix", "<gold>[<aqua>SkyeGUIs<gold>] ") + 
                                    "<red>GUI Module is not enabled!"));
                            }
                            break;
                        case "chatfilter":
                            if (chatFilterModule != null) {
                                chatFilterModule.reloadConfig();
                                sender.sendMessage(miniMessage.deserialize(
                                    config.getString("modules.ChatFilter.prefix", "<dark_red>[UwU-Watch]</dark_red> ") + 
                                    "<green>Chat filter reloaded!"));
                            } else {
                                sender.sendMessage(miniMessage.deserialize(
                                    config.getString("modules.ChatFilter.prefix", "<dark_red>[UwU-Watch]</dark_red> ") + 
                                    "<red>Chat Filter Module is not enabled!"));
                            }
                            break;
                        case "all":
                        default:
                            this.reloadConfig();
                            config = this.getConfig();
                            loadMessages();
                            if (guiModule != null) {
                                guiModule.reloadGUIs();
                            }
                            if (chatFilterModule != null) {
                                chatFilterModule.reloadConfig();
                            }
                            sender.sendMessage(getMessage("reloaded"));
                            break;
                    }
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
