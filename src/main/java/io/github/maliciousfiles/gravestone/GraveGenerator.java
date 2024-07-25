package io.github.maliciousfiles.gravestone;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

public class GraveGenerator implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent evt) {
        Location loc = evt.getPlayer().getLocation();

        Player player = (Player) loc.getWorld().getEntity(UUID.fromString("f0c7f7bb-7407-4dd7-9230-e074ed7c335d"));
        (())

        loc.getWorld().spawn(loc.add(0.5, 1.5, 0.5), TextDisplay.class, text -> {
            text.setAlignment(TextDisplay.TextAlignment.CENTER);
            text.text(evt.deathMessage());
            text.setBillboard(Display.Billboard.CENTER);


            GraveSerializer.addGrave(new Grave(loc, evt.getEntity().getUniqueId(), text.getUniqueId(), evt.deathMessage().toString(), evt.getDrops()));
        });

        GravestonePlugin.instance.getLogger().log(Level.INFO, Arrays.toString(evt.getPlayer().getInventory().getContents()));

        evt.getDrops().clear();
    }
}
