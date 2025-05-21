package me.pilkeysek.skyeNetP.commands;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class FlyCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("fly")
                        .requires(commandSourceStack -> commandSourceStack.getSender() instanceof Player)
                        .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("skyenetp.fly"))
                        .executes(context -> {
                            Player player = (Player) context.getSource().getSender();
                            if (player.getAllowFlight()) {
                                player.setAllowFlight(false);
                                player.sendMessage(Component.text("You can no longer fly", NamedTextColor.RED));
                            } else {
                                player.setAllowFlight(true);
                                player.sendMessage(Component.text("You can now fly", NamedTextColor.GREEN));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Toggle your ability to fly"
        );
    }
}
