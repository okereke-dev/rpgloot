package com.ricardo.rpgloot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatListener implements Listener {

    // Hard caps to prevent item + set stat stacking from becoming overpowered
    private static final double MAX_CRIT_PCT    = 35.0;
    private static final double MAX_BLEED_PCT   = 60.0;

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final SetTracker setTracker;
    private final Random random = new Random();

    // One active bleed runnable per target entity
    private final Map<UUID, BukkitRunnable> activeBleeds = new HashMap<>();

    // Entities currently receiving a bleed tick — skip these in the event handler
    // to prevent bleed damage from re-triggering stats
    private static final Set<UUID> bleedTargets = ConcurrentHashMap.newKeySet();

    public CombatListener(RPGLootPlugin plugin, ItemRarityService rarityService, SetTracker setTracker) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.setTracker = setTracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Ignore damage that originates from a bleed tick
        if (bleedTargets.contains(target.getUniqueId())) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        List<RolledStat> stats = rarityService.getBonusStats(weapon);
        if (stats.isEmpty()) return;

        float fallDistance = player.getFallDistance();
        boolean isCrit = false;

        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case LIFESTEAL -> {
                    double lifestealPct = rolled.value() + setTracker.getSetBonus(player, BonusStat.LIFESTEAL);
                    double healAmount = event.getFinalDamage() * (lifestealPct / 100.0);
                    double newHealth = Math.min(
                            player.getHealth() + healAmount,
                            player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                    player.setHealth(newHealth);
                    ParticleEffects.lifesteal(player);
                    DamageNumbers.show(plugin, player.getLocation().add(0, 2.2, 0), healAmount, DamageNumbers.Type.HEAL);
                }
                case CRIT_CHANCE -> {
                    double critPct = Math.min(
                            rolled.value() + setTracker.getSetBonus(player, BonusStat.CRIT_CHANCE), MAX_CRIT_PCT);
                    if (random.nextDouble() * 100.0 <= critPct) {
                        event.setDamage(event.getDamage() * 1.5);
                        isCrit = true;
                    }
                }
                case KNOCKBACK_BOOST -> {
                    Vector dir = target.getLocation().toVector()
                            .subtract(player.getLocation().toVector())
                            .normalize()
                            .multiply(rolled.value() * 0.25);
                    dir.setY(0.3);
                    target.setVelocity(target.getVelocity().add(dir));
                }
                case BLEEDING -> {
                    double bleedPct = Math.min(
                            rolled.value() + setTracker.getSetBonus(player, BonusStat.BLEEDING), MAX_BLEED_PCT);
                    if (random.nextDouble() * 100.0 > bleedPct) break;

                    // Cancel any existing bleed on this target
                    BukkitRunnable existing = activeBleeds.remove(target.getUniqueId());
                    if (existing != null) existing.cancel();

                    // Damage per tick = weapon base damage × rarity factor (predictable, not hit-dependent)
                    Rarity rarity = rarityService.getRarity(weapon);
                    double baseDmg = VanillaStats.baseDamage(weapon.getType());
                    double tickDamage = baseDmg * bleedTickFactor(rarity);
                    int duration = bleedDuration(rarity);

                    BukkitRunnable bleed = new BukkitRunnable() {
                        int remaining = duration;

                        public void run() {
                            if (!target.isValid() || target.isDead() || remaining-- <= 0) {
                                target.removePotionEffect(PotionEffectType.SLOW);
                                activeBleeds.remove(target.getUniqueId());
                                cancel();
                                return;
                            }
                            // Save velocity — target.damage() applies knockback, we don't want that on bleed ticks
                            org.bukkit.util.Vector savedVelocity = target.getVelocity().clone();
                            bleedTargets.add(target.getUniqueId());
                            // If attacker disconnected, apply damage without attribution to avoid Player reference issues
                            if (player.isOnline()) {
                                target.damage(tickDamage, player);
                            } else {
                                target.damage(tickDamage);
                            }
                            bleedTargets.remove(target.getUniqueId());
                            target.setVelocity(savedVelocity);

                            // Reapply Slowness I so it stays active exactly as long as the bleed
                            target.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOW, 25, 0, true, false, false));

                            ParticleEffects.bleedTick(target);
                            DamageNumbers.show(plugin,
                                    target.getLocation().add(0, target.getHeight() + 0.3, 0),
                                    tickDamage, DamageNumbers.Type.BLEED);
                        }
                    };
                    activeBleeds.put(target.getUniqueId(), bleed);
                    bleed.runTaskTimer(plugin, 20L, 20L);
                }
                case RIPTIDE_SPEED -> {
                    if (player.isInWater() || player.isInRain()) {
                        Vector boost = player.getLocation().getDirection().multiply(rolled.value() / 100.0);
                        player.setVelocity(player.getVelocity().add(boost));
                    }
                }
                case LIGHTNING_CHANCE -> {
                    if (random.nextDouble() * 100.0 <= rolled.value()) {
                        Location loc = target.getLocation();
                        loc.getWorld().strikeLightningEffect(loc);
                        target.damage(4.0, player);
                    }
                }
                case SMASH_RADIUS -> {
                    Material maceType = Material.getMaterial("MACE");
                    if (maceType != null && weapon.getType() == maceType) {
                        double radius = rolled.value();
                        target.getWorld().getNearbyLivingEntities(target.getLocation(), radius)
                                .forEach(nearby -> {
                                    if (nearby == player || nearby == target) return;
                                    double splashDmg = event.getDamage() * 0.5;
                                    nearby.damage(splashDmg, player);
                                    Vector kb = nearby.getLocation().toVector()
                                            .subtract(target.getLocation().toVector())
                                            .normalize().multiply(0.8).setY(0.4);
                                    nearby.setVelocity(kb);
                                    DamageNumbers.show(plugin,
                                            nearby.getLocation().add(0, nearby.getHeight() + 0.3, 0),
                                            splashDmg, DamageNumbers.Type.SMASH);
                                });
                        ParticleEffects.smashRing(target.getLocation(), radius);
                    }
                }
                case FALL_DAMAGE_BONUS -> {
                    if (fallDistance > 0) {
                        double bonus = event.getDamage() * (rolled.value() / 100.0) * (fallDistance / 5.0);
                        event.setDamage(event.getDamage() + bonus);
                    }
                }
            }
        }

        Location numLoc = target.getLocation().add(0, target.getHeight() + 0.3, 0);
        DamageNumbers.show(plugin, numLoc, event.getFinalDamage(),
                isCrit ? DamageNumbers.Type.CRIT : DamageNumbers.Type.NORMAL);
        if (isCrit) {
            ParticleEffects.crit(target.getLocation().add(0, target.getHeight() * 0.5, 0));
        }
    }

    /** Random bleed duration in ticks (1 tick = 1 second), scaled by rarity. */
    private int bleedDuration(Rarity rarity) {
        if (rarity == null) return 2 + random.nextInt(2);
        return switch (rarity) {
            case UNCOMMON  -> 2 + random.nextInt(2); // 2–3s
            case RARE      -> 2 + random.nextInt(3); // 2–4s
            case HERO      -> 3 + random.nextInt(3); // 3–5s
            case LEGENDARY -> 4 + random.nextInt(3); // 4–6s
            default        -> 2;
        };
    }

    /** Bleed damage per tick as a fraction of weapon base damage, scaled by rarity. */
    private double bleedTickFactor(Rarity rarity) {
        if (rarity == null) return 0.15;
        return switch (rarity) {
            case UNCOMMON  -> 0.15; // 15% of base damage/tick
            case RARE      -> 0.22; // 22%
            case HERO      -> 0.30; // 30%
            case LEGENDARY -> 0.40; // 40%
            default        -> 0.15;
        };
    }
}
