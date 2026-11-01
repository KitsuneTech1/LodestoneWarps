package com.lodestonewarps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LodewarpsCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_ADMIN = "lodestonewarps.admin";

    private final LodestoneWarps plugin;

    public LodewarpsCommand(LodestoneWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§5§l━━━ LodestoneWarps ━━━");
        sender.sendMessage("§d/lodewarps list §7- List your portals");
        sender.sendMessage("§d/lodewarps info <name> §7- Get portal info");
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage("§d/lodewarps reload §7- Reload config");
        }
        sender.sendMessage("");
        sender.sendMessage("§7Build a lodestone frame, place a sign with a name,");
        sender.sendMessage("§7and build another portal with the same name to link!");
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("lodestonewarps.list")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        List<Portal> portals;
        String header;

        if (sender instanceof Player player && !sender.hasPermission(PERMISSION_ADMIN)) {
            portals = plugin.getPortalManager().getPortalsByPlayer(player.getUniqueId());
            header = "§5§lYour Portals";
        } else {
            portals = plugin.getPortalManager().getAllPortals();
            header = "§5§lAll Portals";
        }

        if (portals.isEmpty()) {
            sender.sendMessage("§7No portals found.");
            return;
        }

        sender.sendMessage(header + " §7(" + portals.size() + ")");

        for (Portal portal : portals) {
            boolean linked = plugin.getPortalManager().hasPartner(portal);
            String status = linked ? "§a✓" : "§c✗";
            String world = portal.getWorld().getName();
            String coords = String.format("§7(%d, %d, %d)",
                    portal.getSignLocation().getBlockX(),
                    portal.getSignLocation().getBlockY(),
                    portal.getSignLocation().getBlockZ());

            sender.sendMessage(String.format(" %s §d%s §8- §7%s %s",
                    status, portal.getName(), world, coords));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lodewarps info <name>");
            return;
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        List<Portal> portals = plugin.getPortalManager().getAllPortals().stream()
                .filter(p -> p.getName().equals(name))
                .collect(Collectors.toList());

        if (portals.isEmpty()) {
            sender.sendMessage("§cNo portal found with name: " + name);
            return;
        }

        sender.sendMessage("§5§l━━━ Portal: " + name + " ━━━");

        for (int i = 0; i < portals.size(); i++) {
            Portal portal = portals.get(i);
            sender.sendMessage(String.format("§d#%d §7by §f%s", i + 1, portal.getCreatorName()));
            sender.sendMessage(String.format("   §7World: §f%s", portal.getWorld().getName()));
            sender.sendMessage(String.format("   §7Location: §f%d, %d, %d",
                    portal.getSignLocation().getBlockX(),
                    portal.getSignLocation().getBlockY(),
                    portal.getSignLocation().getBlockZ()));
        }

        boolean linked = portals.size() >= 2;
        sender.sendMessage(linked ? "§aStatus: Linked" : "§cStatus: Waiting for partner");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage("§aLodestoneWarps config reloaded!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("list", "info"));
            if (sender.hasPermission(PERMISSION_ADMIN)) {
                options.add("reload");
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return plugin.getPortalManager().getAllPortals().stream()
                    .map(Portal::getName)
                    .distinct()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
