package com.ricardo.rpgloot;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public final class LootListener implements Listener {

    private static final List<Material> WEAPON_POOL = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.TRIDENT, Material.MACE);

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final RarityRoller roller;
    private final Random random = new Random();

    public LootListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.roller = new RarityRoller(plugin.getConfig(), plugin.getLogger());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }
        if (!(entity.getKiller() instanceof Player)) {
            return;
        }

        double dropChance = plugin.getConfig().getDouble("drop-chance", 0.08);
        if (random.nextDouble() > dropChance) {
            return;
        }

        Material weaponMaterial = WEAPON_POOL.get(random.nextInt(WEAPON_POOL.size()));
        Rarity rarity = roller.roll();

        ItemStack weapon = new ItemStack(weaponMaterial);
        rarityService.applyRarity(weapon, rarity);

        event.getDrops().add(weapon);
    }
}
