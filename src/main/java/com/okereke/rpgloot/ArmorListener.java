package com.okereke.rpgloot;

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

    // Hard caps to prevent stacking from becoming overpowered
    private static final double MAX_DODGE_PCT       = 50.0;
    private static final double MAX_REDUCTION_PCT   = 0.60; // 60% damage reduction cap
    private static final double MAX_FALL_REDUCTION  = 0.80;

    private static final long NIGHT_VISION_COOLDOWN_MS = 10_000;

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final SetTracker setTracker;
    private final Random random = new Random();
    private final Map<UUID, Long> nightVisionLastProc = new HashMap<>();

    public ArmorListener(RPGLootPlugin plugin, ItemRarityService rarityService, SetTracker setTracker) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.setTracker = setTracker;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack[] armor = player.getInventory().getArmorContents();

        // Accumulate dodge and reduction across all pieces — roll once to prevent stacking exploits
        double totalDodge     = setTracker.getSetBonus(player, BonusStat.DODGE_CHANCE);
        double totalReduction = setTracker.getSetBonus(player, BonusStat.DAMAGE_REDUCTION);
        double totalFallRed   = 0;
        boolean hasDodge      = totalDodge > 0;
        boolean hasReduction  = totalReduction > 0;
        boolean hasFallRed    = false;

        for (ItemStack piece : armor) {
            if (piece == null) continue;
            List<RolledStat> pieceStats = rarityService.getBonusStats(piece);
            if (pieceStats.isEmpty()) continue; // not an RPG item or no bonus stats
            for (RolledStat rolled : pieceStats) {
                switch (rolled.stat()) {
                    case DODGE_CHANCE      -> { totalDodge     += rolled.value(); hasDodge     = true; }
                    case DAMAGE_REDUCTION  -> { totalReduction += rolled.value(); hasReduction = true; }
                    case FALL_REDUCTION    -> { totalFallRed   += rolled.value(); hasFallRed   = true; }
                    case NIGHT_VISION_CHANCE -> applyNightVision(event, player, rolled.value());
                    default -> {}
                }
            }
        }

        // Single roll for dodge — capped to prevent full immunity
        if (hasDodge) {
            double effectiveDodge = Math.min(totalDodge, MAX_DODGE_PCT);
            if (random.nextDouble() * 100.0 < effectiveDodge) {
                event.setCancelled(true);
                return; // cancelled — no further processing
            }
        }

        // Single application of damage reduction — capped
        if (hasReduction) {
            double reduction = Math.min(totalReduction / 100.0, MAX_REDUCTION_PCT);
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }

        // Fall reduction — capped
        if (hasFallRed && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            double reduction = Math.min(totalFallRed / 100.0, MAX_FALL_REDUCTION);
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }
    }

    /** Thorns: averaged chance across all pieces — single roll per hit. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double totalThornsChance = 0;
        int thornsPieces = 0;

        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
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

    // ── Per-stat helpers ──────────────────────────────────────────────────

    private void applyNightVision(EntityDamageEvent event, Player player, double chancePct) {
        if (player.getHealth() / player.getMaxHealth() >= 0.40) return;
        long now = System.currentTimeMillis();
        if (now - nightVisionLastProc.getOrDefault(player.getUniqueId(), 0L) < NIGHT_VISION_COOLDOWN_MS) return;
        if (random.nextDouble() * 100.0 < chancePct) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, true, false));
            nightVisionLastProc.put(player.getUniqueId(), now);
        }
    }
}
