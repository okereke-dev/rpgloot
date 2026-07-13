package com.okereke.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageNumbers {

    public enum Type { NORMAL, CRIT, BLEED, HEAL, SMASH }

    private static final Set<UUID> active = ConcurrentHashMap.newKeySet();

    // Normal hit: pop 0.1 → 0.9, settle to 0.7
    private static final float SCALE_START  = 0.1f;
    private static final float SCALE_POP    = 0.9f;
    private static final float SCALE_SETTLE = 0.7f;

    // Crit: bigger pop
    private static final float CRIT_POP    = 1.15f;
    private static final float CRIT_SETTLE = 0.9f;

    private DamageNumbers() {}

    public static void show(RPGLootPlugin plugin, Location loc, double amount, Type type) {
        if (!plugin.getConfig().getBoolean("damage-numbers", true)) return;
        Component text = format(amount, type);

        Location spawnLoc = loc.clone().add(
                (Math.random() - 0.5) * 0.5, 0, (Math.random() - 0.5) * 0.5);

        boolean isCrit = type == Type.CRIT;
        float popScale    = isCrit ? CRIT_POP    : SCALE_POP;
        float settleScale = isCrit ? CRIT_SETTLE : SCALE_SETTLE;

        TextDisplay display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, td -> {
            td.text(text);
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(false);
            td.setDefaultBackground(false);
            td.setTransformation(scale(SCALE_START, 0));
        });

        active.add(display.getUniqueId());

        // Phase 1 (tick 1): pop in fast — small → big
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) { active.remove(display.getUniqueId()); return; }
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(4);
            display.setTransformation(scale(popScale, 0));
        }, 1L);

        // Phase 2 (tick 5): settle to normal size and rise
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) { active.remove(display.getUniqueId()); return; }
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(20);
            display.setTransformation(scale(settleScale, 1.2f));
        }, 5L);

        // Remove after both phases complete
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            display.remove();
            active.remove(display.getUniqueId());
        }, 27L);
    }

    public static void cleanup(Server server) {
        for (org.bukkit.World world : server.getWorlds()) {
            world.getEntitiesByClass(TextDisplay.class).stream()
                    .filter(td -> active.contains(td.getUniqueId()))
                    .forEach(org.bukkit.entity.Entity::remove);
        }
        active.clear();
    }

    private static Transformation scale(float s, float yOffset) {
        return new Transformation(
                new Vector3f(0, yOffset, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(s, s, s),
                new AxisAngle4f(0, 0, 0, 1));
    }

    private static Component format(double amount, Type type) {
        String n = num(amount);
        return switch (type) {
            case CRIT  -> Component.text("✦ " + n, TextColor.color(255, 215, 0));
            case BLEED -> Component.text(n,         TextColor.color(180, 0, 0));
            case HEAL  -> Component.text("♥ +" + n, NamedTextColor.GREEN);
            case SMASH -> Component.text(n,         TextColor.color(220, 120, 0));
            default    -> Component.text(n,         NamedTextColor.WHITE);
        };
    }

    /** Always show 1 decimal — never rounds to 0. */
    private static String num(double amount) {
        if (amount < 10.0) return String.format("%.1f", amount);
        return String.valueOf((int) Math.round(amount));
    }
}
