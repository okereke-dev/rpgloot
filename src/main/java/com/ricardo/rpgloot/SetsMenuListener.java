package com.ricardo.rpgloot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class SetsMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SetsMenu menu)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == menu.getInventory() && SetsMenu.isSetSlot(event.getSlot())) {
            menu.cycleRarity();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SetsMenu) {
            event.setCancelled(true);
        }
    }
}
