package com.lodestonewarps;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class PortalManager {

    private final LodestoneWarps plugin;
    private final Map<String, List<Portal>> portalsByName;
    private final Map<Location, Portal> portalsByInterior;
    private final Map<Location, Portal> portalsByFrame;
    private final Map<Location, Portal> portalsBySign;
    private final File dataFile;

    public PortalManager(LodestoneWarps plugin) {
        this.plugin = plugin;
        this.portalsByName = new ConcurrentHashMap<>();
        this.portalsByInterior = new ConcurrentHashMap<>();
        this.portalsByFrame = new ConcurrentHashMap<>();
        this.portalsBySign = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "portals.yml");
    }

    public void registerPortal(Portal portal) {
        String name = portal.getName();

        // Use CopyOnWriteArrayList for thread-safe iteration
        portalsByName.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(portal);

        for (Location loc : portal.getInteriorBlocks()) {
            portalsByInterior.put(normalizeLocation(loc), portal);
        }

        for (Location loc : portal.getFrameBlocks()) {
            portalsByFrame.put(normalizeLocation(loc), portal);
        }

        portalsBySign.put(normalizeLocation(portal.getSignLocation()), portal);

        savePortals();
    }

    public void removePortal(Portal portal) {
        String name = portal.getName();

        List<Portal> portals = portalsByName.get(name);
        if (portals != null) {
            portals.remove(portal);
            if (portals.isEmpty()) {
                portalsByName.remove(name);
            }
        }

        for (Location loc : portal.getInteriorBlocks()) {
            portalsByInterior.remove(normalizeLocation(loc));
        }

        for (Location loc : portal.getFrameBlocks()) {
            portalsByFrame.remove(normalizeLocation(loc));
        }

        portalsBySign.remove(normalizeLocation(portal.getSignLocation()));

        savePortals();
    }

    public Portal getPortalAtInterior(Location location) {
        return portalsByInterior.get(normalizeLocation(location));
    }

    public Portal getPortalAtFrame(Location location) {
        return portalsByFrame.get(normalizeLocation(location));
    }

    public Portal getPortalAtSign(Location location) {
        return portalsBySign.get(normalizeLocation(location));
    }

    public Portal getLinkedPortal(Portal portal) {
        List<Portal> portals = portalsByName.get(portal.getName());
        if (portals == null || portals.size() < 2) {
            return null;
        }

        for (Portal p : portals) {
            if (p != portal) {
                return p;
            }
        }
        return null;
    }

    public boolean isNameAvailable(String name) {
        List<Portal> portals = portalsByName.get(name.toLowerCase());
        return portals == null || portals.size() < 2;
    }

    public boolean hasPartner(Portal portal) {
        return getLinkedPortal(portal) != null;
    }

    public List<Portal> getPortalsByPlayer(UUID playerUUID) {
        return portalsByName.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.getCreatorUUID().equals(playerUUID))
                .collect(Collectors.toList());
    }

    public List<Portal> getAllPortals() {
        return portalsByName.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public int getPortalCount() {
        return (int) portalsByName.values().stream().mapToLong(List::size).sum();
    }

    private Location normalizeLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void savePortals() {
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (List<Portal> portals : portalsByName.values()) {
            for (Portal portal : portals) {
                String path = "portals." + index;
                config.set(path + ".name", portal.getName());
                config.set(path + ".creator-uuid", portal.getCreatorUUID().toString());
                config.set(path + ".creator-name", portal.getCreatorName());
                config.set(path + ".sign-location", serializeLocation(portal.getSignLocation()));
                config.set(path + ".frame-blocks", portal.getFrameBlocks().stream()
                        .map(this::serializeLocation)
                        .collect(Collectors.toList()));
                config.set(path + ".interior-blocks", portal.getInteriorBlocks().stream()
                        .map(this::serializeLocation)
                        .collect(Collectors.toList()));
                config.set(path + ".created-time", portal.getCreatedTime());
                index++;
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save portals: " + e.getMessage());
        }
    }

    public void loadPortals() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection portalsSection = config.getConfigurationSection("portals");

        if (portalsSection == null) {
            return;
        }

        int loaded = 0;
        for (String key : portalsSection.getKeys(false)) {
            try {
                String path = "portals." + key;
                String name = config.getString(path + ".name");
                UUID creatorUUID = UUID.fromString(config.getString(path + ".creator-uuid"));
                String creatorName = config.getString(path + ".creator-name");
                Location signLocation = deserializeLocation(config.getString(path + ".sign-location"));

                Set<Location> frameBlocks = new HashSet<>();
                for (String locStr : config.getStringList(path + ".frame-blocks")) {
                    Location loc = deserializeLocation(locStr);
                    if (loc != null) {
                        frameBlocks.add(loc);
                    }
                }

                Set<Location> interiorBlocks = new HashSet<>();
                for (String locStr : config.getStringList(path + ".interior-blocks")) {
                    Location loc = deserializeLocation(locStr);
                    if (loc != null) {
                        interiorBlocks.add(loc);
                    }
                }

                if (signLocation != null && !frameBlocks.isEmpty() && !interiorBlocks.isEmpty()) {
                    Portal portal = new Portal(name, creatorUUID, creatorName, signLocation, frameBlocks,
                            interiorBlocks);

                    // Register without saving (we're loading)
                    portalsByName.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(portal);
                    for (Location loc : portal.getInteriorBlocks()) {
                        portalsByInterior.put(normalizeLocation(loc), portal);
                    }
                    for (Location loc : portal.getFrameBlocks()) {
                        portalsByFrame.put(normalizeLocation(loc), portal);
                    }
                    portalsBySign.put(normalizeLocation(portal.getSignLocation()), portal);

                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load portal " + key + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " portals.");
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLocation(String str) {
        if (str == null)
            return null;
        String[] parts = str.split(",");
        if (parts.length != 4)
            return null;

        try {
            var world = plugin.getServer().getWorld(parts[0]);
            if (world == null)
                return null;
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }
}
