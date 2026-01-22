package com.yourname.buildermode;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final BuilderMode plugin;
    private FileConfiguration config;
    
    public ConfigManager(BuilderMode plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public int getRenderDistance(World.Environment environment) {
        String path = getEnvironmentPath(environment) + ".render-distance";
        return config.getInt(path, 32);
    }
    
    public int getDuration(World.Environment environment) {
        String path = getEnvironmentPath(environment) + ".duration";
        return config.getInt(path, 60);
    }
    
    public long getCooldown(World.Environment environment) {
        String path = getEnvironmentPath(environment) + ".cooldown";
        return config.getLong(path, 300) * 1000; // Convert to milliseconds
    }
    
    public int getDefaultRenderDistance() {
        return config.getInt("default-render-distance", 10);
    }
    
    public boolean isPluginEnabled() {
        return config.getBoolean("enabled", true);
    }
    
    public long getSafetyCheckInterval() {
        return config.getLong("safety-check-interval", 300);
    }
    
    public boolean isVerboseEnabled() {
        return config.getBoolean("verbose", false);
    }
    
    public String getMessage(String key) {
        String message = config.getString("messages." + key, getDefaultMessage(key));
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private String getDefaultMessage(String key) {
        switch (key) {
            case "no-permission":
                return "&cYou don't have permission to use this command!";
            case "players-only":
                return "&cOnly players can use this command!";
            case "config-reloaded":
                return "&aBuildderMode configuration reloaded!";
            case "already-active":
                return "&cBuilderMode is already active!";
            case "on-cooldown":
                return "&cYou must wait {time} seconds before using BuilderMode again!";
            case "activated":
                return "&aBuilderMode activated! Render distance set to {distance} chunks for {duration} seconds.";
            case "not-active":
                return "&cBuilderMode is not currently active!";
            case "disabled-manually":
                return "&eBuilderMode has been disabled manually.";
            case "expired":
                return "&eBuilderMode has expired. Render distance restored.";
            case "info-active":
                return "&aBuilderMode is active! Time remaining: &e{time} seconds";
            case "info-cooldown":
                return "&eBuilderMode is on cooldown. Time remaining: &c{time} seconds";
            case "info-ready":
                return "&aBuilderMode is ready to use!";
            case "invalid-usage":
                return "&cUsage: /buildermode [on|off|info|reload]";
            case "elytra-warning":
                return "&e&lWARNING: &eYou are wearing an elytra! It will be removed if you activate BuilderMode. Use &6/buildermode on &eagain to confirm.";
            case "elytra-removed-inventory":
                return "&eYour elytra has been moved to your inventory.";
            case "elytra-removed-ground":
                return "&eYour elytra has been dropped on the ground (inventory full).";
            case "cannot-ride-entity":
                return "&cYou cannot ride entities while BuilderMode is active (except minecarts)!";
            case "cannot-use-elytra":
                return "&cYou cannot use elytra while BuilderMode is active!";
            case "cannot-equip-elytra":
                return "&cYou cannot equip elytra while BuilderMode is active!";
            case "plugin-disabled":
                return "&cBuilderMode is currently disabled!";
            case "dimension-disabled":
                return "&eBuilderMode has been disabled because you changed dimensions.";
            default:
                return "&cUnknown message key: " + key;
        }
    }
    
    private String getEnvironmentPath(World.Environment environment) {
        switch (environment) {
            case NETHER:
                return "dimensions.nether";
            case THE_END:
                return "dimensions.the_end";
            default:
                return "dimensions.overworld";
        }
    }
}