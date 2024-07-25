package io.github.maliciousfiles.gravestone;

import org.bukkit.plugin.java.JavaPlugin;

public final class Gravestone extends JavaPlugin {

    public static Gravestone instance;

    @Override
    public void onEnable() {
        instance = this;

        GraveSerializer.deserialize();

        getServer().getPluginManager().registerEvents(new GraveGenerator(), this);
    }

    @Override
    public void onDisable() {

    }
}
