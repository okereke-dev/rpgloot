package com.ricardo.rpgloot;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class ArmorListener implements Listener {

    // Night Vision: cooldown in ms so it doesn't spam-proc on bleed ticks
    private static final long NIGHT_VISION_COOLDOWN_MS = 10_000;

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final Random random = new Random();
    private final Map<UUID, Long> nightVisionLastProc = new HashMap<>();

    public ArmorListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
    }

    /** Proc-based armor stats when the player takes damage. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null) continue;
            if (rarityService.getRarity(piece) == null) continue;
            for (RolledStat rolled : rarityService.getBonusStats(piece)) {
                applyArmorStat(event, player, rolled);
            }
        }
    }

    /** Thorns: reflect damage to attacker. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double totalThornsChance = 0;
        int thornsPieces = 0;

        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            if (rarityService.getRarity(piece) == null) continue;
            for (RolledStat rolled : rarityService.getBonusStats(piece)) {
                if (rolled.stat() == BonusStat.THORNS_CHANCE) {
                    totalThornsChance += rolled.value();
                    thornsPieces++;
                }
            }
        }

        if (thornsPieces > 0 && random.nextDouble() * 100.0 < (totalThornsChance / thornsPieces)) {
            double thornsDmg = event.getFinalDamage() * 0.3;
            if (event.getDamager() instanceof org.bukkit.entity.LivingEntity attacker) {
                attacker.damage(thornsDmg, player);
                DamageNumbers.show(plugin, attacker.getLocation(), thornsDmg, DamageNumbers.Type.NORMAL);
            }
        }
    }

    // ── Per-stat handlers ─────────────────────────────────────────────────

    private void applyArmorStat(EntityDamageEvent event, Player player, RolledStat rolled) {
        switch (rolled.stat()) {
            case DODGE_CHANCE -> {
                if (random.nextDouble() * 100.0 < rolled.value()) {
                    event.setDamage(0);
                    event.setCancelled(true);
                }
            }
            case DAMAGE_REDUCTION -> {
                double reduction = rolled.value() / 100.0;
                event.setDamage(event.getDamage() * (1.0 - reduction));
            }
            case FALL_REDUCTION -> {
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setDamage(event.getDamage() * (1.0 - rolled.value() / 100.0));
                }
            }
            case NIGHT_VISION_CHANCE -> {
                // Proc when health is low (<40%) with a per-player cooldown to prevent spam on bleed ticks
                if (player.getHealth() / player.getMaxHealth() >= 0.40) break;
                long now = System.currentTimeMillis();
                long last = nightVisionLastProc.getOrDefault(player.getUniqueId(), 0L);
                if (now - last < NIGHT_VISION_COOLDOWN_MS) break;
                if (random.nextDouble() * 100.0 < rolled.value()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, true, false));
                    nightVisionLastProc.put(player.getUniqueId(), now);
                }
            }
            // HEALTH_BOOST and SPEED_BOOST are passive attribute modifiers on the item — no listener needed
            default -> {}
        }
    }
}
