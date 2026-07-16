package com.okereke.rpgloot;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Combat VFX. Particle enum constants are resolved by name so Paper 26 renames
 * (REDSTONE→DUST, EXPLOSION_NORMAL→EXPLOSION, etc.) do not crash class load.
 */
public final class ParticleEffects {

    private ParticleEffects() {}

    public static void crit(Location loc) {
        spawn(loc, 5, 0.2, 0.2, 0.2, 0.05, null, "CRIT", "CRITICAL_HIT");
    }

    public static void bleedTick(LivingEntity target) {
        Location loc = target.getLocation().add(0, target.getHeight() * 0.8, 0);
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f);
        spawn(loc, 3, 0.15, 0.1, 0.15, 0, red, "DUST", "REDSTONE");
    }

    public static void lifesteal(Player player) {
        Location loc = player.getLocation().add(0, 1.2, 0);
        spawn(loc, 3, 0.3, 0.2, 0.3, 0, null, "HEART");
    }

    public static void smashRing(Location center, double radius) {
        int points = 8;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            spawn(point, 2, 0.05, 0.05, 0.05, 0.02, null, "EXPLOSION", "EXPLOSION_NORMAL", "POOF");
        }
    }

    private static void spawn(Location loc, int count, double ox, double oy, double oz, double extra,
                              Particle.DustOptions dust, String... names) {
        if (loc.getWorld() == null) return;
        Particle particle = resolve(names);
        if (particle == null) return;
        if (dust != null) {
            loc.getWorld().spawnParticle(particle, loc, count, ox, oy, oz, extra, dust);
        } else {
            loc.getWorld().spawnParticle(particle, loc, count, ox, oy, oz, extra);
        }
    }

    private static Particle resolve(String... names) {
        for (String name : names) {
            try {
                return Particle.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // try next alias
            }
        }
        return null;
    }
}
