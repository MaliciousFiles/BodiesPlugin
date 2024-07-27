package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.serializing.SettingsSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BodyHandler implements Listener {

    private static void destroyBody(BodySerializer.BodyInfo body) {
        Bukkit.getOnlinePlayers().forEach(body.body::destroy);

        for (UUID interaction : body.interactions) body.loc.getWorld().getEntity(interaction).remove();
        body.loc.getWorld().getEntity(body.textDisplay).remove();

        body.loc.getWorld().spawnParticle(Particle.POOF, body.loc, 3, 0, 0, 0, 0.25);

        BodySerializer.removeBody(body);
    }

    public static void claimBody(Player player, BodySerializer.BodyInfo body) {
        destroyBody(body);

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
    }

    private static final List<String> MATERIAL_ORDER = List.of("WOODEN", "STONE", "IRON", "DIAMOND", "NETHERITE");
    public static void spawnZombie(BodySerializer.BodyInfo body) {
        destroyBody(body);

        Zombie zombie = body.loc.getWorld().spawn(body.loc, Zombie.class);
        zombie.setAdult();
        for (EquipmentSlot slot : EquipmentSlot.values()) zombie.getEquipment().setDropChance(slot, 0);

        BodySerializer.addZombie(zombie.getUniqueId(), body);

        zombie.getEquipment().setArmorContents(Arrays.copyOfRange(body.items, 36, 40));

        ItemStack weapon = null;
        for (ItemStack item : body.items) {
            if (item == null) continue;

            String[] name = item.getType().name().split("_");
            if (name.length < 2 || !name[1].equals("SWORD") && !name[1].equals("AXE")) continue;

            int curIdx = weapon == null ? -1 : MATERIAL_ORDER.indexOf(weapon.getType().name().split("_")[0]);
            int newIdx = MATERIAL_ORDER.indexOf(name[0]);
            if (newIdx > curIdx || (newIdx == curIdx && item.getEnchantments().size() > weapon.getEnchantments().size())) {
                weapon = item.clone();
            }
        }

        zombie.getEquipment().setItemInMainHand(weapon);
        zombie.getEquipment().setItemInOffHand(body.items[40]);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent evt) {
        BodySerializer.BodyInfo body;

        if (evt.getPlayer().isSneaking() && evt.getRightClicked() instanceof Interaction i && (body = BodySerializer.getBody(i)) != null) {
            if (!BodiesPlugin.instance.getConfig().getBoolean("enableStealing") &&
                    !evt.getPlayer().getUniqueId().equals(body.player) &&
                    !SettingsSerializer.getSettings(body.player).trusted().contains(evt.getPlayer().getUniqueId())) {
                evt.getPlayer().sendActionBar(Component.text("You are not trusted to open this body!").color(NamedTextColor.RED));
                return;
            }

            if (!body.noZombie && Math.random() < BodiesPlugin.instance.getConfig().getDouble("zombieChance")) spawnZombie(body);
            else claimBody(evt.getPlayer(), body);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent evt) {
        List<BodySerializer.BodyInfo> bodies = BodySerializer.getBodiesForPlayer(evt.getPlayer());
        if (bodies == null) return;

        double radius = BodiesPlugin.instance.getConfig().getDouble("glowRadius");
        for (BodySerializer.BodyInfo body : bodies) {
            if (body.loc.distanceSquared(evt.getPlayer().getLocation()) <= radius*radius) {
                body.body.glow(evt.getPlayer());
            } else {
                body.body.deglow(evt.getPlayer());
            }
        }
    }
}
