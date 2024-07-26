package io.github.maliciousfiles.bodiesplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.Base64;
import java.util.EnumSet;
import java.util.UUID;

public class Body {
    //private Marker marker;
    public Location loc;

    public void spawn(Player player) {
        ServerPlayer sp = ((CraftPlayer) player).getHandle();

        ServerPlayer fakePlayer = new ServerPlayer(sp.getServer(),
                sp.serverLevel(), new GameProfile(
//                        UUID.fromString("4f393528-e106-4139-b67c-4d64f3a620d3"), "Brainpower5"),
                        UUID.randomUUID(), sp.gameProfile.getName()),
                ClientInformation.createDefault());

        setSkin(sp.getUUID(), fakePlayer.gameProfile);
        fakePlayer.setPos(CraftLocation.toVec3D(/*marker.getLocation()*/loc));
        fakePlayer.setRot(loc.getYaw(), loc.getPitch());
        fakePlayer.setPose(Pose.SLEEPING);

        sp.connection.send(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                new ClientboundPlayerInfoUpdatePacket.Entry(
                        fakePlayer.getUUID(), fakePlayer.getGameProfile(), false, 0,
                        GameType.CREATIVE, fakePlayer.getDisplayName(), null)));

        sp.connection.send(fakePlayer.getAddEntityPacket(sp.tracker.serverEntity));

        SynchedEntityData data = fakePlayer.getEntityData();
        sp.connection.send(new ClientboundSetEntityDataPacket(fakePlayer.getId(), data.getNonDefaultValues()));
    }

    // adjusted from https://github.com/ShaneBeee/NMS-API/blob/master/src/main/java/com/shanebeestudios/nms/api/util/McUtils.java#L361
    public static void setSkin(UUID uuid, GameProfile gameProfile) {
        try {
            URL url = new URI("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString() + "?unsigned=false").toURL();
            InputStreamReader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
            JsonObject mainObject = new Gson().fromJson(reader, JsonObject.class);
            if (mainObject == null) return;

            JsonObject properties = mainObject.get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String value = properties.get("value").getAsString();
            String signature = properties.get("signature").getAsString();
            BodiesPlugin.log(value);
            BodiesPlugin.log(signature);

            PropertyMap propertyMap = gameProfile.getProperties();
            propertyMap.put("name", new Property("name", gameProfile.getName()));
            propertyMap.put("textures", new Property("textures", value, signature));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
