package com.lodestonewarps;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Portal {

    private final String name;
    private final UUID creatorUUID;
    private final String creatorName;
    private final Location signLocation;
    private final Set<Location> frameBlocks;
    private final Set<Location> interiorBlocks;
    private final Location teleportDestination;
    private final long createdTime;

    public Portal(String name, UUID creatorUUID, String creatorName, Location signLocation,
            Set<Location> frameBlocks, Set<Location> interiorBlocks) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.creatorUUID = creatorUUID;
        this.creatorName = creatorName;
        this.signLocation = signLocation;
        this.frameBlocks = new HashSet<>(frameBlocks);
        this.interiorBlocks = new HashSet<>(interiorBlocks);
        this.createdTime = System.currentTimeMillis();

        // Calculate teleport destination (center of the portal, at the bottom interior
        // block)
        this.teleportDestination = calculateTeleportDestination();
    }

    private Location calculateTeleportDestination() {
        if (interiorBlocks.isEmpty()) {
            return signLocation.clone();
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        World world = null;

        for (Location loc : interiorBlocks) {
            world = loc.getWorld();
            minX = Math.min(minX, loc.getBlockX());
            minY = Math.min(minY, loc.getBlockY());
            minZ = Math.min(minZ, loc.getBlockZ());
            maxX = Math.max(maxX, loc.getBlockX());
            maxZ = Math.max(maxZ, loc.getBlockZ());
        }

        // Center of portal interior, at the lowest Y level
        // Always land in the middle of a block (+0.5)
        double centerX = (minX + maxX) / 2 + 0.5;
        double centerZ = (minZ + maxZ) / 2 + 0.5;

        return new Location(world, centerX, minY, centerZ);
    }

    public String getName() {
        return name;
    }

    public UUID getCreatorUUID() {
        return creatorUUID;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public Location getSignLocation() {
        return signLocation;
    }

    public Set<Location> getFrameBlocks() {
        return new HashSet<>(frameBlocks);
    }

    public Set<Location> getInteriorBlocks() {
        return new HashSet<>(interiorBlocks);
    }

    public Location getTeleportDestination() {
        return teleportDestination;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public World getWorld() {
        return signLocation.getWorld();
    }

    public boolean containsInteriorBlock(Location location) {
        return containsBlock(interiorBlocks, location);
    }

    public boolean containsFrameBlock(Location location) {
        return containsBlock(frameBlocks, location);
    }

    private boolean containsBlock(Set<Location> blocks, Location location) {
        for (Location block : blocks) {
            if (block.getBlockX() == location.getBlockX() &&
                    block.getBlockY() == location.getBlockY() &&
                    block.getBlockZ() == location.getBlockZ() &&
                    block.getWorld().equals(location.getWorld())) {
                return true;
            }
        }
        return false;
    }

    public boolean isSignLocation(Location location) {
        return signLocation.getBlockX() == location.getBlockX() &&
                signLocation.getBlockY() == location.getBlockY() &&
                signLocation.getBlockZ() == location.getBlockZ() &&
                signLocation.getWorld().equals(location.getWorld());
    }

    /**
     * Generate a unique color based on the portal name for particles
     */
    public int getColor() {
        int hash = name.hashCode();
        // Generate vibrant colors
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;

        // Ensure the color is vibrant (not too dark)
        r = Math.max(r, 80);
        g = Math.max(g, 80);
        b = Math.max(b, 80);

        return (r << 16) | (g << 8) | b;
    }

    @Override
    public String toString() {
        return "Portal{name='" + name + "', creator='" + creatorName +
                "', world='" + getWorld().getName() + "'}";
    }
}
