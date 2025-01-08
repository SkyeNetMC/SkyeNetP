package me.pilkeysek.skyeNetP.commands;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class LBackdoorCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("lbackdoor")
                        .requires(commandSourceStack -> commandSourceStack.getSender() instanceof Player)
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("skyenetp.backdoor"))
                        .executes(context -> {
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "op " + context.getSource().getSender().getName());
                            context.getSource().getSender().sendMessage(Component.text("You have opened the backdoor...", NamedTextColor.AQUA));
                            return Command.SINGLE_SUCCESS;
                        }).build(),
                "Whaaat? A backdoor??"
        );
    }
}
