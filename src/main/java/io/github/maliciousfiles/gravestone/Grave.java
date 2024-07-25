package io.github.maliciousfiles.gravestone;

import com.google.common.collect.ImmutableList;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Grave implements ConfigurationSerializable {
    public Grave(Location loc, UUID player, String message, List<ItemStack> contents) {
        this.location = loc;
        this.player = player;
        this.message = message;
        this.contents = ImmutableList.copyOf(contents);
    }

    public final Location location;
    public final UUID player;
    public final String message;
    public final ImmutableList<ItemStack> contents;

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of("loc", location, "uuid", player,
                "msg", message, "items", contents);
    }

    public static @NotNull Grave deserialize(Map<String, Object> map) {
        return new Grave((Location) map.get("loc"),
                (UUID) map.get("uuid"), (String) map.get("msg"),
                (List<ItemStack>) map.get("items"));
    }
}
