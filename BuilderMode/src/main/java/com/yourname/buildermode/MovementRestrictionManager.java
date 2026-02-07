package com.yourname.buildermode;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MovementRestrictionManager implements Listener {
    
    private final BuilderMode plugin;
    
    public MovementRestrictionManager(BuilderMode plugin) {
        this.plugin = plugin;
        
        // Log that this manager is being initialized
        plugin.getLogger().info("MovementRestrictionManager initialized");
        
        // Start a repeating task to check for elytras
        if (isFolia()) {
            startFoliaElytraCheck();
            startFoliaMountCheck();
        } else {
            startBukkitElytraCheck();
            startBukkitMountCheck();
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
    
    private void startFoliaElytraCheck() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            // Only check if elytra is disabled
            if (!plugin.getConfigManager().isElytraDisabled()) {
                return;
            }
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getRenderDistanceManager().isActive(player)) {
                    checkAndRemoveElytra(player);
                }
            }
        }, 1L, 10L);
    }
    
    private void startBukkitElytraCheck() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Only check if elytra is disabled
            if (!plugin.getConfigManager().isElytraDisabled()) {
                return;
            }
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getRenderDistanceManager().isActive(player)) {
                    checkAndRemoveElytra(player);
                }
            }
        }, 1L, 10L);
    }
    
    private void startFoliaMountCheck() {
        long interval = plugin.getConfigManager().getMountCheckInterval() * 20L; // Convert to ticks
        
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            // Only check if entity riding is disabled
            if (!plugin.getConfigManager().isEntityRidingDisabled()) {
                return;
            }
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getRenderDistanceManager().isActive(player)) {
                    checkAndDismount(player);
                }
            }
        }, interval, interval);
    }
    
    private void startBukkitMountCheck() {
        long interval = plugin.getConfigManager().getMountCheckInterval() * 20L; // Convert to ticks
        
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Only check if entity riding is disabled
            if (!plugin.getConfigManager().isEntityRidingDisabled()) {
                return;
            }
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getRenderDistanceManager().isActive(player)) {
                    checkAndDismount(player);
                }
            }
        }, interval, interval);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntered();
        
        if (!plugin.getRenderDistanceManager().isActive(player)) {
            return;
        }
        
        // Check if entity riding prevention is enabled
        if (!plugin.getConfigManager().isEntityRidingDisabled()) {
            return;
        }
        
        // Block ALL vehicle mounting
        event.setCancelled(true);
        player.sendMessage(plugin.getConfigManager().getMessage("cannot-ride-entity"));
        
        if (plugin.getConfigManager().isVerboseEnabled()) {
            plugin.getLogger().info("Blocked " + player.getName() + " from mounting " + event.getVehicle().getType());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onElytraToggle(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (!plugin.getRenderDistanceManager().isActive(player)) {
            return;
        }
        
        // Check if elytra is disabled
        if (!plugin.getConfigManager().isElytraDisabled()) {
            return;
        }
        
        if (event.isGliding()) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-use-elytra"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (!plugin.getRenderDistanceManager().isActive(player)) {
            return;
        }
        
        // Check if elytra is disabled
        if (!plugin.getConfigManager().isElytraDisabled()) {
            return;
        }
        
        // Check if they're trying to equip an elytra
        if (event.getSlot() == 38) { // Chest slot
            ItemStack item = event.getCursor();
            if (item != null && item.getType() == Material.ELYTRA) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-equip-elytra"));
            }
        }
        
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ELYTRA) {
            if (event.isShiftClick() && event.getSlot() != 38) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-equip-elytra"));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getRenderDistanceManager().isActive(player)) {
            return;
        }
        
        // Check if elytra is disabled
        if (!plugin.getConfigManager().isElytraDisabled()) {
            return;
        }
        
        // Prevent right-clicking with elytra to equip
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.ELYTRA) {
            if (event.getHand() == EquipmentSlot.HAND || event.getHand() == EquipmentSlot.OFF_HAND) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-equip-elytra"));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Check if teleporting to a different world
        if (event.getFrom().getWorld() != null && event.getTo() != null && event.getTo().getWorld() != null) {
            if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
                plugin.getLogger().info("Player " + player.getName() + " teleporting to different world. BuilderMode active: " + plugin.getRenderDistanceManager().isActive(player));
                
                // Disable BuilderMode if active when changing dimensions
                if (plugin.getRenderDistanceManager().isActive(player)) {
                    plugin.getLogger().info("Disabling BuilderMode for " + player.getName() + " due to dimension change via teleport");
                    plugin.getRenderDistanceManager().disableOnDimensionChange(player);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        plugin.getLogger().info("Player " + player.getName() + " changed world. BuilderMode active: " + plugin.getRenderDistanceManager().isActive(player));
        
        // Disable BuilderMode if active when changing dimensions
        if (plugin.getRenderDistanceManager().isActive(player)) {
            plugin.getLogger().info("Disabling BuilderMode for " + player.getName() + " due to dimension change");
            plugin.getRenderDistanceManager().disableOnDimensionChange(player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Disable BuilderMode and start cooldown when player logs out
        if (plugin.getRenderDistanceManager().isActive(player)) {
            if (plugin.getConfigManager().isVerboseEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " disconnected while BuilderMode was active. Disabling and starting cooldown.");
            }
            plugin.getRenderDistanceManager().disableOnLogout(player);
        }
    }
    
    public boolean isWearingElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }
    
    private void checkAndDismount(Player player) {
        // Check if player is riding/mounted on anything
        if (player.isInsideVehicle()) {
            if (plugin.getConfigManager().isVerboseEnabled()) {
                plugin.getLogger().info("Mount check: Dismounting " + player.getName() + " from " + 
                    (player.getVehicle() != null ? player.getVehicle().getType() : "unknown"));
            }
            
            player.leaveVehicle();
            player.sendMessage(plugin.getConfigManager().getMessage("dismounted-check"));
        }
    }
    
    private void checkAndRemoveElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        
        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            player.getInventory().setChestplate(null);
            
            // Try to add to inventory
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(chestplate);
                player.sendMessage(plugin.getConfigManager().getMessage("elytra-removed-inventory"));
            } else {
                // Drop on ground if inventory is full - use region scheduler for Folia
                final ItemStack elytraToDrop = chestplate.clone();
                if (isFolia()) {
                    player.getScheduler().run(plugin, (task) -> {
                        player.getWorld().dropItemNaturally(player.getLocation(), elytraToDrop);
                    }, null);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), elytraToDrop);
                }
                player.sendMessage(plugin.getConfigManager().getMessage("elytra-removed-ground"));
            }
        }
    }
}
