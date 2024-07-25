package io.github.maliciousfiles.gravestone;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class GraveHandler implements Listener {

    private boolean claimGrave(Player player, Grave grave) {
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent evt) {
        if (evt.getAction() == Action.LEFT_CLICK_BLOCK) {
            Grave grave = GraveSerializer.getGrave(evt.getClickedBlock().getLocation());
            if (grave == null) return;

            evt.getPlayer().sendActionBar(Component.text(grave.message));
            evt.setCancelled(true);
        } else if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Grave grave = GraveSerializer.getGrave(evt.getClickedBlock().getLocation());
            if (grave == null) return;

            if (claimGrave(evt.getPlayer(), grave)) evt.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent evt) {
        if (!evt.isSneaking()) return;

        Grave grave = GraveSerializer.getGrave(evt.getPlayer().getLocation().add(0, -1, 0));
        if (grave == null) return;

        claimGrave(evt.getPlayer(), grave);
    }
}
