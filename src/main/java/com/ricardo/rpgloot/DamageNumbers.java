package com.ricardo.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class DamageNumbers {

    public enum Type {
        NORMAL, CRIT, BLEED, HEAL, SMASH
    }

    private DamageNumbers() {}

    public static void show(RPGLootPlugin plugin, Location loc, double amount, Type type) {
        Component text = format(amount, type);
        Location spawnLoc = loc.clone().add(
                (Math.random() - 0.5) * 0.4,
                0,
                (Math.random() - 0.5) * 0.4);

        TextDisplay display = loc.getWorld().spawn(spawnLoc, TextDisplay.class, td -> {
            td.text(text);
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(false);
            td.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new AxisAngle4f(0, 0, 0, 1)));
        });

        // Rise and despawn over ~1.2 seconds (24 ticks)
        new BukkitRunnable() {
            int tick = 0;
            final int total = 24;

            @Override
            public void run() {
                if (!display.isValid() || tick >= total) {
                    display.remove();
                    cancel();
                    return;
                }
                display.teleport(display.getLocation().add(0, 0.04, 0));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static Component format(double amount, Type type) {
        int rounded = (int) Math.round(amount);
        return switch (type) {
            case CRIT  -> Component.text("✦ " + rounded, TextColor.color(255, 215, 0));
            case BLEED -> Component.text(rounded, TextColor.color(180, 0, 0));
            case HEAL  -> Component.text("♥ +" + rounded, NamedTextColor.GREEN);
            case SMASH -> Component.text(rounded, TextColor.color(220, 120, 0));
            default    -> Component.text(rounded, NamedTextColor.WHITE);
        };
    }
}
