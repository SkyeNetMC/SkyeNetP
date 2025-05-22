package me.pilkeysek.skyeNetP.commands;

import me.pilkeysek.skyeNetP.modules.ChatFilterModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatFilterCommand {
    private final JavaPlugin plugin;
    private final ChatFilterModule chatFilter;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ChatFilterCommand(JavaPlugin plugin, ChatFilterModule chatFilter) {
        this.plugin = plugin;
        this.chatFilter = chatFilter;
        registerCommand();
    }

    private void registerCommand() {
        new dev.jorel.commandapi.CommandAPICommand("chatfilter")
            .withPermission("skyenetp.chatfilter.admin")
            .withSubcommand(new dev.jorel.commandapi.CommandAPICommand("reload")
                .executes((sender, args) -> {
                    reloadConfig(sender);
                }))
            .register();
    }

    private void reloadConfig(CommandSender sender) {
        chatFilter.reloadConfig();
        sender.sendMessage(miniMessage.deserialize("<gold>[ChatFilter] <green>Configuration reloaded successfully."));
    }
}
