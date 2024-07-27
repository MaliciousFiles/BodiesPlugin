package io.github.maliciousfiles.bodiesplugin.serializing;

import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import io.github.maliciousfiles.bodiesplugin.util.Body;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BodySerializer {
    private static File configFile;

    private static final Map<UUID, List<BodyInfo>> playerMap = new HashMap<>();
    private static final Map<UUID, BodyInfo> interactionMap = new HashMap<>();

    public static List<BodyInfo> getAllBodies() {
        return playerMap.values().stream().reduce(new ArrayList<>(), (a, b) -> { a.addAll(b); return a; });
    }

    public static List<BodyInfo> getBodiesForPlayer(OfflinePlayer player) {
        return playerMap.get(player.getUniqueId());
    }

    public static List<UUID> getPlayersWithBodies() {
        return List.copyOf(playerMap.keySet());
    }

    public static BodyInfo getBody(Interaction interaction) {
        return interactionMap.get(interaction.getUniqueId());
    }

    public static void addBody(BodyInfo info) {
        for (UUID interaction : info.interactions) interactionMap.put(interaction, info);

        playerMap.putIfAbsent(info.player, new ArrayList<>());
        playerMap.get(info.player).add(info);

        serialize();
    }

    public static void removeBody(BodyInfo body) {
        List<BodyInfo> bodies = playerMap.get(body.player);
        bodies.remove(body);
        if (bodies.isEmpty()) playerMap.remove(body.player);

        for (UUID interaction : body.interactions) interactionMap.remove(interaction);

        serialize();
    }

    private static void serialize() {
        FileConfiguration config = new YamlConfiguration();

        playerMap.forEach((uuid, list) -> config.set(uuid.toString(), list));

        try {
            config.save(configFile);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void deserialize() {
        if (configFile == null) configFile = new File(BodiesPlugin.instance.getDataFolder(), "bodies.yml");
        if (!configFile.exists()) return;

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) { return; }
        for (String key : config.getKeys(false)) {
            for (Object obj : Objects.requireNonNull(config.getList(key))) {
                if (!(obj instanceof BodyInfo bi)) return;

                addBody(bi);
            }
        }
    }

    public static class BodyInfo implements ConfigurationSerializable {
        public final UUID player;
        public final String message;
        public final Location loc;
        public final ItemStack[] items;
        public final int exp;
        public final UUID[] interactions;
        public final UUID textDisplay;
        public final long timestamp;
        public final Body body;


        public BodyInfo(UUID player, String message, Location loc, ItemStack[] items, int exp, UUID[] interactions, UUID textDisplay, long timestamp) {
            this(player, message, loc, items, exp, interactions, textDisplay, timestamp, new Body(loc, player));
        }

        public BodyInfo(UUID player, String message, Location loc, ItemStack[] items, int exp, UUID[] interactions, UUID textDisplay, long timestamp, Body body) {
            this.player = player;
            this.message = message;
            this.loc = loc;
            this.items = items;
            this.exp = exp;
            this.interactions = interactions;
            this.textDisplay = textDisplay;
            this.timestamp = timestamp;
            this.body = body;
        }

        @Override
        public Map<String, Object> serialize() {
            return Map.of(
                    "player", player.toString(),
                    "message", message,
                    "loc", loc,
                    "items", items,
                    "exp", exp,
                    "interactions", Arrays.stream(interactions).map(UUID::toString).toArray(),
                    "textDisplay", textDisplay.toString(),
                    "timestamp", timestamp);
        }

        public static BodyInfo deserialize(Map<String, Object> map) {
            return new BodyInfo(
                    UUID.fromString((String) map.get("player")),
                    (String) map.get("message"),
                    (Location) map.get("loc"),
                    ((List<ItemStack>) map.get("items")).toArray(ItemStack[]::new),
                    (int) map.get("exp"),
                    ((List<String>) map.get("interactions")).stream().map(UUID::fromString).toArray(UUID[]::new),
                    UUID.fromString((String) map.get("textDisplay")),
                    (long) map.get("timestamp"));
        }
    }
}
