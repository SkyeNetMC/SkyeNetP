package me.pilkeysek.skyeNetP.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinMessageListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Paper (Adventure) join message suppression
        event.joinMessage(null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Paper (Adventure) quit message suppression
        event.quitMessage(null);
    }
}
