package io.github.maliciousfiles.gravestone;

import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GraveSerializer {
    private static File configFile;

    private static Map<Location, Grave> locMap = new HashMap<>();
    private static Map<UUID, List<Grave>> playerMap = new HashMap<>();

    public static Grave getGrave(Location loc) {
        return locMap.get(loc);
    }

    public static void addGrave(Grave grave) {
        locMap.put(grave.location, grave);

        playerMap.putIfAbsent(grave.player, new ArrayList<>());
        playerMap.get(grave.player).add(grave);

        serialize();
    }

    public static void removeGrave(Location loc) {
        Grave grave = locMap.remove(loc);
        if (grave == null) return;

        playerMap.get(grave.player).remove(grave);
        serialize();
    }

    private static void serialize() {
        FileConfiguration graveConfig = new YamlConfiguration();

        playerMap.forEach((uuid, graves) -> graveConfig.set(uuid.toString(), graves));

        try {
            graveConfig.save(configFile);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void deserialize() {
        configFile = new File(GravestonePlugin.instance.getDataFolder(), "graves.yml");
        if (!configFile.exists()) {
            if (!configFile.getParentFile().mkdirs()) throw new RuntimeException();
            GravestonePlugin.instance.saveResource("graves.yml", false);
        }

        FileConfiguration graveConfig = new YamlConfiguration();
        try {
            graveConfig.load(configFile);
        } catch (IOException | InvalidConfigurationException e) { throw new RuntimeException(e); }

        for (String uuid : graveConfig.getKeys(false)) {
            List<Grave> graves = (List<Grave>) graveConfig.getList(uuid);

            playerMap.put(UUID.fromString(uuid), graves);
            for (Grave grave : graves) {
                locMap.put(grave.location, grave);
            }
        }
    }
}
