package me.pilkeysek.skyeNetP.handlers;

import me.pilkeysek.skyeNetP.SkyeNetP;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class SkyeNetVHandler implements PluginMessageListener {
    
    private final SkyeNetP plugin;
    
    public SkyeNetVHandler(SkyeNetP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals("skyenetv:teleport")) {
            handleTeleportCommand(player, message);
        }
    }
    
    private void handleTeleportCommand(Player player, byte[] message) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(message);
            DataInputStream dataStream = new DataInputStream(inputStream);

            String command = dataStream.readUTF();  // Should be "TeleportSpawn"
            
            if (!command.equals("TeleportSpawn")) {
                return; // Invalid command
            }

            double x = dataStream.readDouble();
            double y = dataStream.readDouble();
            double z = dataStream.readDouble();
            float yaw = dataStream.readFloat();
            float pitch = dataStream.readFloat();

            // Teleport player to spawn coordinates
            Location spawnLocation = new Location(
                player.getWorld(), x, y, z, yaw, pitch
            );
            
            player.teleport(spawnLocation);
            
            // Send confirmation message using the plugin's message system
            try {
                player.sendMessage(plugin.getMessage("teleport.spawn"));
            } catch (Exception e) {
                // Fallback message if the message system fails
                player.sendMessage("§aTeleported to spawn!");
            }
            
            plugin.getLogger().info("Teleported " + player.getName() + " to spawn location via SkyeNetV");

        } catch (IOException e) {
            plugin.getLogger().severe("Error processing teleport command from SkyeNetV: " + e.getMessage());
        }
    }
}
