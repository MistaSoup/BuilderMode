package com.yourname.buildermode;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RenderDistanceManager {
    
    private final BuilderMode plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Integer> originalRenderDistances = new HashMap<>();
    private final Map<UUID, World.Environment> lastKnownDimension = new HashMap<>();
    private final Map<UUID, Object> activeTasks = new HashMap<>();
    private final Map<UUID, Long> expirationTimes = new HashMap<>();
    private final boolean isFolia;
    
    public RenderDistanceManager(BuilderMode plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.isFolia = checkFolia();
        
        // Start dimension change checker
        startDimensionChecker();
    }
    
    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void startDimensionChecker() {
        // Check for dimension changes every second (20 ticks)
        if (isFolia) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
                checkDimensionChanges();
            }, 20L, 20L);
        } else {
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                checkDimensionChanges();
            }, 20L, 20L);
        }
    }
    
    private void checkDimensionChanges() {
        for (UUID uuid : new HashMap<>(originalRenderDistances).keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                World.Environment currentDim = player.getWorld().getEnvironment();
                World.Environment lastDim = lastKnownDimension.get(uuid);
                
                if (lastDim != null && lastDim != currentDim) {
                    if (configManager.isVerboseEnabled()) {
                        plugin.getLogger().info("Detected dimension change for " + player.getName() + 
                            " from " + lastDim + " to " + currentDim + ". Disabling BuilderMode.");
                    }
                    disableOnDimensionChange(player);
                }
            }
        }
    }
    
    public void enable(Player player) {
        if (isActive(player)) {
            return;
        }
        
        int currentDistance = player.getViewDistance();
        originalRenderDistances.put(player.getUniqueId(), currentDistance);
        
        // Track the dimension they activated in
        lastKnownDimension.put(player.getUniqueId(), player.getWorld().getEnvironment());
        
        int newDistance = configManager.getRenderDistance(player.getWorld().getEnvironment());
        player.setViewDistance(newDistance);
        
        int duration = configManager.getDuration(player.getWorld().getEnvironment());
        long expirationTime = System.currentTimeMillis() + (duration * 1000L);
        expirationTimes.put(player.getUniqueId(), expirationTime);
        
        if (isFolia) {
            scheduleFoliaTask(player, duration);
        } else {
            scheduleBukkitTask(player, duration);
        }
    }
    
    private void scheduleFoliaTask(Player player, int duration) {
        ScheduledTask task = player.getScheduler().runDelayed(plugin, (t) -> {
            disable(player);
        }, null, duration * 20L);
        
        activeTasks.put(player.getUniqueId(), task);
    }
    
    private void scheduleBukkitTask(Player player, int duration) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            disable(player);
        }, duration * 20L);
        
        activeTasks.put(player.getUniqueId(), task);
    }
    
    public void disable(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!originalRenderDistances.containsKey(uuid)) {
            return;
        }
        
        Object task = activeTasks.remove(uuid);
        if (task != null) {
            if (isFolia && task instanceof ScheduledTask) {
                ((ScheduledTask) task).cancel();
            } else if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
        }
        
        Integer originalDistance = originalRenderDistances.remove(uuid);
        lastKnownDimension.remove(uuid);
        expirationTimes.remove(uuid);
        plugin.removeElytraWarning(uuid);
        
        if (originalDistance != null) {
            player.setViewDistance(originalDistance);
            player.sendMessage(configManager.getMessage("expired"));
        }
    }
    
    public void disableOnDimensionChange(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!originalRenderDistances.containsKey(uuid)) {
            return;
        }
        
        Object task = activeTasks.remove(uuid);
        if (task != null) {
            if (isFolia && task instanceof ScheduledTask) {
                ((ScheduledTask) task).cancel();
            } else if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
        }
        
        originalRenderDistances.remove(uuid);
        lastKnownDimension.remove(uuid);
        expirationTimes.remove(uuid);
        plugin.removeElytraWarning(uuid);
        
        // Set to default render distance for the new dimension
        int defaultDistance = configManager.getDefaultRenderDistance();
        player.setViewDistance(defaultDistance);
        player.sendMessage(configManager.getMessage("dimension-disabled"));
        
        // Start cooldown immediately
        plugin.setCooldown(uuid);
    }
    
    public void disableOnLogout(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!originalRenderDistances.containsKey(uuid)) {
            return;
        }
        
        Object task = activeTasks.remove(uuid);
        if (task != null) {
            if (isFolia && task instanceof ScheduledTask) {
                ((ScheduledTask) task).cancel();
            } else if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
        }
        
        originalRenderDistances.remove(uuid);
        lastKnownDimension.remove(uuid);
        expirationTimes.remove(uuid);
        plugin.removeElytraWarning(uuid);
        
        // Start cooldown immediately when logging out
        plugin.setCooldown(uuid);
    }
    
    public void disableAll() {
        for (UUID uuid : originalRenderDistances.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disable(player);
            }
        }
        originalRenderDistances.clear();
        lastKnownDimension.clear();
        activeTasks.clear();
        expirationTimes.clear();
    }
    
    public boolean isActive(Player player) {
        return originalRenderDistances.containsKey(player.getUniqueId());
    }
    
    public long getTimeRemaining(Player player) {
        Long expirationTime = expirationTimes.get(player.getUniqueId());
        if (expirationTime == null) {
            return 0;
        }
        
        long remaining = (expirationTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}