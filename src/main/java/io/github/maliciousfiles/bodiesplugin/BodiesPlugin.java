package io.github.maliciousfiles.bodiesplugin;

import io.github.maliciousfiles.bodiesplugin.listeners.BodyGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class BodiesPlugin extends JavaPlugin {
    public static BodiesPlugin instance;

    public static void log(String message) {
//        instance.getLogger().log(Level.INFO, message);
        Bukkit.broadcast(Component.text(message));
    }

    @Override
    public void onEnable() {
        instance = this;

        getServer().getPluginManager().registerEvents(new BodyGenerator(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
