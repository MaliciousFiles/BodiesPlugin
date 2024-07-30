package io.github.maliciousfiles.bodiesplugin.listeners;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.util.Body;
import io.github.maliciousfiles.bodiesplugin.util.CustomPacketListener;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftZombie;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class BodyGenerator implements Listener {
    private static final PlayerTeam TEAM = new PlayerTeam(null, "") {
        public Team.Visibility getNameTagVisibility() { return Team.Visibility.NEVER; }
        public Team.CollisionRule getCollisionRule() { return Team.CollisionRule.NEVER; }
        public Collection<String> getPlayers() { return List.of(""); }
    };

    @EventHandler
    public void onJoin(PlayerJoinEvent evt) {
        ServerPlayer sp = ((CraftPlayer) evt.getPlayer()).getHandle();

        sp.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(TEAM, false));
        sp.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(TEAM, true));

        BodySerializer.getAllBodies().forEach(b->b.body.spawn(evt.getPlayer()));

        replaceConnection(evt.getPlayer());
    }

    public static void replaceConnection(Player p) {
        ServerPlayer sp = ((CraftPlayer) p).getHandle();

        ServerGamePacketListenerImpl listener = new CustomPacketListener(sp.server, sp.connection.connection, sp,
                new CommonListenerCookie(sp.gameProfile, sp.connection.latency(), sp.clientInformation(), sp.connection.isTransferred()));

        try {
            Field field = Connection.class.getDeclaredField("packetListener");
            field.setAccessible(true);
            field.set(sp.connection.connection, listener);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent evt) {
        int exp = evt.getDroppedExp();
        List<ItemStack> drops = List.copyOf(evt.getDrops());
        ItemStack[] contents = evt.getPlayer().getInventory().getContents();

        evt.getDrops().clear();
        evt.setDroppedExp(0);

        Bukkit.getScheduler().runTask(BodiesPlugin.instance, () -> {
            if (evt.getPlayer().getGameMode() == GameMode.SPECTATOR) return;

            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && !drops.contains(contents[i])) contents[i] = null;
            }

            Difficulty difficulty = evt.getPlayer().getWorld().getDifficulty();
            boolean isZombie = evt.getPlayer().getGameMode() != GameMode.CREATIVE &&
                    difficulty != Difficulty.PEACEFUL &&
                    Math.random() < BodiesPlugin.instance.getConfig().getDouble(difficulty.name().toLowerCase() + "ZombieChance");

            BodySerializer.addBody(spawnBody(evt.getEntity().getUniqueId(), evt.getDeathMessage(), evt.getEntity().getLocation(), evt.getPlayer().getInventory().getHeldItemSlot(), contents, exp, isZombie));
        });
    }

    private static BodySerializer.BodyInfo spawnBody(UUID player, String message, Location location, int selectedItem, ItemStack[] items, int exp, boolean isZombie) {
        Body body = new Body(location.clone(), player, items, selectedItem);
        Bukkit.getOnlinePlayers().forEach(p -> {
            BodyHandler.checkRadius(p);
            body.spawn(p);
        });

        UUID[] interactions = new UUID[4];
        for (int i = 0; i < 4; i++) {
            double rot = Math.toRadians(location.getYaw());
            Location loc = location.clone().add(-Math.sin(rot)*0.84375, 0, Math.cos(rot)*0.84375)
                    .subtract(-Math.sin(rot)*0.5*i, 0, Math.cos(rot)*0.5*i);

            Interaction interaction = loc.getWorld().spawn(loc, Interaction.class);
            interaction.setInteractionWidth(0.5f);
            interaction.setInteractionHeight(0.25f);
            interaction.setResponsive(false);

            interactions[i] = interaction.getUniqueId();
        }

        TextDisplay textDisplay = location.getWorld().spawn(location.clone().add(0, 0.5, 0), TextDisplay.class);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.text(Component.text(message));

        return new BodySerializer.BodyInfo(player, message, location.clone(), selectedItem, items, exp, interactions, textDisplay.getUniqueId(), System.currentTimeMillis(), body, isZombie);
    }

    @EventHandler
    public void onRemoval(EntityRemoveFromWorldEvent evt) {
        if (evt.getEntity() instanceof Zombie zombie) {
            net.minecraft.world.entity.Entity.RemovalReason reason = ((CraftZombie) zombie).getHandle().getRemovalReason();
            if (!reason.shouldDestroy()) return;

            revertToBody(zombie, reason != net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        }
    }

    public static void revertToBody(Zombie zombie, boolean isZombie) {
        if (zombie.isValid()) zombie.remove();
        BodySerializer.BodyInfo body = BodySerializer.getZombieInfo(zombie.getUniqueId());
        if (body == null) return;

        BodySerializer.removeZombie(zombie.getUniqueId());
        BodySerializer.addBody(spawnBody(body.player, body.message, zombie.getLocation(), body.selectedItem, body.items, body.exp, isZombie));
    }
}
