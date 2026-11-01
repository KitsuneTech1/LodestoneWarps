package com.lodestonewarps;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalListener implements Listener {

    private final LodestoneWarps plugin;
    private final Map<UUID, Long> cooldowns;
    private final Map<UUID, Location> lastPortalLocation;

    public PortalListener(LodestoneWarps plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
        this.lastPortalLocation = new HashMap<>();

        if (plugin.getConfig().getBoolean("particles-enabled", true)) {
            startParticleTask();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check on block changes for performance
        if (!isBlockChange(event.getFrom(), event.getTo())) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        Portal portal = plugin.getPortalManager().getPortalAtInterior(to);
        if (portal == null) {
            lastPortalLocation.remove(player.getUniqueId());
            return;
        }

        // Don't teleport if they're still in the same portal (prevents loops)
        Location lastPortal = lastPortalLocation.get(player.getUniqueId());
        if (lastPortal != null && portal.containsInteriorBlock(lastPortal)) {
            return;
        }

        if (!player.hasPermission("lodestonewarps.use")) {
            return;
        }

        Portal destination = plugin.getPortalManager().getLinkedPortal(portal);
        if (destination == null) {
            return; // No linked portal
        }

        if (!plugin.getConfig().getBoolean("allow-cross-dimension", true)) {
            if (!portal.getWorld().equals(destination.getWorld())) {
                return;
            }
        }

        // Check cooldown
        int cooldownSeconds = plugin.getConfig().getInt("teleport-cooldown", 3);
        if (cooldownSeconds > 0) {
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null) {
                long remaining = (lastUse + (cooldownSeconds * 1000L)) - System.currentTimeMillis();
                if (remaining > 0) {
                    player.sendMessage(plugin.getMessage("cooldown", "%time%", String.valueOf((remaining / 1000) + 1)));
                    return;
                }
            }
        }

        recordSuccessfulTeleport(teleportPlayer(player, portal, destination), player.getUniqueId(),
                destination.getTeleportDestination(), cooldowns, lastPortalLocation, System.currentTimeMillis());
    }

    static boolean isBlockChange(Location from, Location to) {
        return from != null && to != null &&
                (from.getBlockX() != to.getBlockX() ||
                        from.getBlockY() != to.getBlockY() ||
                        from.getBlockZ() != to.getBlockZ());
    }

    static boolean recordSuccessfulTeleport(boolean teleported, UUID playerId, Location destination,
            Map<UUID, Long> cooldowns, Map<UUID, Location> lastPortalLocation, long timestamp) {
        if (!teleported || destination == null) {
            return false;
        }
        cooldowns.put(playerId, timestamp);
        lastPortalLocation.put(playerId, destination.clone());
        return true;
    }

    private boolean teleportPlayer(Player player, Portal from, Portal to) {
        Location destination = to.getTeleportDestination().clone();

        // Preserve player's yaw and pitch
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());

        // Play departure effects
        if (plugin.getConfig().getBoolean("sounds-enabled", true)) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);
        }

        if (plugin.getConfig().getBoolean("particles-enabled", true)) {
            spawnTeleportParticles(player.getLocation(), from.getColor());
        }

        // Teleport
        if (!player.teleport(destination, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            return false;
        }

        // Debug logging
        if (plugin.getConfig().getBoolean("debug-enabled", false)) {
            plugin.getLogger().info(String.format("[DEBUG] Teleported %s from %s (%d, %d, %d) to %s (%d, %d, %d)",
                    player.getName(),
                    from.getWorld().getName(), from.getSignLocation().getBlockX(), from.getSignLocation().getBlockY(),
                    from.getSignLocation().getBlockZ(),
                    to.getWorld().getName(), to.getTeleportDestination().getBlockX(),
                    to.getTeleportDestination().getBlockY(), to.getTeleportDestination().getBlockZ()));

            // Check actual location after teleport (one tick later to be more accurate)
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location actual = player.getLocation();
                    plugin.getLogger()
                            .info(String.format("[DEBUG] %s actual location after teleport: %s (%.2f, %.2f, %.2f)",
                                    player.getName(), actual.getWorld().getName(), actual.getX(), actual.getY(),
                                    actual.getZ()));
                }
            }.runTaskLater(plugin, 1L);
        }

        // Play arrival effects
        if (plugin.getConfig().getBoolean("sounds-enabled", true)) {
            player.getWorld().playSound(destination, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 1.2f);
            player.playSound(destination, Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.0f);
        }

        if (plugin.getConfig().getBoolean("particles-enabled", true)) {
            spawnTeleportParticles(destination, to.getColor());
        }

        player.sendMessage(plugin.getMessage("teleporting", "%name%", to.getName()));
        return true;
    }

    private void spawnTeleportParticles(Location location, int color) {
        World world = location.getWorld();
        Particle.DustOptions dust = toDustOptions(color, 1.5f);

        // Spiral effect
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 4;
            double radius = 0.5 + (i / 30.0);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = i * 0.1;

            world.spawnParticle(Particle.DUST, location.clone().add(x, y, z), 1, dust);
        }

        world.spawnParticle(Particle.PORTAL, location, 50, 0.5, 1, 0.5, 0.1);
    }

    private Particle.DustOptions toDustOptions(int color, float size) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return new Particle.DustOptions(
                Color.fromRGB((int) (r * 255), (int) (g * 255), (int) (b * 255)), size);
    }

    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Portal portal : plugin.getPortalManager().getAllPortals()) {
                    // Only show enhanced particles for linked portals
                    if (!plugin.getPortalManager().hasPartner(portal)) {
                        continue;
                    }

                    // Don't render if no players nearby
                    boolean playerNearby = false;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(portal.getWorld())) {
                            Location portalLoc = portal.getTeleportDestination();
                            if (player.getLocation().distanceSquared(portalLoc) < 2500) { // 50 blocks
                                playerNearby = true;
                                break;
                            }
                        }
                    }

                    if (!playerNearby)
                        continue;

                    Particle.DustOptions dust = toDustOptions(portal.getColor(), 1.0f);

                    for (Location interior : portal.getInteriorBlocks()) {
                        if (Math.random() < 0.3) {
                            Location particleLoc = interior.clone().add(
                                    Math.random(), Math.random(), Math.random());
                            interior.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, dust);
                        }
                    }

                    // Occasional portal particle
                    if (Math.random() < 0.5) {
                        Location center = portal.getTeleportDestination();
                        center.getWorld().spawnParticle(Particle.PORTAL, center, 5, 0.3, 0.5, 0.3, 0.05);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 10L); // Every half second
    }
}
