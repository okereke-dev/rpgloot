package com.ricardo.rpgloot;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public final class ProjectileListener implements Listener {

    private final ItemRarityService rarityService;
    private final Random random = new Random();

    public ProjectileListener(ItemRarityService rarityService) {
        this.rarityService = rarityService;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }

        ItemStack bow = event.getBow();
        List<RolledStat> stats = rarityService.getBonusStats(bow);
        if (stats.isEmpty()) {
            return;
        }

        // Store bonus stats on the arrow so we can read them on hit
        arrow.getPersistentDataContainer().set(Keys.BONUS_STATS, PersistentDataType.STRING,
                serializeStats(stats));

        for (RolledStat rolled : stats) {
            switch (rolled.stat()) {
                case FLAME_CHANCE -> {
                    if (random.nextDouble() * 100.0 <= rolled.value()) {
                        arrow.setFireTicks(200);
                    }
                }
                case ARROW_PUNCH -> {
                    arrow.setKnockbackStrength((int) Math.round(rolled.value()));
                }
                case MULTISHOT_CHANCE -> {
                    if (random.nextDouble() * 100.0 <= rolled.value()) {
                        // Fire 2 extra arrows spread slightly
                        for (int i = 0; i < 2; i++) {
                            AbstractArrow extra = arrow.getWorld().spawnArrow(
                                    arrow.getLocation(),
                                    arrow.getVelocity().clone()
                                            .add(new org.bukkit.util.Vector(
                                                    (random.nextDouble() - 0.5) * 0.2,
                                                    (random.nextDouble() - 0.5) * 0.05,
                                                    (random.nextDouble() - 0.5) * 0.2)),
                                    (float) arrow.getVelocity().length(),
                                    1.0f,
                                    org.bukkit.entity.Arrow.class);
                            extra.setShooter(arrow.getShooter());
                            extra.setDamage(arrow.getDamage());
                        }
                    }
                }
                case CHARGE_SPEED -> {
                    // Boost arrow velocity to simulate faster charge
                    double boost = 1.0 + rolled.value() / 100.0;
                    arrow.setVelocity(arrow.getVelocity().multiply(boost));
                }
                default -> {
                }
            }
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) {
            return;
        }

        String raw = arrow.getPersistentDataContainer().get(Keys.BONUS_STATS, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return;
        }

        for (String part : raw.split(";")) {
            RolledStat rolled = RolledStat.deserialize(part);
            if (rolled.stat() == BonusStat.ARROW_DAMAGE) {
                event.setDamage(event.getDamage() * (1.0 + rolled.value() / 100.0));
            } else if (rolled.stat() == BonusStat.PIERCING_CHANCE) {
                if (random.nextDouble() * 100.0 <= rolled.value()) {
                    arrow.setPierceLevel(arrow.getPierceLevel() + 1);
                }
            }
        }
    }

    private String serializeStats(List<RolledStat> stats) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stats.size(); i++) {
            if (i > 0) builder.append(";");
            builder.append(stats.get(i).serialize());
        }
        return builder.toString();
    }
}
