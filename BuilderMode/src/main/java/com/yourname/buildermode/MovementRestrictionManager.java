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
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MovementRestrictionManager implements Listener {
    
    private final BuilderMode plugin;
    
    public MovementRestrictionManager(BuilderMode plugin) {
        this.plugin = plugin;
        
        // Start a repeating task to check for elytras
        if (isFolia()) {
            startFoliaElytraCheck();
        } else {
            startBukkitElytraCheck();
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
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getRenderDistanceManager().isActive(player)) {
                    checkAndRemoveElytra(player);
                }
            }
        }, 1L, 10L);
    }
    
    private void startBukkitElytraCheck() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getRenderDistanceManager().isActive(player)) {
                    checkAndRemoveElytra(player);
                }
            }
        }, 1L, 10L);
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
        
        // Allow minecarts only
        EntityType type = event.getVehicle().getType();
        if (type != EntityType.MINECART && 
            type != EntityType.CHEST_MINECART && 
            type != EntityType.FURNACE_MINECART && 
            type != EntityType.HOPPER_MINECART && 
            type != EntityType.TNT_MINECART && 
            type != EntityType.COMMAND_BLOCK_MINECART &&
            type != EntityType.SPAWNER_MINECART) {
            
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-ride-entity"));
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
        
        // Prevent right-clicking with elytra to equip
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.ELYTRA) {
            if (event.getHand() == EquipmentSlot.HAND || event.getHand() == EquipmentSlot.OFF_HAND) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-equip-elytra"));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        // Only update if BuilderMode is active
        if (!plugin.getRenderDistanceManager().isActive(player)) {
            return;
        }
        
        // Update the render distance for the new dimension
        plugin.getRenderDistanceManager().updateDimension(player);
    }
    
    public boolean isWearingElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
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