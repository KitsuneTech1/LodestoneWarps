package com.lodestonewarps;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Locale;

public class SignListener implements Listener {

    private final LodestoneWarps plugin;
    private final PortalFrameDetector frameDetector;

    public SignListener(LodestoneWarps plugin) {
        this.plugin = plugin;
        this.frameDetector = new PortalFrameDetector(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block signBlock = event.getBlock();

        Block attachedBlock = getAttachedBlock(signBlock);
        if (attachedBlock == null || attachedBlock.getType() != Material.LODESTONE) {
            return;
        }

        if (!player.hasPermission("lodestonewarps.create")) {
            return;
        }

        String portalName = event.getLine(0);
        if (portalName == null || portalName.trim().isEmpty()) {
            return;
        }

        portalName = portalName.trim().toLowerCase(Locale.ROOT);

        // Check if this name already has 2 portals
        if (!plugin.getPortalManager().isNameAvailable(portalName)) {
            player.sendMessage(plugin.getMessage("portal-linked", "%name%", portalName)
                    .replace("linked", "name already has 2 portals"));
            return;
        }

        PortalFrameDetector.FrameResult frameResult = frameDetector.detectFrame(attachedBlock);
        if (frameResult == null) {
            player.sendMessage(plugin.getMessage("invalid-frame"));
            return;
        }

        PortalManager manager = plugin.getPortalManager();
        for (var loc : frameResult.getFrameBlocks()) {
            if (manager.getPortalAtFrame(loc) != null) {
                player.sendMessage("§cThis frame is already part of another portal!");
                return;
            }
        }

        Portal portal = new Portal(
                portalName,
                player.getUniqueId(),
                player.getName(),
                signBlock.getLocation(),
                frameResult.getFrameBlocks(),
                frameResult.getInteriorBlocks());

        manager.registerPortal(portal);

        if (manager.hasPartner(portal)) {
            player.sendMessage(plugin.getMessage("portal-linked", "%name%", portalName));

            Portal partner = manager.getLinkedPortal(portal);
            if (partner != null) {
                Player partnerCreator = plugin.getServer().getPlayer(partner.getCreatorUUID());
                if (partnerCreator != null && partnerCreator != player) {
                    partnerCreator.sendMessage(plugin.getMessage("portal-linked", "%name%", portalName));
                }

                updatePortalSign(partner, true);
            }
        } else {
            player.sendMessage(plugin.getMessage("portal-created", "%name%", portalName));
        }

        event.setLine(0, "§5[LodeWarp]");
        event.setLine(1, "§d" + portalName);
        if (manager.hasPartner(portal)) {
            event.setLine(2, "§a✓ Linked");
        } else {
            event.setLine(2, "§7Waiting...");
        }
    }

    private void updatePortalSign(Portal portal, boolean linked) {
        Block signBlock = portal.getSignLocation().getBlock();
        if (signBlock.getState() instanceof Sign sign) {
            sign.getSide(org.bukkit.block.sign.Side.FRONT).setLine(2, linked ? "§a✓ Linked" : "§7Waiting...");
            sign.update();
        }
    }

    private Block getAttachedBlock(Block signBlock) {
        Material type = signBlock.getType();

        // Wall signs (not hanging)
        if (type.name().contains("WALL_SIGN") && !type.name().contains("HANGING")) {
            if (signBlock.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign) {
                return signBlock.getRelative(wallSign.getFacing().getOppositeFace());
            }
        }

        // Standing signs are attached to block below
        if (type.name().contains("SIGN") && !type.name().contains("WALL") && !type.name().contains("HANGING")) {
            return signBlock.getRelative(BlockFace.DOWN);
        }

        // Hanging signs - check for wall-mounted hanging signs
        if (type.name().contains("HANGING_SIGN")) {
            if (type.name().contains("WALL")) {
                // Wall-mounted hanging sign
                if (signBlock.getBlockData() instanceof WallHangingSign wallHangingSign) {
                    return signBlock.getRelative(wallHangingSign.getFacing().getOppositeFace());
                }
            } else {
                // Chain-mounted hanging sign - attached to block above
                return signBlock.getRelative(BlockFace.UP);
            }
        }

        return null;
    }
}
