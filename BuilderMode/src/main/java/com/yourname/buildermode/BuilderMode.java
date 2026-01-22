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
        
        getServer().getPluginManager().registerEvents(movementManager, this);
        
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
        for (Player player : getServer().getOnlinePlayers()) {
            if (!renderDistanceManager.isActive(player)) {
                if (player.getViewDistance() != defaultDistance) {
                    player.setViewDistance(defaultDistance);
                }
            }
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
        
        // Check for elytra warning
        if (movementManager.isWearingElytra(player)) {
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
}