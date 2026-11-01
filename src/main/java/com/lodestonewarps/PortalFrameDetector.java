package com.lodestonewarps;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Set;

public class PortalFrameDetector {

    private final int minWidth;
    private final int minHeight;
    private final int maxWidth;
    private final int maxHeight;

    public PortalFrameDetector(LodestoneWarps plugin) {
        this(
                plugin.getConfig().getInt("min-portal-width", 1),
                plugin.getConfig().getInt("min-portal-height", 2),
                plugin.getConfig().getInt("max-portal-width", 10),
                plugin.getConfig().getInt("max-portal-height", 10));
    }

    PortalFrameDetector(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    /**
     * Detects a valid portal frame starting from a lodestone block.
     * Returns a FrameResult containing frame blocks and interior blocks, or null if
     * invalid.
     */
    public FrameResult detectFrame(Block startBlock) {
        if (startBlock.getType() != Material.LODESTONE) {
            return null;
        }

        // Try to find a complete rectangular frame
        // We'll check in both X-axis and Z-axis orientations
        FrameResult result = tryDetectFrameInDirection(startBlock, BlockFace.EAST); // X-axis portal
        if (result != null)
            return result;

        result = tryDetectFrameInDirection(startBlock, BlockFace.SOUTH); // Z-axis portal
        return result;
    }

    private FrameResult tryDetectFrameInDirection(Block startBlock, BlockFace direction) {
        Block bottomLeft = findCorner(startBlock, BlockFace.DOWN, direction.getOppositeFace());
        if (bottomLeft == null)
            return null;

        return traceFrame(bottomLeft, direction);
    }

    private Block findCorner(Block block, BlockFace vertical, BlockFace horizontal) {
        Block current = block;

        while (current.getRelative(vertical).getType() == Material.LODESTONE) {
            current = current.getRelative(vertical);
        }

        while (current.getRelative(horizontal).getType() == Material.LODESTONE) {
            current = current.getRelative(horizontal);
        }

        return current;
    }

    private FrameResult traceFrame(Block bottomLeft, BlockFace widthDirection) {
        Set<Location> frameBlocks = new HashSet<>();
        Set<Location> interiorBlocks = new HashSet<>();

        // Measure bottom edge
        int width = 0;
        Block current = bottomLeft;
        while (current.getType() == Material.LODESTONE && width <= maxWidth + 2) {
            frameBlocks.add(current.getLocation());
            current = current.getRelative(widthDirection);
            width++;
        }

        if (width > maxWidth + 2) {
            return null; // Too wide
        }

        if (width < minWidth + 2) {
            return null; // Too narrow (need 2 extra for the frame sides)
        }

        Block bottomRight = bottomLeft.getRelative(widthDirection, width - 1);

        // Measure left edge (height)
        int height = 0;
        current = bottomLeft;
        while (current.getType() == Material.LODESTONE && height <= maxHeight + 2) {
            frameBlocks.add(current.getLocation());
            current = current.getRelative(BlockFace.UP);
            height++;
        }

        if (height > maxHeight + 2) {
            return null; // Too tall
        }

        if (height < minHeight + 2) {
            return null; // Too short
        }

        Block topLeft = bottomLeft.getRelative(BlockFace.UP, height - 1);

        // Verify and add right edge
        current = bottomRight;
        for (int i = 0; i < height; i++) {
            if (current.getType() != Material.LODESTONE) {
                return null; // Right edge incomplete
            }
            frameBlocks.add(current.getLocation());
            current = current.getRelative(BlockFace.UP);
        }

        // Verify and add top edge
        current = topLeft;
        for (int i = 0; i < width; i++) {
            if (current.getType() != Material.LODESTONE) {
                return null; // Top edge incomplete
            }
            frameBlocks.add(current.getLocation());
            current = current.getRelative(widthDirection);
        }

        // Check interior is all air
        int interiorWidth = width - 2;
        int interiorHeight = height - 2;

        Block interiorStart = bottomLeft.getRelative(widthDirection).getRelative(BlockFace.UP);

        for (int x = 0; x < interiorWidth; x++) {
            for (int y = 0; y < interiorHeight; y++) {
                Block interiorBlock = interiorStart.getRelative(widthDirection, x).getRelative(BlockFace.UP, y);
                if (!isAir(interiorBlock.getType())) {
                    return null; // Interior not empty
                }
                interiorBlocks.add(interiorBlock.getLocation());
            }
        }

        if (interiorBlocks.isEmpty()) {
            return null;
        }

        return new FrameResult(frameBlocks, interiorBlocks);
    }

    private boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    public static class FrameResult {
        private final Set<Location> frameBlocks;
        private final Set<Location> interiorBlocks;

        public FrameResult(Set<Location> frameBlocks, Set<Location> interiorBlocks) {
            this.frameBlocks = frameBlocks;
            this.interiorBlocks = interiorBlocks;
        }

        public Set<Location> getFrameBlocks() {
            return frameBlocks;
        }

        public Set<Location> getInteriorBlocks() {
            return interiorBlocks;
        }
    }
}
