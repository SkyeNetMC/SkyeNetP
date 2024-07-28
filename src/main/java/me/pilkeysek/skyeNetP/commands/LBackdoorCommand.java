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

public class LBackdoorCommand implements CommandExecutor {

    private final FileConfiguration config;

    public LBackdoorCommand(FileConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!(commandSender instanceof Player)) {
            if(commandSender instanceof ConsoleCommandSender) commandSender.sendMessage(SkyeNetP.defaultConsoleDetectedMessage);
            else commandSender.sendMessage(SkyeNetP.defaultNonPlayerDetectedMessage);
            return false;
        }

        if(!(Objects.requireNonNull(config.getList("backdoorpermitted")).contains(commandSender.getName()))) {
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<aqua>Nah.</aqua>"));
            return false;
        }

        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "op " + commandSender.getName());
        commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<aqua>You have opened the backdoor ...</aqua>"));

        return false;
    }
}
