package me.pilkeysek.skyeNetP.modules;


import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatFilterModule implements Listener {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    private YamlConfiguration regexConfig;
    private final List<Pattern> spamPatterns = new ArrayList<>();
    private final List<Pattern> ipPatterns = new ArrayList<>();
    private final List<Pattern> capsPatterns = new ArrayList<>();
    private final List<Pattern> customPatterns = new ArrayList<>();
    private long lastLoaded = 0;
    private final long reloadInterval = 60_000; // 1 minute

    private String bypassPermission;

    public ChatFilterModule(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "chatfilter-regex/regex.yml");
        if (!configFile.exists()) {
            plugin.saveResource("chatfilter-regex/regex.yml", false);
        }
        
        regexConfig = YamlConfiguration.loadConfiguration(configFile);
        bypassPermission = regexConfig.getString("bypass-permission", "skyenetp.chatfilter.bypass");
        
        // Clear existing patterns
        spamPatterns.clear();
        ipPatterns.clear();
        capsPatterns.clear();
        customPatterns.clear();

        if (!regexConfig.getBoolean("Enable-regex", true)) {
            return;
        }

        // Load IP blocking patterns
        if (regexConfig.getBoolean("block-ips", true)) {
            try {
                String ipRegex = regexConfig.getString("block-ips.regex", "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
                ipPatterns.add(Pattern.compile(ipRegex));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid IP blocking regex pattern: " + e.getMessage());
            }
        }

        // Load spam character patterns
        if (regexConfig.getBoolean("block-spam-chars", true)) {
            try {
                String spamRegex = regexConfig.getString("block-spam-chars.regex", "(.)\\1{" + 
                    (regexConfig.getInt("block-spam-chars.threshold", 4) - 1) + ",}");
                spamPatterns.add(Pattern.compile(spamRegex));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid spam character regex pattern: " + e.getMessage());
            }
        }

        // Load caps patterns
        if (regexConfig.getBoolean("block-caps", true)) {
            try {
                int threshold = regexConfig.getInt("block-caps.threshold", 25);
                String capsRegex = regexConfig.getString("block-caps.regex", 
                    "(?=.{8,})(?=(?:.*[A-Z]){" + threshold + ",}).*");
                capsPatterns.add(Pattern.compile(capsRegex));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid caps regex pattern: " + e.getMessage());
            }
        }

        // Load custom patterns
        if (regexConfig.getBoolean("Custom-Regex", false)) {
            ConfigurationSection customSection = regexConfig.getConfigurationSection("custom-patterns");
            if (customSection != null) {
                for (String key : customSection.getKeys(false)) {
                    try {
                        String pattern = customSection.getString(key);
                        if (pattern != null && !pattern.isEmpty()) {
                            customPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid custom regex pattern '" + key + "': " + e.getMessage());
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!regexConfig.getBoolean("Enable-regex", true)) {
            return;
        }

        // Check for bypass permission
        if (event.getPlayer().hasPermission(bypassPermission)) {
            return;
        }

        // Reload config periodically
        if (System.currentTimeMillis() - lastLoaded > reloadInterval) {
            loadConfig();
            lastLoaded = System.currentTimeMillis();
        }

        String message = plainSerializer.serialize(event.message());
        boolean filtered = false;
        String replacement = regexConfig.getString("replacement-text", "<red>[Filtered]</red>");

        // Check for IPs
        if (regexConfig.getBoolean("block-ips", true)) {
            for (Pattern pattern : ipPatterns) {
                if (pattern.matcher(message).find()) {
                    message = pattern.matcher(message).replaceAll(replacement);
                    filtered = true;
                }
            }
        }

        // Check for spam characters
        if (regexConfig.getBoolean("block-spam-chars", true)) {
            for (Pattern pattern : spamPatterns) {
                if (pattern.matcher(message).find()) {
                    message = pattern.matcher(message).replaceAll("$1");
                    filtered = true;
                }
            }
        }

        // Check for excessive caps
        if (regexConfig.getBoolean("block-caps.enabled", true)) {
            int minLength = regexConfig.getInt("block-caps.min-length", 6);
            int threshold = regexConfig.getInt("block-caps.threshold", 60);
            
            if (message.length() >= minLength) {
                int capsCount = 0;
                int letterCount = 0;
                
                // Count uppercase letters and total letters
                for (char c : message.toCharArray()) {
                    if (Character.isLetter(c)) {
                        letterCount++;
                        if (Character.isUpperCase(c)) {
                            capsCount++;
                        }
                    }
                }
                
                // Calculate percentage if we have letters
                if (letterCount > 0) {
                    double capsPercentage = (capsCount * 100.0) / letterCount;
                    if (capsPercentage > threshold) {
                        message = message.toLowerCase();
                        filtered = true;
                    }
                }
            }
        }

        // Check custom patterns
        if (regexConfig.getBoolean("Custom-Regex", false)) {
            for (Pattern pattern : customPatterns) {
                if (pattern.matcher(message).find()) {
                    message = pattern.matcher(message).replaceAll(replacement);
                    filtered = true;
                }
            }
        }

        if (filtered) {
            event.message(miniMessage.deserialize(message));
        }
    }
}
