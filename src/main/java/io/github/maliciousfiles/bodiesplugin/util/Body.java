package io.github.maliciousfiles.bodiesplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Body {
    private final List<Packet<? super ClientGamePacketListener>> spawnPackets = new ArrayList<>();
    private final List<Packet<? super ClientGamePacketListener>> destroyPackets = new ArrayList<>();
    private final List<Packet<? super ClientGamePacketListener>> glowPackets = new ArrayList<>();
    private final List<Packet<? super ClientGamePacketListener>> deglowPackets = new ArrayList<>();

    public Body(Location loc, UUID dead) {
        ServerLevel level = ((CraftWorld) loc.getWorld()).getHandle();

        ServerPlayer fakePlayer = new ServerPlayer(level.getServer(),
                level, new GameProfile(dead, ""),
                ClientInformation.createDefault());

        fakePlayer.setUUID(UUID.randomUUID());
        setSkin(fakePlayer.gameProfile);
        fakePlayer.setPos(CraftLocation.toVec3D(/*marker.getLocation()*/loc));;
        fakePlayer.setYRot(-90-loc.getYaw());
        fakePlayer.setYHeadRot(fakePlayer.getYRot());
        fakePlayer.setPose(Pose.SLEEPING);

        spawnPackets.add(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                new ClientboundPlayerInfoUpdatePacket.Entry(
                        fakePlayer.getUUID(), fakePlayer.getGameProfile(), false, 0,
                        GameType.CREATIVE, fakePlayer.getDisplayName(), null)));

        spawnPackets.add(new ClientboundAddEntityPacket(
                fakePlayer.getId(),
                fakePlayer.getUUID(),
                fakePlayer.getX() - Mth.sin((float) Math.toRadians(loc.getYaw())), fakePlayer.getY()+0.125, fakePlayer.getZ() + Mth.cos((float) Math.toRadians(loc.getYaw())),
                fakePlayer.getXRot(), fakePlayer.getYRot(),
                fakePlayer.getType(), 0,
                fakePlayer.getDeltaMovement(), fakePlayer.getYHeadRot()
        ));

        SynchedEntityData data = fakePlayer.getEntityData();
        data.set(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION, Byte.MAX_VALUE);
        spawnPackets.add(new ClientboundSetEntityDataPacket(fakePlayer.getId(), data.packDirty()));

        data.set(EntityDataSerializers.BYTE.createAccessor(0), (byte) 0x40);
        glowPackets.add(new ClientboundSetEntityDataPacket(fakePlayer.getId(), data.packDirty()));

        data.set(EntityDataSerializers.BYTE.createAccessor(0), (byte) 0);
        deglowPackets.add(new ClientboundSetEntityDataPacket(fakePlayer.getId(), data.packDirty()));

        destroyPackets.add(new ClientboundRemoveEntitiesPacket(fakePlayer.getId()));
        destroyPackets.add(new ClientboundPlayerInfoRemovePacket(List.of(fakePlayer.getUUID())));
    }

    public void spawn(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(spawnPackets));
    }

    public void destroy(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(destroyPackets));
    }

    public void glow(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(glowPackets));
    }

    public void deglow(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(deglowPackets));
    }

    // adjusted from https://github.com/ShaneBeee/NMS-API/blob/master/src/main/java/com/shanebeestudios/nms/api/util/McUtils.java#L361
    private static void setSkin(GameProfile gameProfile) {
        try {
            URL url = new URI("https://sessionserver.mojang.com/session/minecraft/profile/" + gameProfile.getId().toString() + "?unsigned=false").toURL();
            InputStreamReader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
            JsonObject mainObject = new Gson().fromJson(reader, JsonObject.class);
            if (mainObject == null) return;

            JsonObject properties = mainObject.get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String value = properties.get("value").getAsString();
            String signature = properties.get("signature").getAsString();
//            BodiesPlugin.log(value);
//            BodiesPlugin.log(signature);

            PropertyMap propertyMap = gameProfile.getProperties();
            propertyMap.put("name", new Property("name", gameProfile.getName()));
            propertyMap.put("textures", new Property("textures", value, signature));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
