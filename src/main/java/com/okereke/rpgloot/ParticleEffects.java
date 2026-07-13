package com.okereke.rpgloot;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class ParticleEffects {

    private ParticleEffects() {}

    public static void crit(Location loc) {
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2, 0.05);
    }

    public static void bleedTick(LivingEntity target) {
        Location loc = target.getLocation().add(0, target.getHeight() * 0.8, 0);
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f);
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 3, 0.15, 0.1, 0.15, red);
    }

    public static void lifesteal(Player player) {
        Location loc = player.getLocation().add(0, 1.2, 0);
        loc.getWorld().spawnParticle(Particle.HEART, loc, 3, 0.3, 0.2, 0.3, 0);
    }

    public static void smashRing(Location center, double radius) {
        int points = 8;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            center.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, point, 2, 0.05, 0.05, 0.05, 0.02);
        }
    }
}
