package io.github.maliciousfiles.bodiesplugin.listeners;

import io.github.maliciousfiles.bodiesplugin.util.Body;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.List;

public class BodyGenerator implements Listener {
    private static final PlayerTeam TEAM = new PlayerTeam(null, "BODIES") {
        public Team.Visibility getNameTagVisibility() { return Team.Visibility.NEVER; }
        public Team.CollisionRule getCollisionRule() { return Team.CollisionRule.NEVER; }
        public Collection<String> getPlayers() { return List.of(""); }
    };

    @EventHandler
    public void onJoin(PlayerJoinEvent evt) {
        ServerPlayer sp = ((CraftPlayer) evt.getPlayer()).getHandle();

//        sp.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(TEAM, false));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent evt) {
        Body body = new Body();
        body.loc = evt.getPlayer().getLocation();

        body.spawn(evt.getPlayer());
    }
}
