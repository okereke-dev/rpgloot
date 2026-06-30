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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class CombatListener implements Listener {

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final Random random = new Random();

    // One active bleed per entity — re-hit cancels the old one and starts fresh
    private final Map<UUID, BukkitRunnable> activebleeds = new HashMap<>();

    public CombatListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        List<RolledStat> stats = rarityService.getBonusStats(weapon);
        if (stats.isEmpty()) return;

        // Capture fall distance now — Paper resets it before the event fires for mace hits
        float fallDistance = player.getFallDistance();
        boolean isCrit = false;

        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case LIFESTEAL -> {
                    double healAmount = event.getFinalDamage() * (rolled.value() / 100.0);
                    double newHealth = Math.min(
                            player.getHealth() + healAmount,
                            player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                    player.setHealth(newHealth);
                    ParticleEffects.lifesteal(player);
                    DamageNumbers.show(plugin, player.getLocation().add(0, 2.2, 0), healAmount, DamageNumbers.Type.HEAL);
                }
                case CRIT_CHANCE -> {
                    if (random.nextDouble() * 100.0 <= rolled.value()) {
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
                    // Cancel any existing bleed on this target before starting a new one
                    BukkitRunnable existing = activebleeds.remove(target.getUniqueId());
                    if (existing != null) existing.cancel();

                    double tickDamage = event.getFinalDamage() * (rolled.value() / 100.0) / 3.0;
                    BukkitRunnable bleed = new BukkitRunnable() {
                        int ticks = 3;
                        public void run() {
                            if (!target.isValid() || target.isDead() || ticks-- <= 0) {
                                activebleeds.remove(target.getUniqueId());
                                cancel();
                                return;
                            }
                            target.damage(tickDamage, player);
                            ParticleEffects.bleedTick(target);
                            DamageNumbers.show(plugin,
                                    target.getLocation().add(0, target.getHeight() + 0.3, 0),
                                    tickDamage, DamageNumbers.Type.BLEED);
                        }
                    };
                    activebleeds.put(target.getUniqueId(), bleed);
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
                    if (weapon.getType() == Material.MACE) {
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

        // Main damage number — shown after all modifiers are applied
        Location numLoc = target.getLocation().add(0, target.getHeight() + 0.3, 0);
        DamageNumbers.show(plugin, numLoc, event.getFinalDamage(),
                isCrit ? DamageNumbers.Type.CRIT : DamageNumbers.Type.NORMAL);
        if (isCrit) {
            ParticleEffects.crit(target.getLocation().add(0, target.getHeight() * 0.5, 0));
        }
    }
}
