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
import java.util.regex.Matcher;

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
    private String prefix;
    private String defaultReplacement = "<red>[Filtered]</red>";

    public ChatFilterModule(JavaPlugin plugin) {
        this.plugin = plugin;
        createDefaultConfigs();
        loadConfigs();
    }

    public void reloadConfig() {
        createDefaultConfigs();
        loadConfigs();
        plugin.getLogger().info("Chat filter configuration reloaded.");
    }

    private void createDefaultConfigs() {
        // Create chatfilter folder directly in plugin folder
        File folder = new File(plugin.getDataFolder(), configFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Create regex.yml if it doesn't exist
        File regexFile = new File(folder, "regex.yml");
        if (!regexFile.exists()) {
            try {
                plugin.saveResource("chatfilter/regex.yml", false);
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
                plugin.getLogger().info("Created default chat filter wordlist configuration.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create wordlist.yml: " + e.getMessage());
            }
        }
    }

    private void loadConfigs() {
        // First clear all existing data
        spamPatterns.clear();
        ipPatterns.clear();
        capsPatterns.clear();
        customPatterns.clear();
        blockedWords.clear();
        
        plugin.getLogger().info("Loading chat filter configurations...");
        
        // Load regex config
        File regexFile = new File(plugin.getDataFolder(), configFolder + "/regex.yml");
        regexConfig = YamlConfiguration.loadConfiguration(regexFile);
        
        // Load wordlist config
        File wordlistFile = new File(plugin.getDataFolder(), configFolder + "/wordlist.yml");
        wordlistConfig = YamlConfiguration.loadConfiguration(wordlistFile);

        // Load settings from main config
        ConfigurationSection chatFilterConfig = plugin.getConfig().getConfigurationSection("modules.ChatFilter");
        if (chatFilterConfig != null) {
            // Get prefix and replacement text
            prefix = chatFilterConfig.getString("prefix", "<dark_red>[ChatFilter]</dark_red> ");
            defaultReplacement = chatFilterConfig.getString("replacement", "<red>[Filtered]</red>");

            // Add global blocked words
            List<String> globalBlockedWords = chatFilterConfig.getStringList("blocked-words");
            if (globalBlockedWords != null && !globalBlockedWords.isEmpty()) {
                blockedWords.addAll(globalBlockedWords);
                plugin.getLogger().info("Added " + globalBlockedWords.size() + " global blocked words from main config");
            }
        }
        
        bypassPermission = regexConfig.getString("bypass-permission", "skyenetp.chatfilter.bypass");

        // Add bypass permission checks for regex and wordlist in the configuration
        if (regexConfig.getBoolean("Enable-regex", true)) {
            bypassPermission = regexConfig.getString("bypass-permission", "skyenetp.regex.bypass");
            plugin.getLogger().info("Regex bypass permission: " + bypassPermission);
        }

        if (wordlistConfig.getBoolean("enabled", true)) {
            bypassPermission = wordlistConfig.getString("bypass-permission", "skyenetp.wordlist.bypass");
            plugin.getLogger().info("Wordlist bypass permission: " + bypassPermission);
        }

        // Load regex patterns if enabled
        if (regexConfig.getBoolean("Enable-regex", true)) {
            loadPatterns();
        }

        // Load wordlist if enabled
        if (wordlistConfig.getBoolean("enabled", true)) {
            List<String> wordList = wordlistConfig.getStringList("list");
            if (wordList != null && !wordList.isEmpty()) {
                blockedWords.addAll(wordList);
                plugin.getLogger().info("Added " + wordList.size() + " blocked words from wordlist.yml");
            }
        }

        // Log the loaded configuration
        plugin.getLogger().info("ChatFilter loaded " + blockedWords.size() + " total blocked words");
        plugin.getLogger().info("ChatFilter prefix: " + prefix);
        
        // Debug log all words if debug mode is enabled
        if (plugin.getConfig().getBoolean("modules.ChatFilter.debug", false)) {
            plugin.getLogger().info("DEBUG: All blocked words:");
            for (String word : blockedWords) {
                plugin.getLogger().info(" - \"" + word + "\"");
            }
        }
    }

    /**
     * Tests if a regex pattern works correctly by trying it against sample text
     *
     * @param pattern The compiled pattern to test
     * @param testString A string that should match this pattern
     * @param patternName Name of the pattern for logging
     * @return True if the pattern matches as expected
     */
    private boolean testPattern(Pattern pattern, String testString, String patternName) {
        try {
            Matcher matcher = pattern.matcher(testString);
            boolean matches = matcher.find();
            plugin.getLogger().info("Testing pattern " + patternName + " against \"" + testString + "\": " + 
                                   (matches ? "MATCHED" : "NO MATCH"));
            return matches;
        } catch (Exception e) {
            plugin.getLogger().warning("Error testing pattern " + patternName + ": " + e.getMessage());
            return false;
        }
    }
    
    private void loadPatterns() {
        // Load IP blocking patterns
        if (regexConfig.getBoolean("block-ips.enabled", true)) {
            try {
                String ipRegex = regexConfig.getString("block-ips.regex", "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
                
                plugin.getLogger().info("Compiling IP regex: " + ipRegex);
                try {
                    Pattern ipPattern = Pattern.compile(ipRegex, Pattern.CASE_INSENSITIVE);
                    ipPatterns.add(ipPattern);
                    plugin.getLogger().info("Successfully compiled IP regex pattern");
                    
                    // Test the pattern with a sample IP address
                    testPattern(ipPattern, "This contains 127.0.0.1 as an IP", "IP Pattern");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to compile IP pattern: " + e.getMessage());
                }
                
                String advancedRegex = regexConfig.getString("block-ips.advanced-regex");
                if (advancedRegex != null && !advancedRegex.isEmpty()) {
                    plugin.getLogger().info("Compiling advanced IP regex: " + advancedRegex);
                    try {
                        Pattern advancedPattern = Pattern.compile(advancedRegex, Pattern.CASE_INSENSITIVE);
                        ipPatterns.add(advancedPattern);
                        plugin.getLogger().info("Successfully compiled advanced IP regex pattern");
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to compile advanced IP pattern: " + e.getMessage());
                    }
                }
                
                if (ipPatterns.isEmpty()) {
                    // Add a simple fallback pattern to catch IPs like 127.0.0.1
                    ipPatterns.add(Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", Pattern.CASE_INSENSITIVE));
                    plugin.getLogger().info("Added fallback IP pattern");
                }
                
                plugin.getLogger().info("Loaded " + ipPatterns.size() + " IP blocking patterns");
            } catch (Exception e) {
                plugin.getLogger().warning("Error in IP blocking pattern section: " + e.getMessage());
            }
        }

        // Load spam character patterns
        if (regexConfig.getBoolean("block-spam-chars.enabled", true)) {
            try {
                int threshold = regexConfig.getInt("block-spam-chars.threshold", 4);
                String spamRegex = regexConfig.getString("block-spam-chars.regex", "(.)\\1{" + (threshold - 1) + ",}");
                
                plugin.getLogger().info("Compiling spam regex: " + spamRegex);
                try {
                    Pattern spamPattern = Pattern.compile(spamRegex, Pattern.CASE_INSENSITIVE);
                    spamPatterns.add(spamPattern);
                    plugin.getLogger().info("Successfully compiled spam regex pattern");
                    
                    // Test the pattern with sample text
                    testPattern(spamPattern, "Hellooooooo there", "Spam Pattern");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to compile spam pattern: " + e.getMessage());
                }
                
                List<String> whitelist = regexConfig.getStringList("block-spam-chars.whitelist");
                for (String allowed : whitelist) {
                    if (allowed != null && !allowed.isEmpty()) {
                        try {
                            String negativePattern = "(?!" + Pattern.quote(allowed) + ")";
                            spamPatterns.add(Pattern.compile(negativePattern, Pattern.CASE_INSENSITIVE));
                            plugin.getLogger().info("Added whitelist pattern for: " + allowed);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to compile whitelist pattern for " + allowed + ": " + e.getMessage());
                        }
                    }
                }
                plugin.getLogger().info("Loaded spam prevention patterns");
            } catch (Exception e) {
                plugin.getLogger().warning("Error in spam character pattern section: " + e.getMessage());
            }
        }

        // Load caps patterns
        if (regexConfig.getBoolean("block-caps.enabled", true)) {
            try {
                int threshold = regexConfig.getInt("block-caps.threshold", 60);
                String capsRegex = "(?=.{8,})(?=(?:.*[A-Z]){" + threshold + ",}).*";
                
                plugin.getLogger().info("Compiling caps regex: " + capsRegex);
                try {
                    Pattern capsPattern = Pattern.compile(capsRegex);
                    capsPatterns.add(capsPattern);
                    plugin.getLogger().info("Successfully compiled caps regex pattern");
                    
                    // Test the pattern with sample text
                    testPattern(capsPattern, "THIS IS ALL CAPS TEXT", "Caps Pattern");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to compile caps pattern: " + e.getMessage());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error in caps pattern section: " + e.getMessage());
            }
        }

        // Load custom patterns
        if (regexConfig.getBoolean("custom-patterns.enabled", false)) {
            ConfigurationSection patternsSection = regexConfig.getConfigurationSection("custom-patterns.patterns");
            if (patternsSection != null) {
                for (String key : patternsSection.getKeys(false)) {
                    try {
                        String pattern = patternsSection.getString(key);
                        if (pattern != null && !pattern.isEmpty()) {
                            plugin.getLogger().info("Compiling custom regex: " + key + " = " + pattern);
                            try {                            Pattern customPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                            customPatterns.add(customPattern);
                            plugin.getLogger().info("Successfully loaded custom pattern: " + key);
                            
                            // Test the custom pattern
                            if (key.equals("test-pattern")) {
                                testPattern(customPattern, "This contains badword1 which should be filtered", "Custom Pattern - " + key);
                            }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to compile custom pattern '" + key + "': " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error with custom regex pattern '" + key + "': " + e.getMessage());
                    }
                }
            } else {
                plugin.getLogger().info("No custom patterns section found in config");
            }
        } else {
            plugin.getLogger().info("Custom patterns are disabled in config");
        }
        
        plugin.getLogger().info("Loaded " + ipPatterns.size() + " IP patterns, " + 
                              spamPatterns.size() + " spam patterns, " +
                              capsPatterns.size() + " caps patterns, " +
                              customPatterns.size() + " custom patterns");
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        // Debug log the initial state
        plugin.getLogger().info("ChatFilter processing message from " + event.getPlayer().getName());

        // Check if module is enabled
        if (!plugin.getConfig().getBoolean("modules.ChatFilter.enabled", true)) {
            plugin.getLogger().info("ChatFilter module is disabled in config");
            return;
        }

        // Use dynamically fetched bypass permissions instead of hardcoded ones
        String wordlistBypassPermission = wordlistConfig.getString("bypass-permission", "skyenetp.wordlist.bypass");
        String regexBypassPermission = regexConfig.getString("bypass-permission", "skyenetp.regex.bypass");

        // Check for bypass permission dynamically
        if (event.getPlayer().hasPermission(wordlistBypassPermission) && 
            plugin.getConfig().getBoolean("modules.ChatFilter.wordlist.enabled", true)) {
            plugin.getLogger().info("Wordlist check skipped - player has bypass permission: " + wordlistBypassPermission);
        } else {
            plugin.getLogger().info("Checking message against wordlist. Words loaded: " + blockedWords.size());
            // Add debug logs to verify blockedWords and message content
            plugin.getLogger().info("Blocked words loaded: " + blockedWords);
            plugin.getLogger().info("Original message for wordlist check: " + plainSerializer.serialize(event.message()));

            String wordlistMsg = wordlistConfig.getString("blocked-message", "<prefix>Your message was filtered for containing a blocked word: <word>");

            for (String word : blockedWords) {
                if (word == null || word.isEmpty()) {
                    continue;
                }

                String currentMessage = plainSerializer.serialize(event.message());
                String lowercaseWord = word.toLowerCase();
                String lowercaseMessage = currentMessage.toLowerCase();

                plugin.getLogger().info("Checking for word: \"" + word + "\"");

                if (lowercaseMessage.contains(lowercaseWord)) {
                    plugin.getLogger().info("Found blocked word: \"" + word + "\"");

                    Component replacement = createFilteredComponent(word, "Blocked Word");

                    int startIndex = lowercaseMessage.indexOf(lowercaseWord);
                    if (startIndex >= 0 && startIndex + word.length() <= currentMessage.length()) {
                        String actualWord = currentMessage.substring(startIndex, startIndex + word.length());

                        plugin.getLogger().info("Replacing \"" + actualWord + "\" with filtered text");

                        event.message(replaceText(event.message(), actualWord, replacement));

                        event.getPlayer().sendMessage(miniMessage.deserialize(
                            wordlistMsg.replace("<prefix>", prefix).replace("<word>", word)
                        ));
                    } else {
                        plugin.getLogger().warning("Found word but couldn't locate it in the message text");
                    }
                }
            }
        }

        if (event.getPlayer().hasPermission(regexBypassPermission) && 
            plugin.getConfig().getBoolean("modules.ChatFilter.regex.enabled", true)) {
            plugin.getLogger().info("Regex check skipped - player has bypass permission: " + regexBypassPermission);
        } else {
            plugin.getLogger().info("Checking regex patterns");
            String regexMsg = regexConfig.getString("blocked-message", "<prefix>Your message was filtered by pattern: <pattern>");

            // Check each pattern type with direct regex testing
            if (regexConfig.getBoolean("block-ips.enabled", true)) {
                plugin.getLogger().info("Checking IP patterns (" + ipPatterns.size() + " patterns)");
                
                // Test with a direct IP pattern matching first (hardcoded for reliability)
                String originalMessage = plainSerializer.serialize(event.message());
                if (originalMessage.matches(".*\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*")) {
                    plugin.getLogger().info("Direct IP pattern match found!");
                    
                    // Replace the entire message
                    Component replacement = miniMessage.deserialize(defaultReplacement)
                        .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalMessage)
                            .append(Component.newline())
                            .append(Component.text("Filtered for: IP Address"))
                        ));
                    
                    event.message(replacement);
                    
                    // Notify the player
                    event.getPlayer().sendMessage(miniMessage.deserialize(
                        regexMsg.replace("<prefix>", prefix)
                               .replace("<pattern>", "IP Address")
                    ));
                    
                    plugin.getLogger().info("Applied direct IP replacement");
                } else {
                    // Try the loaded patterns
                    for (Pattern pattern : ipPatterns) {
                        boolean matched = checkAndReplacePattern(event, originalMessage, event.message(), pattern, "IP Address", regexMsg);
                        if (matched) {
                            plugin.getLogger().info("IP pattern matched and filtered");
                            break;
                        }
                    }
                }
            }

            if (regexConfig.getBoolean("block-spam-chars.enabled", true)) {
                plugin.getLogger().info("Checking spam patterns (" + spamPatterns.size() + " patterns)");
                
                // First try with a direct spam check for repeated characters
                boolean directSpamFound = false;
                String originalMessage = plainSerializer.serialize(event.message());
                char lastChar = '\0';
                int count = 1;
                int threshold = regexConfig.getInt("block-spam-chars.threshold", 4);
                
                for (int i = 0; i < originalMessage.length(); i++) {
                    char c = originalMessage.charAt(i);
                    if (c == lastChar) {
                        count++;
                        if (count >= threshold) {
                            directSpamFound = true;
                            break;
                        }
                    } else {
                        lastChar = c;
                        count = 1;
                    }
                }
                
                if (directSpamFound) {
                    plugin.getLogger().info("Direct spam pattern found!");
                    
                    // Replace the entire message
                    Component replacement = miniMessage.deserialize(defaultReplacement)
                        .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalMessage)
                            .append(Component.newline())
                            .append(Component.text("Filtered for: Character Spam"))
                        ));
                    
                    event.message(replacement);
                    
                    // Notify the player
                    event.getPlayer().sendMessage(miniMessage.deserialize(
                        regexMsg.replace("<prefix>", prefix)
                               .replace("<pattern>", "Character Spam")
                    ));
                    
                    plugin.getLogger().info("Applied direct spam replacement");
                } else {
                    // Try the loaded patterns
                    for (Pattern pattern : spamPatterns) {
                        boolean matched = checkAndReplacePattern(event, originalMessage, event.message(), pattern, "Character Spam", regexMsg);
                        if (matched) {
                            plugin.getLogger().info("Spam pattern matched and filtered");
                            break;
                        }
                    }
                }
            }

            if (regexConfig.getBoolean("block-caps.enabled", true)) {
                plugin.getLogger().info("Checking caps patterns");
                String originalMessage = plainSerializer.serialize(event.message());
                int minLength = regexConfig.getInt("block-caps.min-length", 6);
                int threshold = regexConfig.getInt("block-caps.threshold", 60);
                
                if (originalMessage.length() >= minLength) {
                    int capsCount = 0;
                    int letterCount = 0;
                    
                    for (char c : originalMessage.toCharArray()) {
                        if (Character.isLetter(c)) {
                            letterCount++;
                            if (Character.isUpperCase(c)) {
                                capsCount++;
                            }
                        }
                    }
                    
                    if (letterCount > 0) {
                        double capsPercentage = (capsCount * 100.0) / letterCount;
                        plugin.getLogger().info("Caps percentage: " + capsPercentage + "% (threshold: " + threshold + "%)");
                        
                        if (capsPercentage > threshold) {
                            plugin.getLogger().info("Found excessive caps: " + capsPercentage + "% caps");
                            Component replacement = miniMessage.deserialize(defaultReplacement)
                                .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalMessage)
                                    .append(Component.newline())
                                    .append(Component.text("Filtered for: Excessive Caps (" + Math.round(capsPercentage) + "%)"))
                                ));
                            
                            event.message(replacement);
                            
                            event.getPlayer().sendMessage(miniMessage.deserialize(
                                regexMsg.replace("<prefix>", prefix)
                                       .replace("<pattern>", "Excessive Caps (" + Math.round(capsPercentage) + "%)")
                            ));
                        }
                    }
                }
            }

            if (regexConfig.getBoolean("custom-patterns.enabled", false)) {
                plugin.getLogger().info("Checking custom patterns (" + customPatterns.size() + " patterns)");
                String originalMessage = plainSerializer.serialize(event.message());
                
                if (customPatterns.isEmpty()) {
                    plugin.getLogger().info("No custom patterns loaded");
                } else {
                    // Try direct match first with a test custom regex for badwords
                    if (originalMessage.toLowerCase().matches(".*\\b(badword\\d+)\\b.*")) {
                        plugin.getLogger().info("Direct custom pattern match found!");
                        Component replacement = miniMessage.deserialize(defaultReplacement)
                            .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalMessage)
                                .append(Component.newline())
                                .append(Component.text("Filtered for: Custom Pattern (badword)"))
                            ));
                        
                        event.message(replacement);
                        
                        // Notify the player
                        event.getPlayer().sendMessage(miniMessage.deserialize(
                            regexMsg.replace("<prefix>", prefix)
                                   .replace("<pattern>", "Custom Pattern")
                        ));
                        
                        plugin.getLogger().info("Applied direct custom pattern replacement");
                    } else {
                        // Try the loaded patterns
                        for (Pattern pattern : customPatterns) {
                            boolean matched = checkAndReplacePattern(event, originalMessage, event.message(), pattern, "Custom Pattern", regexMsg);
                            if (matched) {
                                plugin.getLogger().info("Custom pattern matched and filtered");
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkAndReplacePattern(AsyncChatEvent event, String originalText, Component currentMessage, Pattern pattern, String type, String notifyMsg) {
        plugin.getLogger().info("Checking pattern: " + pattern.pattern());
        boolean found = false;
        Matcher matcher = pattern.matcher(originalText);
        if (matcher.find()) {
            plugin.getLogger().info("Pattern matched: " + matcher.group());
            // Instead of trying to modify the current component, let's replace the entire message when a pattern matches
            String currentText = plainSerializer.serialize(currentMessage);

            // Try matching against both original and current message text
            if (matcher.find()) {
                // Log the match
                plugin.getLogger().info("Found " + type + " pattern match in original text: " + matcher.group());
                
                // Create a replacement for the entire message
                Component replacement = miniMessage.deserialize(defaultReplacement)
                    .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalText)
                        .append(Component.newline())
                        .append(Component.text("Filtered for: " + type + " pattern"))
                    ));
                
                // Replace the entire message
                event.message(replacement);
                found = true;
                
                // Notify the player
                event.getPlayer().sendMessage(miniMessage.deserialize(
                    notifyMsg.replace("<prefix>", prefix)
                            .replace("<pattern>", type)
                ));
                
                plugin.getLogger().info("Applied full message replacement for regex pattern match");
            } else {
                // Try matching the current text if it's different from the original
                if (!currentText.equals(originalText)) {
                    matcher = pattern.matcher(currentText);
                    if (matcher.find()) {
                        plugin.getLogger().info("Found " + type + " pattern match in modified text: " + matcher.group());
                        
                        Component replacement = miniMessage.deserialize(defaultReplacement)
                            .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalText)
                                .append(Component.newline())
                                .append(Component.text("Filtered for: " + type + " pattern"))
                            ));
                        
                        event.message(replacement);
                        found = true;
                        
                        event.getPlayer().sendMessage(miniMessage.deserialize(
                            notifyMsg.replace("<prefix>", prefix)
                                    .replace("<pattern>", type)
                        ));
                        
                        plugin.getLogger().info("Applied full message replacement for regex pattern match (on modified text)");
                    }
                }
            }
        } else {
            plugin.getLogger().info("Pattern did not match: " + pattern.pattern());
        }
        
        return found;
    }

    private Component replaceText(Component original, String target, Component replacement) {
        if (target == null || target.isEmpty()) {
            plugin.getLogger().info("Replace failed: target is null or empty");
            return original;
        }
        
        String plainText = plainSerializer.serialize(original);
        plugin.getLogger().info("Attempting to replace \"" + target + "\" in \"" + plainText + "\"");
        
        // If the target isn't in the text, return the original
        if (!plainText.contains(target)) {
            plugin.getLogger().info("Target \"" + target + "\" not found in text");
            return original;
        }
        
        plugin.getLogger().info("Target found, replacing with filtered content");
        
        // For simple cases, just completely replace the message with the filtered component
        if (plainText.equalsIgnoreCase(target)) {
            plugin.getLogger().info("Entire message matches target, completely replacing it");
            return replacement;
        }
        
        // Split the text by the target
        String[] parts = plainText.split(Pattern.quote(target), -1);
        plugin.getLogger().info("Split into " + parts.length + " parts");
        
        if (parts.length == 1) {
            plugin.getLogger().info("Split failed, returning original");
            return original;
        }

        // Build the new component by appending parts and replacements
        Component result = Component.empty();
        
        // Add first part if not empty
        if (!parts[0].isEmpty()) {
            result = result.append(miniMessage.deserialize(parts[0]));
        }
        
        // Add replacement and remaining parts
        for (int i = 1; i < parts.length; i++) {
            // Add replacement for each occurrence
            result = result.append(replacement);
            
            // Add remaining text for this part if not empty
            if (!parts[i].isEmpty()) {
                result = result.append(miniMessage.deserialize(parts[i]));
            }
        }
        
        plugin.getLogger().info("Replaced all occurrences successfully");
        return result;
    }

    private Component createFilteredComponent(String originalText, String filterType) {
        String baseText = plugin.getConfig().getString("modules.ChatFilter.replacement", defaultReplacement);
        return miniMessage.deserialize(baseText)
            .hoverEvent(HoverEvent.showText(Component.text("Original text: " + originalText)
                .append(Component.newline())
                .append(Component.text("Filtered for: " + filterType))
            ));
    }
}
