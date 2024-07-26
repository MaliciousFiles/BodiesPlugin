package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.util.Body;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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
        Body body = new Body(evt.getEntity().getLocation(), evt.getEntity().getUniqueId());
        Bukkit.getOnlinePlayers().forEach(body::spawn);

        UUID[] interactions = new UUID[4];
        for (int i = 0; i < 4; i++) {
            Location loc = evt.getEntity().getLocation();
            double rot = Math.toRadians(loc.getYaw());
            loc = loc.add(-Math.sin(rot)*0.84375, 0, Math.cos(rot)*0.84375)
                    .subtract(-Math.sin(rot)*0.5*i, 0, Math.cos(rot)*0.5*i);

            Interaction interaction = evt.getEntity().getWorld().spawn(loc, Interaction.class);
            interaction.setInteractionWidth(0.5f);
            interaction.setInteractionHeight(0.25f);
            interaction.setResponsive(false);

            interactions[i] = interaction.getUniqueId();
        }

        TextDisplay textDisplay = evt.getEntity().getWorld().spawn(evt.getEntity().getLocation().add(0, 0.5, 0), TextDisplay.class);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.text(evt.deathMessage());

        BodySerializer.addBody(new BodySerializer.BodyInfo(evt.getPlayer().getUniqueId(),
                evt.getPlayer().getLocation(), interactions, textDisplay.getUniqueId(), System.currentTimeMillis(), body));
    }
}
