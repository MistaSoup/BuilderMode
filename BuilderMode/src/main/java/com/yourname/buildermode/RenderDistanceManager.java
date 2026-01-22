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
    private final Map<UUID, World.Environment> originalDimensions = new HashMap<>();
    private final Map<UUID, Object> activeTasks = new HashMap<>();
    private final Map<UUID, Long> expirationTimes = new HashMap<>();
    private final boolean isFolia;
    
    public RenderDistanceManager(BuilderMode plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.isFolia = checkFolia();
    }
    
    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public void enable(Player player) {
        if (isActive(player)) {
            return;
        }
        
        int currentDistance = player.getViewDistance();
        World.Environment currentDimension = player.getWorld().getEnvironment();
        
        originalRenderDistances.put(player.getUniqueId(), currentDistance);
        originalDimensions.put(player.getUniqueId(), currentDimension);
        
        int newDistance = configManager.getRenderDistance(currentDimension);
        player.setViewDistance(newDistance);
        
        int duration = configManager.getDuration(currentDimension);
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
        originalDimensions.remove(uuid);
        expirationTimes.remove(uuid);
        plugin.removeElytraWarning(uuid);
        
        if (originalDistance != null) {
            player.setViewDistance(originalDistance);
            player.sendMessage(configManager.getMessage("expired"));
        }
    }
    
    public void updateDimension(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!originalRenderDistances.containsKey(uuid)) {
            return;
        }
        
        World.Environment newDimension = player.getWorld().getEnvironment();
        World.Environment originalDimension = originalDimensions.get(uuid);
        
        // If this is their first dimension (where they activated), save it
        if (originalDimension == null) {
            originalDimensions.put(uuid, newDimension);
            return;
        }
        
        // Set the new dimension's BuilderMode render distance
        int newDistance = configManager.getRenderDistance(newDimension);
        player.setViewDistance(newDistance);
        
        String message = configManager.getMessage("dimension-changed")
            .replace("{distance}", String.valueOf(newDistance));
        player.sendMessage(message);
    }
    
    public void disableAll() {
        for (UUID uuid : originalRenderDistances.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disable(player);
            }
        }
        originalRenderDistances.clear();
        originalDimensions.clear();
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