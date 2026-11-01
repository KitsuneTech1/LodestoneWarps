package com.lodestonewarps;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalListenerTest {

    @Test
    void nullMovementDestinationIsNotABlockChange() {
        Location from = new Location(testWorld(), 1, 2, 3);

        assertFalse(PortalListener.isBlockChange(from, null));
    }

    @Test
    void failedTeleportDoesNotRecordCooldownOrArrivalLocation() {
        UUID playerId = UUID.randomUUID();
        Map<UUID, Long> cooldowns = new HashMap<>();
        Map<UUID, Location> arrivals = new HashMap<>();
        Location destination = new Location(testWorld(), 4, 5, 6);

        assertFalse(PortalListener.recordSuccessfulTeleport(false, playerId, destination,
                cooldowns, arrivals, 123L));
        assertTrue(cooldowns.isEmpty());
        assertTrue(arrivals.isEmpty());
    }

    @Test
    void successfulTeleportRecordsAnIndependentArrivalLocation() {
        UUID playerId = UUID.randomUUID();
        Map<UUID, Long> cooldowns = new HashMap<>();
        Map<UUID, Location> arrivals = new HashMap<>();
        Location destination = new Location(testWorld(), 4, 5, 6);

        assertTrue(PortalListener.recordSuccessfulTeleport(true, playerId, destination,
                cooldowns, arrivals, 123L));
        destination.setX(99);

        assertEquals(123L, cooldowns.get(playerId));
        assertEquals(4, arrivals.get(playerId).getBlockX());
    }

    private static World testWorld() {
        return (World) Proxy.newProxyInstance(
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
                    return null;
                });
    }
}
