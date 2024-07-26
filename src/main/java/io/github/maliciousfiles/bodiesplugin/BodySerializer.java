package io.github.maliciousfiles.bodiesplugin;

import io.github.maliciousfiles.bodiesplugin.util.Body;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BodySerializer {
    private static File configFile;

    private static Map<UUID, List<BodyInfo>> playerMap;
    private static Map<UUID, BodyInfo> interactionMap;
    private static Map<BodyInfo, Body> bodies;

    public static Body getBody(BodyInfo info) {
        if (!bodies.containsKey(info)) bodies.put(info, new Body(info.loc, info.player));
        return bodies.get(info);
    }

    public static List<BodyInfo> getAllBodies() {
        return playerMap.values().stream().reduce(new ArrayList<>(), (a, b) -> { a.addAll(b); return a; });
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

        for (String uuid : config.getKeys(false)) {
            List<BodyInfo> list = new ArrayList<>();
            for (Object obj : Objects.requireNonNull(config.getList(uuid))) {
                if (!(obj instanceof BodyInfo)) return;
                list.add((BodyInfo) obj);
            }

            playerMap.put(UUID.fromString(uuid), list);
        }
    }

    public record BodyInfo(UUID player, Location loc, UUID[] interactions, long timestamp) { }
}
