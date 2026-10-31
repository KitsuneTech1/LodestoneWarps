package com.lodestonewarps;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private final LodestoneWarps plugin;

    public BlockBreakListener(LodestoneWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        PortalManager manager = plugin.getPortalManager();

        if (block.getType().name().contains("SIGN")) {
            Portal portal = manager.getPortalAtSign(block.getLocation());
            if (portal != null) {
                handlePortalDestruction(portal, player);
                return;
            }
        }

        if (block.getType() == Material.LODESTONE) {
            Portal portal = manager.getPortalAtFrame(block.getLocation());
            if (portal != null) {
                handlePortalDestruction(portal, player);
                return;
            }
        }
    }

    private void handlePortalDestruction(Portal portal, Player player) {
        PortalManager manager = plugin.getPortalManager();

        Portal partner = manager.getLinkedPortal(portal);
        manager.removePortal(portal);

        player.sendMessage(plugin.getMessage("portal-destroyed", "%name%", portal.getName()));

        if (partner != null) {
            Player partneruser = plugin.getServer().getPlayer(partner.getCreatorUUID());
            if (partneruser != null && partneruser != player) {
                partneruser.sendMessage(plugin.getMessage("portal-unlinked", "%name%", portal.getName()));
            }

            updatePortalSign(partner);
        }
    }

    private void updatePortalSign(Portal portal) {
        Block signBlock = portal.getSignLocation().getBlock();
        if (signBlock.getState() instanceof Sign sign) {
            sign.getSide(org.bukkit.block.sign.Side.FRONT).setLine(2, "§7Waiting...");
            sign.update();
        }
    }
}
