package com.okereke.rpgloot;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;

/**
 * Forces crafted (and Crafter-block) tools to become RPGLoot tools. Crafted axes are always
 * {@link WeaponType#AXE_TOOL}. Also handles netherite smithing upgrades for tools.
 */
public final class CraftListener implements Listener {

    private final RPGLootPlugin plugin;
    private final ToolCrafting toolCrafting;
    private final ItemRarityService rarityService;

    public CraftListener(RPGLootPlugin plugin, ToolCrafting toolCrafting, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.toolCrafting = toolCrafting;
        this.rarityService = rarityService;
        registerCrafterHook();
    }

    public void reload() {
        toolCrafting.reload(plugin.getConfig());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!toolCrafting.isEnabled()) return;
        if (event.isRepair()) return;
        if (event.getRecipe() == null) return;

        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null || result.getType().isAir()) return;
        if (!ToolCrafting.isToolOrAxeMaterial(result.getType())) return;
        if (rarityService.getRarity(result) != null) return;

        inv.setResult(toolCrafting.toRpgTool(result, null));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!toolCrafting.isEnabled()) return;

        SmithingInventory inv = event.getInventory();
        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) return;
        if (!ToolCrafting.isToolOrAxeMaterial(result.getType())) return;

        ItemStack base = inv.getInputEquipment();
        Rarity preserved = base != null ? rarityService.getRarity(base) : null;
        event.setResult(toolCrafting.toRpgTool(result, preserved));
    }

    /**
     * CrafterCraftEvent exists on 1.21+ / Paper 26.x but this project may compile against older
     * paper-api — register reflectively so Crafters cannot bypass tool conversion.
     */
    @SuppressWarnings("unchecked")
    private void registerCrafterHook() {
        try {
            Class<? extends Event> eventClass =
                    (Class<? extends Event>) Class.forName("org.bukkit.event.block.CrafterCraftEvent");
            Method getResult = eventClass.getMethod("getResult");
            Method setResult = eventClass.getMethod("setResult", ItemStack.class);

            EventExecutor executor = (listener, event) -> {
                try {
                    if (!toolCrafting.isEnabled()) return;
                    ItemStack result = (ItemStack) getResult.invoke(event);
                    if (result == null || result.getType().isAir()) return;
                    if (!ToolCrafting.isToolOrAxeMaterial(result.getType())) return;
                    if (rarityService.getRarity(result) != null) return;
                    setResult.invoke(event, toolCrafting.toRpgTool(result, null));
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().warning("CrafterCraftEvent tool convert failed: " + ex.getMessage());
                }
            };

            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    this,
                    EventPriority.HIGH,
                    executor,
                    plugin,
                    true
            );
            plugin.getLogger().info("CrafterCraftEvent hook registered — crafted tools via Crafter become RPGLoot");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("CrafterCraftEvent not present on this server — player crafting still converts tools");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not register CrafterCraftEvent hook: " + e.getMessage());
        }
    }
}
