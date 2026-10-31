package com.lodestonewarps;

import org.bukkit.plugin.java.JavaPlugin;

public class LodestoneWarps extends JavaPlugin {

    private static LodestoneWarps instance;
    private PortalManager portalManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        portalManager = new PortalManager(this);
        portalManager.loadPortals();

        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);

        getCommand("lodewarps").setExecutor(new LodewarpsCommand(this));
        getCommand("lodewarps").setTabCompleter(new LodewarpsCommand(this));
        
        getLogger().info("LodestoneWarps enabled! Create portals with lodestone frames and signs.");
    }

    @Override
    public void onDisable() {
        if (portalManager != null) {
            portalManager.savePortals();
        }
        getLogger().info("LodestoneWarps disabled.");
    }

    public static LodestoneWarps getInstance() {
        return instance;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public String getMessage(String key) {
        String msg = getConfig().getString("messages." + key, "&cMissing message: " + key);
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getMessage(String key, String placeholder, String value) {
        return getMessage(key).replace(placeholder, value);
    }
}
