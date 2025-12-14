package me.pilkeysek.skyeNetP.modules;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatModule implements Listener {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private LuckPerms luckPerms;
    private String chatFormat;

    public ChatModule(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfiguration();
        
        // Try to hook into LuckPerms
        RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            plugin.getLogger().info("ChatModule hooked into LuckPerms successfully");
        } else {
            plugin.getLogger().warning("LuckPerms not found! Chat formatting will work without prefixes/suffixes.");
        }
    }

    public void loadConfiguration() {
        chatFormat = plugin.getConfig().getString("modules.Chat.format", 
            "{prefix}<white>{player}</white>{suffix}<gray>:</gray> {message}");
        plugin.getLogger().info("ChatModule loaded with format: " + chatFormat);
    }

    public void reload() {
        loadConfiguration();
        plugin.getLogger().info("ChatModule reloaded");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component messageComponent = event.message();
        
        // Get the plain text message
        String message = PlainTextComponentSerializer.plainText().serialize(messageComponent);
        
        // Get prefix and suffix from LuckPerms
        String prefix = "";
        String suffix = "";
        
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                CachedMetaData metaData = user.getCachedData().getMetaData();
                prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
                suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
            }
        }
        
        // Build the formatted message
        String formattedMessage = chatFormat
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{player}", player.getName())
            .replace("{message}", message);
        
        // Parse MiniMessage and set as the new message
        Component finalComponent = miniMessage.deserialize(formattedMessage);
        
        // Cancel the default message and send our formatted one
        event.setCancelled(true);
        plugin.getServer().broadcast(finalComponent);
    }
}
