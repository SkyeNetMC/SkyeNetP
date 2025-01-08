package me.pilkeysek.skyeNetP.commands;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static me.pilkeysek.skyeNetP.SkyeNetP.config;

@SuppressWarnings("UnstableApiUsage")
public class SudoCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("sudo")
                        .requires(commandSourceStack -> config.getList("enabledcommands").contains("sudo"))
                        .requires(commandSourceStack -> commandSourceStack.getSender() instanceof Player)
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("skyenetp.sudo"))
                        .then(
                                Commands.argument("command", greedyString())
                                        .executes(context -> {
                                            String command = context.getArgument("command", String.class);
                                            Bukkit.getLogger().info(context.getSource().getSender().getName() + " is now executing the following command as console: " + command);
                                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                                            return Command.SINGLE_SUCCESS;
                                        })
                        )
                        .build(),
                "Executes a command as console"
        );
    }
}
