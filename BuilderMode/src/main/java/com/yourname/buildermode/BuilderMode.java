package com.yourname.buildermode;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BuilderMode extends JavaPlugin {
    
    private ConfigManager configManager;
    private RenderDistanceManager renderDistanceManager;
    private MovementRestrictionManager movementManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> elytraWarnings = new HashSet<>();
    
    @Override
    public void onEnable() {
        // Create default config if it doesn't exist
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        renderDistanceManager = new RenderDistanceManager(this, configManager);
        movementManager = new MovementRestrictionManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(movementManager, this);
        getLogger().info("Event listeners registered successfully");
        
        // Start render distance safety check
        startRenderDistanceSafetyCheck();
        
        getLogger().info("BuilderMode has been enabled!");
    }
    
    @Override
    public void onDisable() {
        renderDistanceManager.disableAll();
        getLogger().info("BuilderMode has been disabled!");
    }
    
    private void startRenderDistanceSafetyCheck() {
        long interval = configManager.getSafetyCheckInterval() * 20L; // Convert seconds to ticks
        
        if (configManager.isVerboseEnabled()) {
            getLogger().info("Starting safety check task with interval: " + configManager.getSafetyCheckInterval() + " seconds");
        }
        
        if (isFolia()) {
            getServer().getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
                performSafetyCheck();
            }, interval, interval);
        } else {
            getServer().getScheduler().runTaskTimer(this, () -> {
                performSafetyCheck();
            }, interval, interval);
        }
    }
    
    private void performSafetyCheck() {
        int defaultDistance = configManager.getDefaultRenderDistance();
        int resetCount = 0;
        int totalChecked = 0;
        
        if (configManager.isVerboseEnabled()) {
            getLogger().info("Running safety check...");
        }
        
        for (Player player : getServer().getOnlinePlayers()) {
            totalChecked++;
            if (!renderDistanceManager.isActive(player)) {
                int currentDistance = player.getViewDistance();
                
                if (configManager.isVerboseEnabled()) {
                    getLogger().info("Checking " + player.getName() + ": current=" + currentDistance + ", default=" + defaultDistance + ", buildermode=" + renderDistanceManager.isActive(player));
                }
                
                if (currentDistance != defaultDistance) {
                    player.setViewDistance(defaultDistance);
                    resetCount++;
                    
                    if (configManager.isVerboseEnabled()) {
                        getLogger().info("Safety check: Reset " + player.getName() + "'s render distance from " + currentDistance + " to " + defaultDistance);
                    }
                }
            }
        }
        
        if (configManager.isVerboseEnabled()) {
            getLogger().info("Safety check completed: Checked " + totalChecked + " player(s), reset " + resetCount + " player(s)");
        }
    }
    
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Handle reload command separately
        if (command.getName().equalsIgnoreCase("bmr")) {
            if (!sender.hasPermission("buildermode.reload")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }
            
            reloadConfig();
            configManager.reload();
            sender.sendMessage(configManager.getMessage("config-reloaded"));
            return true;
        }
        
        if (command.getName().equalsIgnoreCase("buildermode")) {
            
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getMessage("players-only"));
                return true;
            }
            
            Player player = (Player) sender;
            
            // Info command
            if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
                return handleInfoCommand(player);
            }
            
            // Off command
            if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
                return handleOffCommand(player);
            }
            
            // On command (or no args defaults to on)
            if (args.length == 0 || args[0].equalsIgnoreCase("on")) {
                return handleOnCommand(player);
            }
            
            // Debug command to test dimension change manually
            if (args.length > 0 && args[0].equalsIgnoreCase("testdimension")) {
                if (renderDistanceManager.isActive(player)) {
                    renderDistanceManager.disableOnDimensionChange(player);
                    player.sendMessage("§aManually triggered dimension change disable");
                } else {
                    player.sendMessage("§cBuilderMode is not active");
                }
                return true;
            }
            
            player.sendMessage(configManager.getMessage("invalid-usage"));
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("buildermode")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                String input = args[0].toLowerCase();
                
                for (String option : Arrays.asList("on", "off", "info")) {
                    if (option.startsWith(input)) {
                        completions.add(option);
                    }
                }
                
                return completions;
            }
        }
        
        return Collections.emptyList();
    }
    
    private boolean handleInfoCommand(Player player) {
        if (!configManager.isPluginEnabled()) {
            player.sendMessage(configManager.getMessage("plugin-disabled"));
            return true;
        }
        
        if (renderDistanceManager.isActive(player)) {
            long timeLeft = renderDistanceManager.getTimeRemaining(player);
            String message = configManager.getMessage("info-active")
                .replace("{time}", String.valueOf(timeLeft));
            player.sendMessage(message);
        } else {
            long cooldownTime = configManager.getCooldown(player.getWorld().getEnvironment());
            long currentTime = System.currentTimeMillis();
            
            if (cooldowns.containsKey(player.getUniqueId())) {
                long lastUsed = cooldowns.get(player.getUniqueId());
                long timeLeft = (cooldownTime - (currentTime - lastUsed)) / 1000;
                
                if (timeLeft > 0) {
                    String message = configManager.getMessage("info-cooldown")
                        .replace("{time}", String.valueOf(timeLeft));
                    player.sendMessage(message);
                } else {
                    player.sendMessage(configManager.getMessage("info-ready"));
                }
            } else {
                player.sendMessage(configManager.getMessage("info-ready"));
            }
        }
        
        return true;
    }
    
    private boolean handleOffCommand(Player player) {
        if (!configManager.isPluginEnabled()) {
            player.sendMessage(configManager.getMessage("plugin-disabled"));
            return true;
        }
        
        if (!renderDistanceManager.isActive(player)) {
            player.sendMessage(configManager.getMessage("not-active"));
            return true;
        }
        
        renderDistanceManager.disable(player);
        player.sendMessage(configManager.getMessage("disabled-manually"));
        return true;
    }
    
    private boolean handleOnCommand(Player player) {
        if (!configManager.isPluginEnabled()) {
            player.sendMessage(configManager.getMessage("plugin-disabled"));
            return true;
        }
        
        if (renderDistanceManager.isActive(player)) {
            player.sendMessage(configManager.getMessage("already-active"));
            return true;
        }
        
        // Check if riding an entity and kick them off if enabled
        if (configManager.isEntityRidingDisabled() && player.isInsideVehicle()) {
            player.leaveVehicle();
            player.sendMessage(configManager.getMessage("dismounted"));
        }
        
        // Check for elytra warning
        if (configManager.isElytraDisabled() && movementManager.isWearingElytra(player)) {
            if (!elytraWarnings.contains(player.getUniqueId())) {
                elytraWarnings.add(player.getUniqueId());
                player.sendMessage(configManager.getMessage("elytra-warning"));
                return true;
            } else {
                // Second attempt - remove warning flag
                elytraWarnings.remove(player.getUniqueId());
            }
        }
        
        long cooldownTime = configManager.getCooldown(player.getWorld().getEnvironment());
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(player.getUniqueId())) {
            long lastUsed = cooldowns.get(player.getUniqueId());
            long timeLeft = (cooldownTime - (currentTime - lastUsed)) / 1000;
            
            if (timeLeft > 0) {
                String message = configManager.getMessage("on-cooldown")
                    .replace("{time}", String.valueOf(timeLeft));
                player.sendMessage(message);
                return true;
            }
        }
        
        renderDistanceManager.enable(player);
        // Set cooldown immediately when activated
        cooldowns.put(player.getUniqueId(), currentTime);
        
        int distance = configManager.getRenderDistance(player.getWorld().getEnvironment());
        int duration = configManager.getDuration(player.getWorld().getEnvironment());
        
        String message = configManager.getMessage("activated")
            .replace("{distance}", String.valueOf(distance))
            .replace("{duration}", String.valueOf(duration));
        player.sendMessage(message);
        
        return true;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public RenderDistanceManager getRenderDistanceManager() {
        return renderDistanceManager;
    }
    
    public void removeElytraWarning(UUID uuid) {
        elytraWarnings.remove(uuid);
    }
    
    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }
}
