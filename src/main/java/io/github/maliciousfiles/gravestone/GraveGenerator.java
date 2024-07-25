package io.github.maliciousfiles.gravestone;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class GraveGenerator implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent evt) {
        Location loc = evt.getEntity().getLocation();
        while (!loc.getBlock().getType().isAir() && loc.getBlockY() < loc.getWorld().getMaxHeight()) {
            loc = loc.add(0, 1, 0);
        }
        if (!loc.getBlock().getType().isAir()) loc = evt.getEntity().getLocation();

        loc.getBlock().setType(Material.COMPOSTER);

        evt.getDrops().clear();
    }
}
