package me.pilkeysek.skyeNetP.commands;

import me.pilkeysek.skyeNetP.SkyeNetP;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SudoCommand implements CommandExecutor {
    private final FileConfiguration config;

    public SudoCommand(FileConfiguration config) {
        this.config = config;
    }
    @Override
    @SuppressWarnings("UnstableApiUsage") // Cuz I like having no warnings ._.
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!commandSender.hasPermission("skyenet.command.sudo")) {
            commandSender.sendMessage(SkyeNetP.defaultPermMessage);
            return false;
        }
        if(!Objects.requireNonNull(config.getList("enabledcommands")).contains("sudo")) {
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Command disabled in config</red>"));
            return false;
        }
        if(!(commandSender instanceof Player)) {
            if(commandSender instanceof ConsoleCommandSender) commandSender.sendMessage(SkyeNetP.defaultConsoleDetectedMessage);
            else commandSender.sendMessage(SkyeNetP.defaultNonPlayerDetectedMessage);
            return false;
        }

        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Sending command: </green><dark_aqua>" + String.join(" ", strings) + "</dark_aqua>"));
        Bukkit.getLogger().info(commandSender.getName() + " is now executing the following command as console: " + String.join(" ", strings));
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), String.join(" ", strings));
        return false;
    }
}
