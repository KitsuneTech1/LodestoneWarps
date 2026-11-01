package com.lodestonewarps;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PortalFrameDetectorTest {

    @Test
    void detectsFrameWithinConfiguredBounds() {
        FakeWorld world = new FakeWorld();
        world.addFrame(3, 4);

        PortalFrameDetector detector = new PortalFrameDetector(1, 2, 2, 2);
        PortalFrameDetector.FrameResult result = detector.detectFrame(world.block(0, 0, 0));

        assertNotNull(result);
        assertEquals(10, result.getFrameBlocks().size());
        assertEquals(2, result.getInteriorBlocks().size());
    }

    @Test
    void rejectsFrameThatExceedsMaximumWidth() {
        FakeWorld world = new FakeWorld();
        world.addFrame(5, 4);

        PortalFrameDetector detector = new PortalFrameDetector(1, 2, 2, 2);

        assertNull(detector.detectFrame(world.block(0, 0, 0)));
    }

    @Test
    void rejectsFrameThatExceedsMaximumHeight() {
        FakeWorld world = new FakeWorld();
        world.addFrame(3, 5);

        PortalFrameDetector detector = new PortalFrameDetector(1, 2, 2, 2);

        assertNull(detector.detectFrame(world.block(0, 0, 0)));
    }

    @Test
    void portalDestinationUsesTheInteriorCenter() {
        FakeWorld world = new FakeWorld();
        Set<Location> frame = new HashSet<>();
        Set<Location> interior = Set.of(
                new Location(world.world, 10, 64, 20),
                new Location(world.world, 11, 64, 20),
                new Location(world.world, 10, 65, 20),
                new Location(world.world, 11, 65, 20));

        Portal portal = new Portal("Home", UUID.randomUUID(), "player",
                new Location(world.world, 9, 64, 20), frame, interior);

        Location destination = portal.getTeleportDestination();
        assertEquals(10.5, destination.getX());
        assertEquals(64.0, destination.getY());
        assertEquals(20.5, destination.getZ());
    }

    private static final class FakeWorld {
        private final Map<Key, Material> blocks = new HashMap<>();
        private final World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getName")) {
                        return "test-world";
                    }
                    if (method.getName().equals("hashCode")) {
                        return 1;
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    if (method.getName().equals("toString")) {
                        return "test-world";
                    }
                    return defaultValue(method.getReturnType());
                });

        private void addFrame(int width, int height) {
            for (int x = 0; x < width; x++) {
                set(x, 0, 0, Material.LODESTONE);
                set(x, height - 1, 0, Material.LODESTONE);
            }
            for (int y = 0; y < height; y++) {
                set(0, y, 0, Material.LODESTONE);
                set(width - 1, y, 0, Material.LODESTONE);
            }
        }

        private void set(int x, int y, int z, Material material) {
            blocks.put(new Key(x, y, z), material);
        }

        private Block block(int x, int y, int z) {
            return (Block) Proxy.newProxyInstance(
                    Block.class.getClassLoader(), new Class<?>[]{Block.class}, (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getType" -> blocks.getOrDefault(new Key(x, y, z), Material.AIR);
                            case "getLocation" -> new Location(world, x, y, z);
                            case "getRelative" -> {
                                BlockFace face = (BlockFace) args[0];
                                int distance = args.length == 2 ? (int) args[1] : 1;
                                yield block(x + face.getModX() * distance,
                                        y + face.getModY() * distance,
                                        z + face.getModZ() * distance);
                            }
                            case "hashCode" -> new Key(x, y, z).hashCode();
                            case "equals" -> proxy == args[0];
                            case "toString" -> "block(" + x + "," + y + "," + z + ")";
                            default -> defaultValue(method.getReturnType());
                        };
                    });
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == char.class) {
                return '\0';
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0.0f;
            }
            if (type == double.class) {
                return 0.0d;
            }
            return null;
        }
    }

    private record Key(int x, int y, int z) {
    }
}
