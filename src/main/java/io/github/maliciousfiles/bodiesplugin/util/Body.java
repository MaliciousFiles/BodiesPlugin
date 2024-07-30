package io.github.maliciousfiles.bodiesplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import io.github.maliciousfiles.bodiesplugin.serializing.SettingsSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

    private List<Packet<? super ClientGamePacketListener>> replacePackets = new ArrayList<>();
    private List<Packet<? super ClientGamePacketListener>> zombieGlow = new ArrayList<>();
    private List<Packet<? super ClientGamePacketListener>> zombieDeglow = new ArrayList<>();
    private UUID skin;

    public Body(Location loc, UUID dead, ItemStack[] items, int selectedItem) {
        this.skin = dead;

        ServerLevel level = ((CraftWorld) loc.getWorld()).getHandle();

        ServerPlayer fakePlayer = new ServerPlayer(level.getServer(),
                level, new GameProfile(dead, ""),
                ClientInformation.createDefault());

        fakePlayer.setUUID(UUID.randomUUID());
        setSkin(fakePlayer.gameProfile);
        fakePlayer.setPos(CraftLocation.toVec3D(loc));
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

        spawnPackets.add(new ClientboundSetEquipmentPacket(fakePlayer.getId(), List.of(
                Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(items[39])),
                Pair.of(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(items[38])),
                Pair.of(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(items[37])),
                Pair.of(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(items[36])),
                Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(items[selectedItem])),
                Pair.of(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(items[40]))
        )));

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

    public void setReplacing(Entity entity) {
        ServerLevel level = ((CraftWorld) entity.getWorld()).getHandle();

        ServerPlayer fakePlayer = new ServerPlayer(level.getServer(),
                level, new GameProfile(skin, Bukkit.getOfflinePlayer(skin).getName()),
                ClientInformation.createDefault());

        fakePlayer.setId(entity.getEntityId());
        fakePlayer.setUUID(entity.getUniqueId());
        setSkin(fakePlayer.gameProfile);
        fakePlayer.setPos(CraftLocation.toVec3D(entity.getLocation()));
        fakePlayer.setRot(entity.getYaw(), entity.getPitch());
        fakePlayer.setYHeadRot(entity.getYaw());

        replacePackets = new ArrayList<>();
        zombieGlow = new ArrayList<>();
        zombieDeglow = new ArrayList<>();

        replacePackets.add(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                new ClientboundPlayerInfoUpdatePacket.Entry(
                        fakePlayer.getUUID(), fakePlayer.getGameProfile(), false, 0,
                        GameType.CREATIVE, fakePlayer.getDisplayName(), null)));

        replacePackets.add(new ClientboundAddEntityPacket(
                fakePlayer.getId(),
                fakePlayer.getUUID(),
                fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                fakePlayer.getXRot(), fakePlayer.getYRot(),
                fakePlayer.getType(), 0,
                fakePlayer.getDeltaMovement(), fakePlayer.getYHeadRot()
        ));

        SynchedEntityData data = fakePlayer.getEntityData();
        data.set(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION, Byte.MAX_VALUE);
        replacePackets.add(new ClientboundSetEntityDataPacket(fakePlayer.getId(), data.packDirty()));

        data.set(EntityDataSerializers.BYTE.createAccessor(0), (byte) 0x40);
        zombieGlow.add(new ClientboundSetEntityDataPacket(fakePlayer.getId(), data.packDirty()));

        data.set(EntityDataSerializers.BYTE.createAccessor(0), (byte) 0);
        zombieDeglow.add(new ClientboundSetEntityDataPacket(fakePlayer.getId(), data.packDirty()));

    }

    public ClientboundBundlePacket getReplacePacket(Player player) {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        packets.addAll(replacePackets);
        if (glowing.contains(player.getUniqueId())) packets.addAll(zombieGlow);

        return new ClientboundBundlePacket(packets);
    }

    public void replace(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(getReplacePacket(player));
        if (glowing.contains(player.getUniqueId())) glow(player);
    }

    private final List<UUID> withinRadius = new ArrayList<>();
    private final List<UUID> glowing = new ArrayList<>();

    private boolean glowable(Player player) {
        return player.getUniqueId().equals(skin) || SettingsSerializer.getSettings(skin).trusted().contains(player.getUniqueId());
    }

    public void setWithinRadius(Player player, boolean within) {
        if (within != withinRadius.contains(player.getUniqueId())) {
            if (within) withinRadius.add(player.getUniqueId());
            else withinRadius.remove(player.getUniqueId());
        }

        boolean shouldGlow = glowable(player) && within;
        if (shouldGlow != glowing.contains(player.getUniqueId())) {
            if (shouldGlow) {
                glowing.add(player.getUniqueId());
                glow(player);
            } else {
                glowing.remove(player.getUniqueId());
                deglow(player);
            }
        }
    }

    public boolean noneWithinRadius() {
        return withinRadius.stream().noneMatch(p -> Bukkit.getPlayer(p) != null);
    }

    public void spawn(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(spawnPackets));
        if (glowing.contains(player.getUniqueId())) glow(player);
    }

    public void destroy(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(destroyPackets));
    }

    public void glow(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(
                zombieGlow.isEmpty() ? glowPackets : zombieGlow));
    }

    public void deglow(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundBundlePacket(
                zombieDeglow.isEmpty() ? deglowPackets : zombieDeglow));
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
