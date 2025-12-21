package me.pilkeysek.skyeNetP.modules;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
        // Refresh display names for online players so command feedback etc updates immediately
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            applyLuckPermsDisplayName(online);
        }
        plugin.getLogger().info("ChatModule reloaded");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // Ensure display name is formatted for join/quit/command feedback contexts
        applyLuckPermsDisplayName(event.getPlayer());
    }

    private void applyLuckPermsDisplayName(Player player) {
        if (luckPerms == null) {
            return;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return;
        }

        CachedMetaData metaData = user.getCachedData().getMetaData();
        String prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
        String suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";

        // Parse in one MiniMessage pass so LuckPerms tags work, but keep the name readable
        Component display = miniMessage.deserialize(prefix + "<white>" + player.getName() + "</white>" + suffix);

        player.displayName(display);
        // Also update tab list name (best-effort; Paper provides this)
        player.playerListName(display);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Convert {placeholders} to MiniMessage <placeholders> so we can safely inject Components
        final String mmFormat = chatFormat
            .replace("{prefix}", "<prefix>")
            .replace("{suffix}", "<suffix>")
            .replace("{player}", "<player>")
            .replace("{message}", "<message>");
        
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

        final String finalPrefix = prefix;
        final String finalSuffix = suffix;

        // Use Paper's renderer (1.21.10) so chat isn't double-sent and stays in the normal pipeline
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            TagResolver resolver = TagResolver.builder()
                // Parse LuckPerms meta inline so its tags (e.g. <gradient>) behave as expected
                .resolver(Placeholder.parsed("prefix", finalPrefix == null ? "" : finalPrefix))
                .resolver(Placeholder.parsed("suffix", finalSuffix == null ? "" : finalSuffix))
                // Insert the name as plain text so surrounding MiniMessage tags can affect it
                .resolver(Placeholder.unparsed("player", source.getName()))
                .resolver(Placeholder.component("message", message))
                .build();

            return miniMessage.deserialize(mmFormat, resolver);
        });
    }
}
