package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.serializing.SettingsSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BodyHandler implements Listener {

    public static void claimBody(Player player, BodySerializer.BodyInfo body) {
        Bukkit.getOnlinePlayers().forEach(body.body::destroy);

        for (UUID interaction : body.interactions) body.loc.getWorld().getEntity(interaction).remove();
        body.loc.getWorld().getEntity(body.textDisplay).remove();

        body.loc.getWorld().spawnParticle(Particle.POOF, body.loc, 3, 0, 0, 0, 0.25);

        ItemStack[] bodyContents = body.items;
        ItemStack[] playerContents = player.getInventory().getContents();
        ItemStack[] finalContents = new ItemStack[bodyContents.length];

        List<ItemStack> toAdd = new ArrayList<>();

        for (int j = 0; j < finalContents.length; j++) {
            if (bodyContents[j] == null) finalContents[j] = playerContents[j];
            else if (playerContents[j] == null) finalContents[j] = bodyContents[j];
            else if (playerContents[j].isSimilar(bodyContents[j])) {
                int totalSize = playerContents[j].getAmount() + bodyContents[j].getAmount();
                int maxSize = playerContents[j].getMaxStackSize();

                if (totalSize > maxSize) {
                    playerContents[j].setAmount(maxSize);
                    finalContents[j] = playerContents[j];

                    bodyContents[j].setAmount(totalSize - maxSize);
                    toAdd.add(bodyContents[j]);
                } else {
                    playerContents[j].setAmount(totalSize);
                    finalContents[j] = playerContents[j];
                }
            } else if (SettingsSerializer.getSettings(player.getUniqueId()).prioritizeInv()) {
                finalContents[j] = playerContents[j];
                toAdd.add(bodyContents[j]);
            } else {
                finalContents[j] = bodyContents[j];
                toAdd.add(playerContents[j]);
            }
        }

        player.getInventory().setContents(finalContents);
        for (ItemStack it : player.getInventory().addItem(toAdd.toArray(ItemStack[]::new)).values()) {
            player.getWorld().dropItem(player.getLocation(), it);
        }

        player.giveExp(body.exp);

        BodySerializer.removeBody(body);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent evt) {
        BodySerializer.BodyInfo body;

        if (evt.getPlayer().isSneaking() && evt.getRightClicked() instanceof Interaction i && (body = BodySerializer.getBody(i)) != null) {
            if (!evt.getPlayer().getUniqueId().equals(body.player) &&
                    !SettingsSerializer.getSettings(body.player).trusted().contains(evt.getPlayer().getUniqueId())) {
                evt.getPlayer().sendActionBar(Component.text("You are not trusted to open this body!").color(NamedTextColor.RED));

                return;
            }

            claimBody(evt.getPlayer(), body);
        }
    }
}
