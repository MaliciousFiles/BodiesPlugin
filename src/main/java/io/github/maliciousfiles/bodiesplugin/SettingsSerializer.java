package io.github.maliciousfiles.bodiesplugin;

import com.google.common.collect.ImmutableList;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SettingsSerializer {
    private static File configFile;

    private static final Map<UUID, PlayerSettings> playerSettings = new HashMap<>();

    public static PlayerSettings getSettings(UUID player) {
        playerSettings.putIfAbsent(player, new PlayerSettings());
        return playerSettings.get(player);
    }

    public static void trustPlayer(Player truster, Player trusted) {
        getSettings(truster.getUniqueId()).trusted.add(trusted.getUniqueId());
        serialize();
    }

    public static void untrustPlayer(Player truster, Player trusted) {
        getSettings(truster.getUniqueId()).trusted.remove(trusted.getUniqueId());
        serialize();
    }

    public static void setPrioritizeInv(Player player, boolean prioritizeInv) {
        getSettings(player.getUniqueId()).prioritizeInv = prioritizeInv;
        serialize();
    }

    private static void serialize() {
        FileConfiguration config = new YamlConfiguration();

        playerSettings.forEach((uuid, settings) -> config.set(uuid.toString(), settings));

        try {
            config.save(configFile);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void deserialize() {
        if (configFile == null) configFile = new File(BodiesPlugin.instance.getDataFolder(), "playerSettings.yml");
        if (!configFile.exists()) return;

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) { return; }

        config.getKeys(false).forEach(uuid -> playerSettings.put(UUID.fromString(uuid), (PlayerSettings) config.get(uuid)));
    }

    public static class PlayerSettings implements ConfigurationSerializable {
        private List<UUID> trusted = new ArrayList<>();
        private boolean prioritizeInv;

        private PlayerSettings() {}

        public ImmutableList<UUID> trusted() { return ImmutableList.copyOf(trusted); }
        public boolean prioritizeInv() { return prioritizeInv; }

        @Override
        public Map<String, Object> serialize() {
            return Map.of("trusted", trusted.stream().map(UUID::toString), "prioritizeInv", prioritizeInv);
        }

        public static PlayerSettings deserialize(Map<String, Object> map) {
            PlayerSettings settings = new PlayerSettings();
            settings.trusted = ((List<String>) map.get("trusted")).stream().map(UUID::fromString).toList();
            settings.prioritizeInv = (boolean) map.get("prioritizeInv");
            return settings;
        }
    }
}
