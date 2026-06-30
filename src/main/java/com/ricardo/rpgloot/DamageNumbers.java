package com.ricardo.rpgloot;

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

    // Track active displays so we can clean up on plugin disable
    private static final Set<UUID> active = ConcurrentHashMap.newKeySet();

    private static final Transformation SCALE_ONLY = new Transformation(
            new Vector3f(0, 0, 0),
            new AxisAngle4f(0, 0, 0, 1),
            new Vector3f(0.7f, 0.7f, 0.7f),
            new AxisAngle4f(0, 0, 0, 1));

    private static final Transformation RISEN = new Transformation(
            new Vector3f(0, 1.2f, 0),
            new AxisAngle4f(0, 0, 0, 1),
            new Vector3f(0.7f, 0.7f, 0.7f),
            new AxisAngle4f(0, 0, 0, 1));

    private DamageNumbers() {}

    public static void show(RPGLootPlugin plugin, Location loc, double amount, Type type) {
        // Skip heal numbers that are too small to be meaningful
        if (type == Type.HEAL && amount < 0.05) return;

        Component text = format(amount, type);
        Location spawnLoc = loc.clone().add(
                (Math.random() - 0.5) * 0.5, 0, (Math.random() - 0.5) * 0.5);

        TextDisplay display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, td -> {
            td.text(text);
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(false);
            td.setDefaultBackground(false);
            td.setTransformation(SCALE_ONLY);
        });

        active.add(display.getUniqueId());

        // Tick 1: activate client-side interpolation toward risen position.
        // The client animates this smoothly — no per-tick server work needed.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) {
                active.remove(display.getUniqueId());
                return;
            }
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(20);
            display.setTransformation(RISEN);
        }, 1L);

        // Remove after animation ends (1 setup tick + 20 animation ticks + 2 buffer)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            display.remove();
            active.remove(display.getUniqueId());
        }, 23L);
    }

    /** Call from RPGLootPlugin.onDisable() to remove any orphaned display entities. */
    public static void cleanup(Server server) {
        for (org.bukkit.World world : server.getWorlds()) {
            world.getEntitiesByClass(TextDisplay.class).stream()
                    .filter(td -> active.contains(td.getUniqueId()))
                    .forEach(org.bukkit.entity.Entity::remove);
        }
        active.clear();
    }

    private static Component format(double amount, Type type) {
        return switch (type) {
            case CRIT  -> Component.text("✦ " + num(amount, false), TextColor.color(255, 215, 0));
            case BLEED -> Component.text(num(amount, false),         TextColor.color(180, 0, 0));
            case HEAL  -> Component.text("♥ +" + num(amount, true),  NamedTextColor.GREEN);
            case SMASH -> Component.text(num(amount, false),         TextColor.color(220, 120, 0));
            default    -> Component.text(num(amount, false),         NamedTextColor.WHITE);
        };
    }

    private static String num(double amount, boolean decimalIfSmall) {
        if (decimalIfSmall && amount < 1.0) {
            return String.format("%.1f", amount);
        }
        return String.valueOf((int) Math.round(amount));
    }
}
