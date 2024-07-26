package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.util.Body;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class BodyGenerator implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent evt) {
        Body body = new Body();
        body.loc = evt.getPlayer().getLocation();

        body.spawn(evt.getPlayer());
    }
}
