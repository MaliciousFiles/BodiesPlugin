package io.github.maliciousfiles.bodiesplugin;

import io.github.maliciousfiles.bodiesplugin.listeners.BodyGenerator;
import io.github.maliciousfiles.bodiesplugin.listeners.BodyHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class BodiesPlugin extends JavaPlugin {
    public static BodiesPlugin instance;

    public static void log(String message) {
//        instance.getLogger().log(Level.INFO, message);
        Bukkit.broadcast(Component.text(message));
    }

    @Override
    public void onEnable() {
        instance = this;

        ConfigurationSerialization.registerClass(BodySerializer.BodyInfo.class);
        ConfigurationSerialization.registerClass(SettingsSerializer.PlayerSettings.class);

        SettingsSerializer.deserialize();
        BodySerializer.deserialize();

        getServer().getPluginManager().registerEvents(new BodyGenerator(), this);
        getServer().getPluginManager().registerEvents(new BodyHandler(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
