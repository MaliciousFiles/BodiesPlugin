package io.github.maliciousfiles.bodiesplugin;

import io.github.maliciousfiles.bodiesplugin.commands.BodiesCommand;
import io.github.maliciousfiles.bodiesplugin.listeners.BodyGenerator;
import io.github.maliciousfiles.bodiesplugin.listeners.BodyHandler;
import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.serializing.SettingsSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public final class BodiesPlugin extends JavaPlugin {
    public static BodiesPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        ConfigurationSerialization.registerClass(BodySerializer.BodyInfo.class);
        ConfigurationSerialization.registerClass(SettingsSerializer.PlayerSettings.class);

        SettingsSerializer.deserialize();
        BodySerializer.deserialize();

        getServer().getPluginManager().registerEvents(new BodyGenerator(), this);
        getServer().getPluginManager().registerEvents(new BodyHandler(), this);

        getCommand("bodies").setExecutor(new BodiesCommand());
        getCommand("bodies").setTabCompleter(new BodiesCommand());

        Bukkit.getOnlinePlayers().forEach(p -> {
            BodySerializer.getAllBodies().forEach(b -> b.body.spawn(p));
            BodyGenerator.replaceConnection(p);
            BodyHandler.helpNewPlayer(p);
        });

        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        BodySerializer.getAllBodies().forEach(b -> Bukkit.getOnlinePlayers().forEach(b.body::destroy));
    }
}
