package me.pilkeysek.skyeNetP.commands;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class DatapacksCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("datapacks")
                        .executes(context -> {
                            DatapackManager datapackManager = Bukkit.getDatapackManager();
                            List<Datapack> enabledPacks = new ArrayList<>(datapackManager.getEnabledPacks());
                            Component message = Component.text("There are " + enabledPacks.size() + " data pack(s) enabled: ");
                            for (int i = 0; i < enabledPacks.size(); i++) {
                                Datapack pack = enabledPacks.get(i);
                                Component packText = Component.text('[', NamedTextColor.GREEN)
                                        .append(Component.text(pack.getName(), NamedTextColor.GRAY))
                                        .append(Component.text(']', NamedTextColor.GREEN))
                                        .hoverEvent(HoverEvent.showText(Component.text(pack.getName())));
                                
                                message = message.append(packText);
                                if (i < enabledPacks.size() - 1) {
                                    message = message.append(Component.text(", ", NamedTextColor.GRAY));
                                }
                            }
                            message = message.append(Component.text("\nThere are no more data packs available"));
                            context.getSource().getSender().sendMessage(message);
                            return Command.SINGLE_SUCCESS;
                        }).build(),
                "List enabled datapacks"
        );
    }
}
