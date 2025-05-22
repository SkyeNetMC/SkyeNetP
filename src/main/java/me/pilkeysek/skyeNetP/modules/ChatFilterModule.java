package me.pilkeysek.skyeNetP.modules;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
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
    private YamlConfiguration wordlistConfig;
    private final List<Pattern> spamPatterns = new ArrayList<>();
    private final List<Pattern> ipPatterns = new ArrayList<>();
    private final List<Pattern> capsPatterns = new ArrayList<>();
    private final List<Pattern> customPatterns = new ArrayList<>();
    private final List<String> blockedWords = new ArrayList<>();
    private long lastLoaded = 0;
    private final long reloadInterval = 60_000; // 1 minute
    private String bypassPermission;
    private final String configFolder = "chatfilter";
    private String prefix; // Centralized prefix storage

    public ChatFilterModule(JavaPlugin plugin) {
        this.plugin = plugin;
        createDefaultConfigs();
        loadConfigs();
    }

    private void createDefaultConfigs() {
        File moduleFolder = new File(plugin.getDataFolder().getParentFile(), "modules");
        File folder = new File(moduleFolder, configFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Create regex.yml if it doesn't exist
        File regexFile = new File(folder, "regex.yml");
        if (!regexFile.exists()) {
            try {
                plugin.saveResource("chatfilter/regex.yml", false);
                java.nio.file.Files.copy(
                    new File(plugin.getDataFolder(), "chatfilter/regex.yml").toPath(),
                    regexFile.toPath()
                );
                plugin.getLogger().info("Created default chat filter regex configuration.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create regex.yml: " + e.getMessage());
            }
        }

        // Create wordlist.yml if it doesn't exist
        File wordlistFile = new File(folder, "wordlist.yml");
        if (!wordlistFile.exists()) {
            try {
                plugin.saveResource("chatfilter/wordlist.yml", false);
                java.nio.file.Files.copy(
                    new File(plugin.getDataFolder(), "chatfilter/wordlist.yml").toPath(),
                    wordlistFile.toPath()
                );
                plugin.getLogger().info("Created default chat filter wordlist configuration.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create wordlist.yml: " + e.getMessage());
            }
        }
    }

    public void reloadConfig() {
        createDefaultConfigs();
        loadConfigs();
        plugin.getLogger().info("Chat filter configuration reloaded.");
    }

    private void loadConfigs() {
        // Load regex config
        File regexFile = new File(plugin.getDataFolder().getParentFile(), "modules/" + configFolder + "/regex.yml");
        regexConfig = YamlConfiguration.loadConfiguration(regexFile);
        
        // Load wordlist config
        File wordlistFile = new File(plugin.getDataFolder().getParentFile(), "modules/" + configFolder + "/wordlist.yml");
        wordlistConfig = YamlConfiguration.loadConfiguration(wordlistFile);
        
        // Load prefix from main config, fall back to regex.yml, then default
        prefix = plugin.getConfig().getString("modules.ChatFilter.prefix", 
               regexConfig.getString("prefix", "<dark_red>[ChatFilter]</dark_red> "));
        
        bypassPermission = regexConfig.getString("bypass-permission", "skyenetp.chatfilter.bypass");
        
        // Clear existing patterns and words
        spamPatterns.clear();
        ipPatterns.clear();
        capsPatterns.clear();
        customPatterns.clear();
        blockedWords.clear();

        // Load regex patterns if enabled
        if (regexConfig.getBoolean("Enable-regex", true)) {
            loadPatterns();
        }

        // Load wordlist if enabled
        if (wordlistConfig.getBoolean("enabled", true)) {
            blockedWords.addAll(wordlistConfig.getStringList("list"));
        }
    }

    private void loadPatterns() {
        // Load IP blocking patterns
        if (regexConfig.getBoolean("block-ips", true)) {
            try {
                String ipRegex = regexConfig.getString("block-ips.regex", "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
                ipPatterns.add(Pattern.compile(ipRegex));
                
                String advancedRegex = regexConfig.getString("block-ips.advanced-regex");
                if (advancedRegex != null && !advancedRegex.isEmpty()) {
                    ipPatterns.add(Pattern.compile(advancedRegex));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid IP blocking regex pattern: " + e.getMessage());
            }
        }

        // Load spam character patterns
        if (regexConfig.getBoolean("block-spam-chars", true)) {
            try {
                int threshold = regexConfig.getInt("block-spam-chars.threshold", 4);
                String spamRegex = regexConfig.getString("block-spam-chars.regex", "(.)\\1{" + (threshold - 1) + ",}");
                spamPatterns.add(Pattern.compile(spamRegex));
                
                List<String> whitelist = regexConfig.getStringList("block-spam-chars.whitelist");
                for (String allowed : whitelist) {
                    spamPatterns.add(Pattern.compile("(?!" + Pattern.quote(allowed) + ")"));
                }
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
                            plugin.getLogger().info("Loaded custom pattern: " + key);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid custom regex pattern '" + key + "': " + e.getMessage());
                    }
                }
            }
        }
    }

    private Component createFilteredComponent(String originalText, String filterType) {
        String baseText = regexConfig.getString("replacement-text", "<red>[Filtered]</red>");
        Component replacementComponent = miniMessage.deserialize(baseText)
            .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalText)
                .append(Component.newline())
                .append(Component.text("Filtered for: " + filterType))
            ));
        return replacementComponent;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        // Debug log the initial state
        plugin.getLogger().info("ChatFilter processing message from " + event.getPlayer().getName());

        // Check for bypass permission
        if (event.getPlayer().hasPermission(bypassPermission)) {
            plugin.getLogger().info("Player has bypass permission: " + bypassPermission);
            return;
        }

        // Reload configs periodically
        if (System.currentTimeMillis() - lastLoaded > reloadInterval) {
            loadConfigs();
            lastLoaded = System.currentTimeMillis();
        }

        String message = plainSerializer.serialize(event.message());
        boolean filtered = false;

        // Debug log the configs
        plugin.getLogger().info("Wordlist enabled: " + wordlistConfig.getBoolean("enabled", true));
        plugin.getLogger().info("Regex enabled: " + regexConfig.getBoolean("Enable-regex", true));
        plugin.getLogger().info("Number of blocked words: " + blockedWords.size());

        // Check wordlist first if not bypassed
        if (!event.getPlayer().hasPermission("skyenetp.wordlist.bypass") && wordlistConfig.getBoolean("enabled", true)) {
            String wordlistMsg = wordlistConfig.getString("blocked-message", "<prefix>Your message was filtered for containing a blocked word: <word>");
            for (String word : blockedWords) {
                if (message.toLowerCase().contains(word.toLowerCase())) {
                    plugin.getLogger().info("Found blocked word: " + word);
                    // Replace the word with filtered component that has hover text
                    Component filteredComponent = createFilteredComponent(word, "Blocked Word");
                    message = message.replaceAll("(?i)" + Pattern.quote(word), plainSerializer.serialize(filteredComponent));
                    filtered = true;
                    
                    // Send message to player
                    event.getPlayer().sendMessage(miniMessage.deserialize(
                        wordlistMsg.replace("<prefix>", prefix).replace("<word>", word)
                    ));
                }
            }
        }

        // Only check regex patterns if regex filtering is enabled and not bypassed
        if (!event.getPlayer().hasPermission("skyenetp.regex.bypass") && regexConfig.getBoolean("Enable-regex", true)) {
            String regexMsg = regexConfig.getString("blocked-message", "<prefix>Your message was filtered by pattern: <pattern>");
            
            // Check for IPs
            if (regexConfig.getBoolean("block-ips", true)) {
                for (Pattern pattern : ipPatterns) {
                    if (pattern.matcher(message).find()) {
                        plugin.getLogger().info("Found IP address pattern match");
                        String matchedText = message.substring(pattern.matcher(message).start(), pattern.matcher(message).end());
                        Component filteredComponent = createFilteredComponent(matchedText, "IP Address");
                        message = pattern.matcher(message).replaceAll(plainSerializer.serialize(filteredComponent));
                        filtered = true;
                        
                        // Send message to player
                        event.getPlayer().sendMessage(miniMessage.deserialize(
                            regexMsg.replace("<prefix>", prefix).replace("<pattern>", "IP Address")
                        ));
                    }
                }
            }

            // Check for spam characters
            if (regexConfig.getBoolean("block-spam-chars", true)) {
                for (Pattern pattern : spamPatterns) {
                    if (pattern.matcher(message).find()) {
                        plugin.getLogger().info("Found spam character pattern match");
                        String matchedText = message.substring(pattern.matcher(message).start(), pattern.matcher(message).end());
                        Component filteredComponent = createFilteredComponent(matchedText, "Character Spam");
                        message = pattern.matcher(message).replaceAll(plainSerializer.serialize(filteredComponent));
                        filtered = true;
                        
                        // Send message to player
                        event.getPlayer().sendMessage(miniMessage.deserialize(
                            regexMsg.replace("<prefix>", prefix).replace("<pattern>", "Character Spam")
                        ));
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
                    
                    for (char c : message.toCharArray()) {
                        if (Character.isLetter(c)) {
                            letterCount++;
                            if (Character.isUpperCase(c)) {
                                capsCount++;
                            }
                        }
                    }
                    
                    if (letterCount > 0) {
                        double capsPercentage = (capsCount * 100.0) / letterCount;
                        if (capsPercentage > threshold) {
                            plugin.getLogger().info("Found excessive caps: " + capsPercentage + "% caps");
                            message = plainSerializer.serialize(createFilteredComponent(message, "Excessive Caps"));
                            filtered = true;
                            
                            // Send message to player
                            event.getPlayer().sendMessage(miniMessage.deserialize(
                                regexMsg.replace("<prefix>", prefix)
                                       .replace("<pattern>", "Excessive Caps (" + Math.round(capsPercentage) + "%)")
                            ));
                        }
                    }
                }
            }

            // Check custom patterns
            if (regexConfig.getBoolean("Custom-Regex", false)) {
                for (Pattern pattern : customPatterns) {
                    if (pattern.matcher(message).find()) {
                        plugin.getLogger().info("Found custom pattern match");
                        String matchedText = message.substring(pattern.matcher(message).start(), pattern.matcher(message).end());
                        Component filteredComponent = createFilteredComponent(matchedText, "Custom Pattern");
                        message = pattern.matcher(message).replaceAll(plainSerializer.serialize(filteredComponent));
                        filtered = true;
                        
                        // Send message to player
                        event.getPlayer().sendMessage(miniMessage.deserialize(
                            regexMsg.replace("<prefix>", prefix).replace("<pattern>", "Custom Pattern")
                        ));
                    }
                }
            }
        }

        if (filtered) {
            plugin.getLogger().info("Message was filtered, setting new message");
            event.message(miniMessage.deserialize(message));
        } else {
            plugin.getLogger().info("Message passed all filters");
        }
    }
}
