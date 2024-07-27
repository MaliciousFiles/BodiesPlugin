package io.github.maliciousfiles.bodiesplugin.commands;

import io.github.maliciousfiles.bodiesplugin.listeners.BodyHandler;
import io.github.maliciousfiles.bodiesplugin.serializing.BodySerializer;
import io.github.maliciousfiles.bodiesplugin.serializing.SettingsSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.entity.ExperienceOrb;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class BodiesCommand implements CommandExecutor, TabCompleter {
    private void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message+" (`/bodies help` for help)").color(NamedTextColor.RED));
    }

    private void success(CommandSender sender, String message, Object... args) {
        Component component = Component.empty();

        String[] sections = message.split("%s");
        for (int i = 0; i < args.length+1; i++) {
            String section = i >= sections.length ? "" : sections[i];

            component = component.append(Component.text(section).color(NamedTextColor.GOLD));

            if (i != args.length) component = component.append(Component.text(args[i].toString()).color(NamedTextColor.DARK_RED));
        }

        sender.sendMessage(component);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String name, @NotNull String[] args) {
        if (!(sender instanceof Player player)) error(sender, "Only players can use this command");
        else if (args.length == 0) error(sender, "Invalid subcommand ' '");
        else if (args[0].equalsIgnoreCase("help")) help(player);
        else if (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) trust(player, args);
        else if (args[0].equalsIgnoreCase("priority")) priority(player, args);
        else if (args[0].equalsIgnoreCase("list")) list(player, args);
        else if (!player.isOp()) error(sender, "You do not have permission to use this command");
        else if (args[0].equalsIgnoreCase("info")) info(player, args);
        else if (args[0].equalsIgnoreCase("claim")) claim(player, args);
        else error(sender, "Invalid subcommand '%s'".formatted(args[0]));

        return true;
    }

    private void help(Player sender) {
        success(sender, "Bodies Help:");
        success(sender, "  /%s %s - Show this help message", "bodies", "help");
        success(sender, "  /%s %s <%s> - Trust a player to access your body", "bodies", "trust", "player");
        success(sender, "  /%s %s <%s> - Untrust a player from accessing your body", "bodies", "untrust", "player");
        success(sender, "  /%s %s <%s/%s> - Set whether to prioritize your body or player inventory", "bodies", "priority", "body", "player");

        if (sender.isOp()) {
            success(sender, "  /bodies %s [%s] - List all bodies for a player", "bodies", "list", "player");
            success(sender, "  /bodies %s <%s> <%s> - Get information about a player's body", "bodies", "info", "player", "id");
            success(sender, "  /bodies %s <%s> <%s> [%s] - Remotely claim a player's body", "bodies", "claim", "player", "id", "recipient");
        } else {
            success(sender, "  /bodies %s - List all your bodies", "bodies", "list");
            success(sender, "  /bodies %s <%s> - Get information about a body", "bodies", "info", "id");
        }
    }

    private void trust(Player sender, String[] args) {
        if (args.length < 2) {
            error(sender, "Invalid player ' '");
            return;
        }

        OfflinePlayer trusted = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (trusted == null) {
            error(sender, "Invalid player '%s'".formatted(args[1]));
            return;
        }

        if (args[0].equalsIgnoreCase("trust")) {
            if (SettingsSerializer.getSettings(sender.getUniqueId()).trusted().contains(trusted.getUniqueId())) {
                error(sender, "Player '%s' is already trusted".formatted(args[1]));
                return;
            }

            success(sender, "Player %s is now trusted", trusted.getName());
            SettingsSerializer.trustPlayer(sender, trusted);
        } else {
            if (!SettingsSerializer.getSettings(sender.getUniqueId()).trusted().contains(trusted.getUniqueId())) {
                error(sender, "Player '%s' is not trusted".formatted(args[1]));
                return;
            }

            success(sender, "Player %s is no longer trusted", trusted.getName());
            SettingsSerializer.untrustPlayer(sender, trusted);
        }
    }

    private void priority(Player sender, String[] args) {
        if (args.length < 2) {
            error(sender, "Invalid priority ' '");
            return;
        }

        if (args[1].equalsIgnoreCase("body")) {
            success(sender, "Now prioritizing %s inventory contents", "body");
            SettingsSerializer.setPrioritizeInv(sender, false);
        }  else if (args[1].equalsIgnoreCase("player")) {
            success(sender, "Now prioritizing %s inventory contents", "player");
            SettingsSerializer.setPrioritizeInv(sender, true);
        } else error(sender, "Invalid priority '%s'".formatted(args[1]));
    }

    private void list(Player sender, String[] args) {
        if (args.length > 1 && !sender.isOp()) {
            error(sender, "You do not have permission to view other players' bodies");
            return;
        }

        OfflinePlayer op = args.length < 2 ? sender : Bukkit.getOfflinePlayerIfCached(args[1]);
        if (op == null) {
            error(sender, "Invalid player '%s'".formatted(args[1]));
            return;
        }
        List<BodySerializer.BodyInfo> bodies = BodySerializer.getBodiesForPlayer(op);

        success(sender, "Bodies for %s: (%s <%s> for more info)", op.getName(), "/bodies info", "id");
        for (int i = 0; i < bodies.size(); i++) {
            BodySerializer.BodyInfo body = bodies.get(i);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(body.timestamp);

            success(sender, "  (%s) %s:%s %s-%s-%s", i, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.YEAR));
        }
    }

    private void info(Player sender, String[] args) {
        if (args.length < 2) {
            error(sender, sender.isOp() ? "Invalid player ' '" : "Invalid body ' '");
            return;
        }

        OfflinePlayer op = sender.isOp() ? Bukkit.getOfflinePlayerIfCached(args[1]) : sender;
        if (op == null) {
            error(sender, "Invalid player '%s'".formatted(args[1]));
            return;
        }

        List<BodySerializer.BodyInfo> bodies = BodySerializer.getBodiesForPlayer(op);
        if (sender.isOp() && args.length < 3) {
            error(sender, "Invalid body ' '");
            return;
        }

        int indexIdx = sender.isOp() ? 2 : 1;
        int index;
        try {
            index = Integer.parseInt(args[indexIdx]);
        } catch (NumberFormatException e) {
            error(sender, "Invalid body '%s'".formatted(args[indexIdx]));
            return;
        }

        if (bodies == null || index >= bodies.size() || index < 0) {
            error(sender, "Invalid body '%s'".formatted(args[indexIdx]));
            return;
        }

        BodySerializer.BodyInfo body = bodies.get(index);
        success(sender, "Body %s for %s:", index, op.getName());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(body.timestamp);
        success(sender, "  Time: %s:%s:%s %s-%s-%s", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.YEAR));

        success(sender, "  Message: %s", body.message);
        success(sender, "  Experience: %s", body.exp);
        success(sender, "  Location: (%s,%s,%s,%s)", body.loc.getBlockX(), body.loc.getBlockY(), body.loc.getBlockZ(), body.loc.getWorld().getName());

        StringBuilder str = new StringBuilder("  Items: [");
        for (ItemStack item : body.items) {
            if (item == null) continue;

            str.append("%s, ");
        }
        success(sender, str.substring(0, str.length()-2) + "]", Arrays.stream(body.items).filter(Objects::nonNull).map(ItemStack::getType).toArray());
    }

    private void claim(Player sender, String[] args) {
        if (args.length < 2) {
            error(sender, "Invalid player ' '");
            return;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (op == null) {
            error(sender, "Invalid player '%s'".formatted(args[1]));
            return;
        }

        List<BodySerializer.BodyInfo> bodies = BodySerializer.getBodiesForPlayer(op);
        if (args.length < 3) {
            error(sender, "Invalid body ' '");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            error(sender, "Invalid body '%s'".formatted(args[2]));
            return;
        }

        if (bodies == null || index >= bodies.size() || index < 0) {
            error(sender, "Invalid body '%s'".formatted(args[2]));
            return;
        }

        Player recipient = args.length < 4 ? sender : Bukkit.getPlayer(args[3]);
        if (recipient == null) {
            error(sender, "Invalid recipient '%s'".formatted(args[3]));
            return;
        }

        success(sender, "Claimed body %s for %s", index, op.getName());

        BodySerializer.BodyInfo body = bodies.get(index);
        BodyHandler.claimBody(recipient, body);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String name, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        if (!(sender instanceof Player player)) return options;

        if (args.length == 1) {
            options = new ArrayList<>(List.of("trust", "untrust", "priority", "list", "help"));
            if (player.isOp()) options.addAll(List.of("claim", "info"));
        } else if (args.length == 2) {
            switch (args[0]) {
                case "trust" -> {
                    List<UUID> trusted = SettingsSerializer.getSettings(player.getUniqueId()).trusted();
                    options = Bukkit.getOnlinePlayers().stream().filter(p -> p != sender && !trusted.contains(p.getUniqueId())).map(Player::getName).toList();
                }
                case "untrust" -> options = SettingsSerializer.getSettings(player.getUniqueId()).trusted().stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).toList();
                case "priority" -> options = List.of("body", "player");
                case "list" -> {
                    if (player.isOp()) options = BodySerializer.getPlayersWithBodies().stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).toList();
                    else for (int i = 0; i < BodySerializer.getBodiesForPlayer(player).size(); i++) options.add(String.valueOf(i));
                }
                case "claim", "info" -> { if (player.isOp()) options = BodySerializer.getPlayersWithBodies().stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).toList(); }
            }
        } else if (args.length == 3) {
            if (player.isOp() && (args[0].equalsIgnoreCase("claim") || args[0].equalsIgnoreCase("info"))) {
                OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (op == null) return options;

                for (int i = 0; i < BodySerializer.getBodiesForPlayer(op).size(); i++) options.add(String.valueOf(i));
            }
        } else if (args.length == 4) {
            if (player.isOp() && args[0].equalsIgnoreCase("claim")) {
                options = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }

        return options.stream().filter(st -> st.startsWith(args[args.length-1])).sorted().toList();
    }
}
