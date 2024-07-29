package io.github.maliciousfiles.bodiesplugin.util;

import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CustomPacketListener extends ServerGamePacketListenerImpl {
    public CustomPacketListener(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
        super(server, connection, player, clientData);
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener callbacks) {
        BodySerializer.BodyInfo body;
        if (packet instanceof ClientboundAddEntityPacket add && (body = BodySerializer.getZombieInfo(add.getUUID())) != null) {
            packet = body.body.getReplacePacket(this.getCraftPlayer());
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            for (Packet<?> p : bundle.subPackets()) {
                if (p instanceof ClientboundAddEntityPacket add && (body = BodySerializer.getZombieInfo(add.getUUID())) != null) {
                    super.send(body.body.getReplacePacket(this.getCraftPlayer()), null);
                } else {
                    packets.add((Packet<? super ClientGamePacketListener>) p);
                }
            }

            packet = new ClientboundBundlePacket(packets);
        }

        super.send(packet, callbacks);
    }
}
