package com.ricardo.rpgloot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public final class CombatListener implements Listener {

    private final ItemRarityService rarityService;
    private final Random random = new Random();

    public CombatListener(ItemRarityService rarityService) {
        this.rarityService = rarityService;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        List<RolledStat> stats = rarityService.getBonusStats(weapon);
        if (stats.isEmpty()) {
            return;
        }

        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case LIFESTEAL -> {
                    double healAmount = event.getFinalDamage() * (rolled.value() / 100.0);
                    double newHealth = Math.min(player.getHealth() + healAmount,
                            player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                    player.setHealth(newHealth);
                }
                case CRIT_CHANCE -> {
                    if (random.nextDouble() * 100.0 <= rolled.value()) {
                        event.setDamage(event.getDamage() * 1.5);
                    }
                }
                case KNOCKBACK_BOOST -> {
                    Vector direction = target.getLocation().toVector()
                            .subtract(player.getLocation().toVector())
                            .normalize()
                            .multiply(rolled.value() * 0.25);
                    direction.setY(0.3);
                    target.setVelocity(target.getVelocity().add(direction));
                }
                case SWEEP_BONUS -> {
                    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                        event.setDamage(event.getDamage() * (1.0 + rolled.value() / 100.0));
                    }
                }
                case RIPTIDE_SPEED -> {
                    // Boost player velocity when in water or rain
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
                        target.getWorld().getNearbyLivingEntities(target.getLocation(), radius).forEach(nearby -> {
                            if (nearby != player && nearby != target) {
                                nearby.damage(event.getDamage() * 0.5, player);
                                Vector knockback = nearby.getLocation().toVector()
                                        .subtract(target.getLocation().toVector())
                                        .normalize().multiply(0.8).setY(0.4);
                                nearby.setVelocity(knockback);
                            }
                        });
                    }
                }
                case FALL_DAMAGE_BONUS -> {
                    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                            && player.getFallDistance() > 0) {
                        double bonus = event.getDamage() * (rolled.value() / 100.0) * (player.getFallDistance() / 5.0);
                        event.setDamage(event.getDamage() + bonus);
                    }
                }
            }
        }
    }
}
