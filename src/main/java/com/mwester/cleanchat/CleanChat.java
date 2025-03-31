package com.mwester.cleanchat;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CleanChat extends JavaPlugin implements Listener {

    private List<Pattern> blockedWordPatterns;
    private String warningMessage;
    private boolean blockWebsites;
    private boolean blockIPs;
    private boolean debugMode;

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?[\\w\\-]+\\.(com|net|org|io|gg|co|biz|info|xyz|us|uk|me|edu|gov|au|de|fr|ca|in)\\b");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        List<String> blockedWords = config.getStringList("blocked_words");
        warningMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("warning_message", "&cPlease watch your language!"));
        blockWebsites = config.getBoolean("block_websites", true);
        blockIPs = config.getBoolean("block_ips", true);
        debugMode = config.getBoolean("debug_mode", false);

        blockedWordPatterns = blockedWords.stream()
                .map(word -> Pattern.compile(Pattern.quote(word.toLowerCase()), Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CleanChat has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CleanChat has been disabled.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message()).toLowerCase();

        if (debugMode) {
            getLogger().info("Raw: " + event.message().toString());
            getLogger().info("Plain: " + plainMessage);
        }

        for (Pattern pattern : blockedWordPatterns) {
            if (pattern.matcher(plainMessage).find()) {
                cancelChat(event);
                return;
            }
        }

        if (blockWebsites && URL_PATTERN.matcher(plainMessage).find()) {
            cancelChat(event);
            return;
        }

        if (blockIPs && IP_PATTERN.matcher(plainMessage).find()) {
            cancelChat(event);
        }
    }

    private void cancelChat(AsyncChatEvent event) {
        event.setCancelled(true);
        event.viewers().clear(); // Prevent message from being sent
        event.getPlayer().sendMessage(warningMessage);
    }
}