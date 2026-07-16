package com.okereke.rpgloot;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Gives hostile (non-boss) mobs a chance to spawn already wielding/wearing a real RPGLoot
 * item — visible loot signal before a kill even happens, separate from and additive to the
 * on-death drop-chance in {@link LootListener}. Reuses LootListener's tier/pool/rarity logic
 * as-is so this doesn't introduce a second rarity system to keep in sync. If the mob dies,
 * the item it's wearing is what drops (its slot's drop chance is set to 100%).
 */
public final class MobEquipListener implements Listener {

    private final RPGLootPlugin plugin;
    private final LootListener lootListener;
    private final Random random = new Random();

    public MobEquipListener(RPGLootPlugin plugin, ItemRarityService rarityService, LootListener lootListener) {
        this.plugin = plugin;
        this.lootListener = lootListener;
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster) || lootListener.isBoss(entity)) return;
        if (!plugin.getConfig().getBoolean("mob-equip.enabled", true)) return;
        if (!plugin.isDropsAllowed(entity.getLocation())) return;

        double chance = plugin.getConfig().getDouble("mob-equip.chance", 0.15);
        if (random.nextDouble() > chance) return;

        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;

        int tier = lootListener.getMobTier(entity);
        Rarity rarity = lootListener.rollRarity(entity);
        ItemStack item = lootListener.rollDropItem(entity, tier, rarity);
        equip(equipment, item);
    }

    /** Puts the item in the matching equipment slot for its material, and marks that slot to drop 100% of the time on death. */
    private void equip(EntityEquipment equipment, ItemStack item) {
        String name = item.getType().name();
        if (name.endsWith("_HELMET")) {
            equipment.setHelmet(item);
            equipment.setHelmetDropChance(1.0f);
        } else if (name.endsWith("_CHESTPLATE")) {
            equipment.setChestplate(item);
            equipment.setChestplateDropChance(1.0f);
        } else if (name.endsWith("_LEGGINGS")) {
            equipment.setLeggings(item);
            equipment.setLeggingsDropChance(1.0f);
        } else if (name.endsWith("_BOOTS")) {
            equipment.setBoots(item);
            equipment.setBootsDropChance(1.0f);
        } else {
            equipment.setItemInMainHand(item);
            equipment.setItemInMainHandDropChance(1.0f);
        }
    }
}
