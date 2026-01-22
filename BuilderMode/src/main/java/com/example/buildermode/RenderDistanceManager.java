package com.yourname.buildermode;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RenderDistanceManager {
    
    private final BuilderMode plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Integer> originalRenderDistances = new HashMap<>();
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
        originalRenderDistances.put(player.getUniqueId(), currentDistance);
        
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
        expirationTimes.remove(uuid);
        plugin.removeElytraWarning(uuid);
        
        if (originalDistance != null) {
            player.setViewDistance(originalDistance);
            player.sendMessage(configManager.getMessage("expired"));
        }
    }
    
    public void disableAll() {
        for (UUID uuid : originalRenderDistances.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disable(player);
            }
        }
        originalRenderDistances.clear();
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