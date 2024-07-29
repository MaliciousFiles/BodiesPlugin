package io.github.maliciousfiles.bodiesplugin.serializing;

import io.github.maliciousfiles.bodiesplugin.BodiesPlugin;
import io.github.maliciousfiles.bodiesplugin.util.Body;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Interaction;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BodySerializer {
    private static File bodiesFile;
    private static File zombiesFile;

    private static final Map<UUID, List<BodyInfo>> playerMap = new HashMap<>();
    private static final Map<UUID, BodyInfo> interactionMap = new HashMap<>();

    private static final Map<UUID, BodyInfo> zombieMap = new HashMap<>();

    public static void addZombie(UUID zombie, BodyInfo body) {
        addZombie(zombie, body, true);
    }

    private static void addZombie(UUID zombie, BodyInfo body, boolean serialize) {
        zombieMap.put(zombie, body);
        if (serialize) serialize();
    }

    public static BodyInfo getZombieInfo(UUID zombie) {
        return zombieMap.get(zombie);
    }

    public static List<UUID> getAllZombies() {
        return List.copyOf(zombieMap.keySet());
    }

    public static void removeZombie(UUID zombie) {
        zombieMap.remove(zombie);
        serialize();
    }

    public static List<BodyInfo> getAllBodies() {
        return playerMap.values().stream().reduce(new ArrayList<>(), (a, b) -> { a.addAll(b); return a; });
    }

    public static List<BodyInfo> getBodiesForPlayer(OfflinePlayer player) {
        return playerMap.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    public static List<UUID> getPlayersWithBodies() {
        return List.copyOf(playerMap.keySet());
    }

    public static BodyInfo getBody(Interaction interaction) {
        return interactionMap.get(interaction.getUniqueId());
    }

    public static void addBody(BodyInfo info) {
        addBody(info, true);
    }

    private static void addBody(BodyInfo info, boolean serialize) {
        for (UUID interaction : info.interactions) interactionMap.put(interaction, info);

        playerMap.putIfAbsent(info.player, new ArrayList<>());
        playerMap.get(info.player).add(info);

        if (serialize) serialize();
    }

    public static void removeBody(BodyInfo body) {
        List<BodyInfo> bodies = playerMap.get(body.player);
        bodies.remove(body);
        if (bodies.isEmpty()) playerMap.remove(body.player);

        for (UUID interaction : body.interactions) interactionMap.remove(interaction);

        serialize();
    }

    private static void serialize() {
        FileConfiguration bodyConfig = new YamlConfiguration();
        playerMap.forEach((uuid, list) -> bodyConfig.set(uuid.toString(), list));

        FileConfiguration zombieConfig = new YamlConfiguration();
        zombieMap.forEach((uuid, body) -> zombieConfig.set(uuid.toString(), body));

        try {
            bodyConfig.save(bodiesFile);
            zombieConfig.save(zombiesFile);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void deserialize() {
        if (bodiesFile == null) bodiesFile = new File(BodiesPlugin.instance.getDataFolder(), "bodies.yml");
        if (bodiesFile.exists()) {
            FileConfiguration config = new YamlConfiguration();
            try {
                config.load(bodiesFile);
                for (String key : config.getKeys(false)) {
                    for (Object obj : Objects.requireNonNull(config.getList(key))) {
                        if (!(obj instanceof BodyInfo bi)) continue;

                        addBody(bi, false);
                    }
                }
            } catch (IOException | InvalidConfigurationException ignored) { }
        }

        if (zombiesFile == null) zombiesFile = new File(BodiesPlugin.instance.getDataFolder(), "zombies.yml");
        if (zombiesFile.exists()) {
            FileConfiguration config = new YamlConfiguration();
            try {
                config.load(zombiesFile);
                for (String key : config.getKeys(false)) {
                    if (!(config.get(key) instanceof BodyInfo bi)) continue;

                    UUID zombie = UUID.fromString(key);
                    bi.body.setReplacing(Bukkit.getEntity(zombie));
                    addZombie(zombie, bi, false);
                }
            } catch (IOException | InvalidConfigurationException ignored) { }
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
        public final boolean isZombie;


        private BodyInfo(UUID player, String message, Location loc, ItemStack[] items, int exp, UUID[] interactions, UUID textDisplay, long timestamp, boolean isZombie) {
            this(player, message, loc, items, exp, interactions, textDisplay, timestamp, new Body(loc, player), isZombie);
        }

        public BodyInfo(UUID player, String message, Location loc, ItemStack[] items, int exp, UUID[] interactions, UUID textDisplay, long timestamp, Body body, boolean isZombie) {
            this.player = player;
            this.message = message;
            this.loc = loc;
            this.items = items;
            this.exp = exp;
            this.interactions = interactions;
            this.textDisplay = textDisplay;
            this.timestamp = timestamp;
            this.body = body;
            this.isZombie = isZombie;
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
                    "timestamp", timestamp,
                    "isZombie", isZombie);
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
                    (long) map.get("timestamp"),
                    (boolean) map.get("isZombie"));
        }
    }
}
