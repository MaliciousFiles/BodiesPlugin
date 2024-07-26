package io.github.maliciousfiles.bodiesplugin.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public class Body {
    private Marker marker;

    public void spawn(Player player) {
        ServerPlayer sp = ((CraftPlayer) player).getHandle();

        //new ClientboundPlayerInfoUpdatePacket()
        //new ClientboundPlayerAbilitiesPacket()
        //new ClientboundPlayerPositionPacket
        //new ClientboundAddEntityPacket()
        //new ClientboundSetEntityDataPacket()
        //new ClientboundMoveEntityPacket()
        sp.connection.send(new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                new ServerPlayer(sp.getServer(), sp.serverLevel(), new GameProfile(
                        sp.getGameProfile().getId(), sp.getGameProfile().getName()),
                        sp.clientInformation()
                )));
    }
}
