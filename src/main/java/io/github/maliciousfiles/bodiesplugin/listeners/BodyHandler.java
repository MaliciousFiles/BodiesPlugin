package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import io.github.maliciousfiles.bodiesplugin.BodySerializer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public class BodyHandler implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent evt) {
        BodySerializer.BodyInfo body;

        if (evt.getPlayer().isSneaking() && evt.getRightClicked() instanceof Interaction i && (body = BodySerializer.getBody(i)) != null) {
            Bukkit.getOnlinePlayers().forEach(body.body::destroy);

            for (UUID interaction : body.interactions) body.loc.getWorld().getEntity(interaction).remove();
            body.loc.getWorld().getEntity(body.textDisplay).remove();

            body.loc.getWorld().spawnParticle(Particle.POOF, body.loc, 3, 0, 0, 0, 0.25);

            BodySerializer.removeBody(body);
        }
    }
}
