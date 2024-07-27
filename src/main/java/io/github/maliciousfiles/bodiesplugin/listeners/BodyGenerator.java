package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.util.Body;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent evt) {
        ItemStack[] contents = evt.getPlayer().getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (!evt.getDrops().contains(contents[i])) contents[i] = null;
        }
        evt.getDrops().clear();


        BodySerializer.addBody(spawnBody(evt.getEntity().getUniqueId(), evt.getDeathMessage(), evt.getEntity().getLocation(), contents, evt.getDroppedExp(), false));
        evt.setDroppedExp(0);
    }

    private static BodySerializer.BodyInfo spawnBody(UUID player, String message, Location location, ItemStack[] items, int exp, boolean noZombie) {
        Body body = new Body(location, player);
        Bukkit.getOnlinePlayers().forEach(body::spawn);

        UUID[] interactions = new UUID[4];
        for (int i = 0; i < 4; i++) {
            double rot = Math.toRadians(location.getYaw());
            Location loc = location.add(-Math.sin(rot)*0.84375, 0, Math.cos(rot)*0.84375)
                    .subtract(-Math.sin(rot)*0.5*i, 0, Math.cos(rot)*0.5*i);

            Interaction interaction = loc.getWorld().spawn(loc, Interaction.class);
            interaction.setInteractionWidth(0.5f);
            interaction.setInteractionHeight(0.25f);
            interaction.setResponsive(false);

            interactions[i] = interaction.getUniqueId();
        }

        TextDisplay textDisplay = location.getWorld().spawn(location.add(0, 0.5, 0), TextDisplay.class);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.text(Component.text(message));

        return new BodySerializer.BodyInfo(player, message, location, items, exp, interactions, textDisplay.getUniqueId(), System.currentTimeMillis(), body, noZombie);
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent evt) {
        BodySerializer.BodyInfo body;
        if (!(evt.getEntity() instanceof Zombie zombie) || (body = BodySerializer.getZombieInfo(zombie.getUniqueId())) == null) return;

        BodySerializer.removeZombie(zombie.getUniqueId());
        BodySerializer.addBody(spawnBody(body.player, body.message, zombie.getLocation(), body.items, body.exp, true));
    }
}
