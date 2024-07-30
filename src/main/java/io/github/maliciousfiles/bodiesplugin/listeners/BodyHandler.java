package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.serializing.SettingsSerializer;
import io.github.maliciousfiles.bodiesplugin.util.CustomZombie;
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class BodyHandler implements Listener {

    private static void destroyBody(BodySerializer.BodyInfo body) {
        Bukkit.getOnlinePlayers().forEach(body.body::destroy);

        for (UUID interaction : body.interactions) Optional.ofNullable(body.loc.getWorld().getEntity(interaction)).ifPresent(Entity::remove);
        Optional.ofNullable(body.loc.getWorld().getEntity(body.textDisplay)).ifPresent(Entity::remove);

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
            } else if (SettingsSerializer.getSettings(player.getUniqueId()).prioritizeInv() || !player.getUniqueId().equals(body.player)) {
                finalContents[j] = playerContents[j];
                toAdd.add(bodyContents[j]);
            } else {
                finalContents[j] = bodyContents[j];
                toAdd.add(playerContents[j]);
            }
        }

        player.getInventory().setContents(finalContents);
        for (ItemStack it : player.getInventory().addItem(toAdd.toArray(ItemStack[]::new)).values()) {
            player.getWorld().dropItem(player.getLocation(), it, e -> e.setVelocity(new Vector()));
        }

        player.giveExp(body.exp);
    }

    private static final List<String> MATERIAL_ORDER = List.of("WOODEN", "STONE", "IRON", "DIAMOND", "NETHERITE");
    public static void spawnZombie(BodySerializer.BodyInfo body) {
        destroyBody(body);
        body.loc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, body.loc, 3, 0, 0, 0, 0.25);

        Zombie zombie = (Zombie) new CustomZombie(((CraftWorld) body.loc.getWorld()).getHandle()).getBukkitEntity();

        body.body.setReplacing(zombie);
        BodySerializer.addZombie(zombie.getUniqueId(), body);

        zombie.spawnAt(body.loc);

        zombie.setCanPickupItems(false);
        zombie.setShouldBurnInDay(false);
        zombie.setCustomName(Bukkit.getOfflinePlayer(body.player).getName());
        zombie.setCustomNameVisible(true);
        zombie.setRemoveWhenFarAway(false);
        zombie.setAdult();
        for (EquipmentSlot slot : EquipmentSlot.values()) zombie.getEquipment().setDropChance(slot, 0);

        zombie.getEquipment().setArmorContents(Arrays.copyOfRange(body.items, 36, 40));

        int weaponIdx = body.selectedItem;
        ItemStack weapon = body.items[body.selectedItem];
        for (int i = 0; i < body.items.length; i++) {
            ItemStack item = body.items[i];
            if (item == null) continue;

            String[] name = item.getType().name().split("_");
            if (name.length < 2 || !name[1].equals("SWORD") && !name[1].equals("AXE")) continue;

            float curIdx = weapon == null ? -1 : MATERIAL_ORDER.indexOf(weapon.getType().name().split("_")[0]);
            if (weapon != null && weapon.getType().name().split("_")[1].equals("AXE")) curIdx += 0.5f;

            float newIdx = MATERIAL_ORDER.indexOf(name[0]);
            if (name[1].equals("AXE")) newIdx += 0.5f;

            if (newIdx > curIdx || (newIdx == curIdx && item.getEnchantments().size() > weapon.getEnchantments().size())) {
                weapon = item.clone();
                weaponIdx = i;
            }
        }

        zombie.getEquipment().setItemInMainHand(weapon);
        zombie.getEquipment().setItemInOffHand(body.items[40]);

        BodySerializer.addZombie(zombie.getUniqueId(), new BodySerializer.BodyInfo(
                body.player, body.message, body.loc, weaponIdx,
                body.items, body.exp, body.interactions, body.textDisplay,
                body.timestamp, body.body, true
        ));
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

            if (body.isZombie && evt.getPlayer().getGameMode() != GameMode.CREATIVE && body.loc.getWorld().getDifficulty() != Difficulty.PEACEFUL) spawnZombie(body);
            else claimBody(evt.getPlayer(), body);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent evt) {
        if (!evt.hasChangedPosition()) return;

        checkRadius(evt.getPlayer());
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent evt) {
        BodySerializer.BodyInfo body;
        if ((body = BodySerializer.getZombieInfo(evt.getEntity().getUniqueId())) == null) return;

        double radius = BodiesPlugin.instance.getConfig().getDouble("glowRadius");
        for (Player player : Bukkit.getOnlinePlayers()) {
            body.body.setWithinRadius(player, evt.getEntity().getLocation().distanceSquared(player.getLocation()) <= radius*radius);
        }

        if (body.body.noneWithinRadius() && evt.getEntity().isOnGround() && evt.getEntity().isValid()) evt.getEntity().remove();
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent evt) {
        checkRadius(evt.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent evt) {
        checkRadius(evt.getPlayer());

        helpNewPlayer(evt.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent evt) {
        checkRadius(evt.getPlayer());
    }

    public static void helpNewPlayer(Player player) {
        if (!SettingsSerializer.hasSettings(player.getUniqueId())) {
            SettingsSerializer.getSettings(player.getUniqueId());
            player.performCommand("bodies help");
        }

    }

    public static void checkRadius(Player player) {
        double radius = BodiesPlugin.instance.getConfig().getDouble("glowRadius");
        for (BodySerializer.BodyInfo body : BodySerializer.getAllBodies()) {
            body.body.setWithinRadius(player, body.loc.distanceSquared(player.getLocation()) <= radius*radius);
        }

        for (UUID zombie : BodySerializer.getAllZombies()) {
            BodySerializer.BodyInfo body = BodySerializer.getZombieInfo(zombie);
            Zombie entity = (Zombie) Bukkit.getEntity(zombie);
            body.body.setWithinRadius(player, entity.getLocation().distanceSquared(player.getLocation()) <= radius*radius);

            if (body.body.noneWithinRadius() && entity.isOnGround()) entity.remove();
        }
    }
}
