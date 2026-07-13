package com.okereke.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * Broadcasts a server-wide message (+ sound/firework) when a player finds a rare
 * RPGLoot item from a mob or boss kill. Structure loot is never announced since
 * there's no "finder" at chest-generation time.
 */
public final class DropAnnouncer {

    private final RPGLootPlugin plugin;

    public DropAnnouncer(RPGLootPlugin plugin) {
        this.plugin = plugin;
    }

    public void announce(Player finder, ItemStack item, Rarity rarity) {
        if (!plugin.getConfig().getBoolean("broadcast.enabled", true)) return;
        if (rarity.ordinal() < parseMinRarity().ordinal()) return;

        Component itemName = item.getItemMeta() != null ? item.getItemMeta().displayName() : null;
        if (itemName == null) itemName = Component.text(item.getType().name());

        Component message = Component.text(finder.getName() + " found a ", NamedTextColor.GRAY)
                .append(Component.text(rarity.getDisplayName() + " ", rarity.getColor()))
                .append(itemName)
                .append(Component.text("!", NamedTextColor.GRAY));

        plugin.getServer().broadcast(message);

        if (plugin.getConfig().getBoolean("broadcast.sound", true)) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        if (plugin.getConfig().getBoolean("broadcast.firework", true)) {
            spawnFirework(finder.getLocation(), rarity.getColor());
        }
    }

    private Rarity parseMinRarity() {
        String raw = plugin.getConfig().getString("broadcast.min-rarity", "HERO");
        try {
            return Rarity.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid broadcast.min-rarity '" + raw + "' — defaulting to HERO.");
            return Rarity.HERO;
        }
    }

    private void spawnFirework(Location loc, TextColor color) {
        Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
                .with(FireworkEffect.Type.BURST)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }
}
