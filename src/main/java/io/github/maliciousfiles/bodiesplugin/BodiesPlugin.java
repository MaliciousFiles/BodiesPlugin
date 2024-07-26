package io.github.maliciousfiles.bodiesplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class BodiesPlugin extends JavaPlugin {

    public static BodiesPlugin instance;

    @Override
    public void onEnable() {
        instance = this;


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
