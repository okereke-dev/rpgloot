package com.ricardo.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemRarityService {

    private final Random random = new Random();
    private final WeaponNameGenerator nameGenerator = new WeaponNameGenerator();

    public boolean isSupportedWeapon(Material material) {
        return WeaponType.isSupported(material);
    }

    public ItemStack applyRarity(ItemStack item, Rarity rarity) {
        ItemMeta meta = item.getItemMeta();

        double damageMultiplier = randomBetween(rarity.getMinDamageMultiplier(), rarity.getMaxDamageMultiplier());
        double speedMultiplier = randomBetween(rarity.getMinSpeedMultiplier(), rarity.getMaxSpeedMultiplier());

        double baseDamage = VanillaStats.baseDamage(item.getType());
        double baseSpeed = VanillaStats.baseSpeed(item.getType());

        double damageBonus = baseDamage * (damageMultiplier - 1.0);
        double speedBonus = baseSpeed * (speedMultiplier - 1.0);

        if (damageBonus > 0) {
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    new NamespacedKey(Keys.RARITY.getNamespace(), "rpgloot_damage"),
                    damageBonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        if (speedBonus > 0) {
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                    new NamespacedKey(Keys.RARITY.getNamespace(), "rpgloot_speed"),
                    speedBonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }

        WeaponType weaponType = WeaponType.of(item.getType());
        List<RolledStat> rolledStats = rollBonusStats(rarity, weaponType);

        String weaponName = nameGenerator.generate(weaponType);

        meta.getPersistentDataContainer().set(Keys.RARITY, PersistentDataType.STRING, rarity.name());
        meta.getPersistentDataContainer().set(Keys.BONUS_STATS, PersistentDataType.STRING, serializeStats(rolledStats));
        meta.getPersistentDataContainer().set(Keys.WEAPON_NAME, PersistentDataType.STRING, weaponName);

        meta.displayName(Component.text(weaponName, rarity.getColor()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(rarity.getDisplayName(), rarity.getColor()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(statLine("Attack Damage", damageMultiplier));
        lore.add(statLine("Attack Speed", speedMultiplier));
        if (!rolledStats.isEmpty()) {
            lore.add(Component.empty());
            for (RolledStat rolled : rolledStats) {
                lore.add(Component.text(rolled.stat().getLabel() + ": +" + (int) Math.round(rolled.value()) + rolled.stat().getUnit(),
                        NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    public Rarity getRarity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(Keys.RARITY, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        return Rarity.valueOf(raw);
    }

    public List<RolledStat> getBonusStats(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return List.of();
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(Keys.BONUS_STATS, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<RolledStat> stats = new ArrayList<>();
        for (String part : raw.split(";")) {
            stats.add(RolledStat.deserialize(part));
        }
        return stats;
    }

    private List<RolledStat> rollBonusStats(Rarity rarity, WeaponType weaponType) {
        List<RolledStat> result = new ArrayList<>();
        List<BonusStat> pool = weaponType != null ? new ArrayList<>(weaponType.getBonusPool()) : new ArrayList<>();
        int count = Math.min(rarity.getBonusStatCount(), pool.size());

        for (int i = 0; i < count; i++) {
            BonusStat chosen = pool.remove(random.nextInt(pool.size()));
            double[] range = chosen.getRangeFor(rarity);
            double value = randomBetween(range[0], range[1]);
            result.add(new RolledStat(chosen, value));
        }
        return result;
    }

    private String serializeStats(List<RolledStat> stats) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stats.size(); i++) {
            if (i > 0) {
                builder.append(";");
            }
            builder.append(stats.get(i).serialize());
        }
        return builder.toString();
    }

    private Component statLine(String label, double multiplier) {
        int percent = Math.round((float) ((multiplier - 1.0) * 100));
        String sign = percent >= 0 ? "+" : "";
        return Component.text(label + ": " + sign + percent + "%", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    private double randomBetween(double min, double max) {
        if (min >= max) {
            return min;
        }
        return min + random.nextDouble() * (max - min);
    }

}
