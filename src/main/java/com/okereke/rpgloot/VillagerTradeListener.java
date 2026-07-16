package com.okereke.rpgloot;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Converts toolsmith / weaponsmith / armorer trade results into RPGLoot gear.
 * Max rarity is always Rare — Hero and Legendary stay drop/chest only.
 * <p>
 * Uses {@link HashSet} (not {@code EnumSet}): on Paper 26+ {@link Villager.Profession}
 * is an interface/{@code OldEnum}, not a Java {@code enum}.
 */
public final class VillagerTradeListener implements Listener {

    private final RPGLootPlugin plugin;
    private final ItemRarityService rarityService;
    private final RarityRoller roller;
    private boolean enabled = true;
    private Rarity maxRarity = Rarity.RARE;
    private Set<Villager.Profession> professions = defaultProfessions();

    public VillagerTradeListener(RPGLootPlugin plugin, ItemRarityService rarityService) {
        this.plugin = plugin;
        this.rarityService = rarityService;
        this.roller = new RarityRoller(plugin.getConfig(), plugin.getLogger());
        reload();
    }

    public void reload() {
        roller.reload(plugin.getConfig());
        enabled = plugin.getConfig().getBoolean("villager-trades.enabled", true);
        String maxName = plugin.getConfig().getString("villager-trades.max-rarity", "RARE");
        try {
            Rarity parsed = Rarity.valueOf(maxName.trim().toUpperCase(Locale.ROOT));
            // Never allow Hero/Legendary from villagers regardless of config typo.
            maxRarity = parsed.ordinal() > Rarity.RARE.ordinal() ? Rarity.RARE : parsed;
        } catch (IllegalArgumentException e) {
            maxRarity = Rarity.RARE;
            plugin.getLogger().warning("Invalid villager-trades.max-rarity '" + maxName + "' — using RARE");
        }

        List<String> listed = plugin.getConfig().getStringList("villager-trades.professions");
        if (listed == null || listed.isEmpty()) {
            professions = defaultProfessions();
            return;
        }
        Set<Villager.Profession> next = new HashSet<>();
        for (String raw : listed) {
            Villager.Profession profession = resolveProfession(raw);
            if (profession != null) {
                next.add(profession);
            } else {
                plugin.getLogger().warning("Unknown villager-trades.professions entry: " + raw);
            }
        }
        professions = next.isEmpty() ? defaultProfessions() : next;
    }

    private static Set<Villager.Profession> defaultProfessions() {
        Set<Villager.Profession> set = new HashSet<>();
        set.add(Villager.Profession.TOOLSMITH);
        set.add(Villager.Profession.WEAPONSMITH);
        set.add(Villager.Profession.ARMORER);
        return set;
    }

    /**
     * Resolves by static Profession constants (works for both enum-era and Paper 26 OldEnum).
     * Avoids {@code Profession.valueOf} / {@code EnumSet}, which break when Profession is not a Java enum.
     */
    private static Villager.Profession resolveProfession(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String key = raw.trim().toUpperCase(Locale.ROOT).replace("MINECRAFT:", "");
        return switch (key) {
            case "TOOLSMITH" -> Villager.Profession.TOOLSMITH;
            case "WEAPONSMITH" -> Villager.Profession.WEAPONSMITH;
            case "ARMORER" -> Villager.Profession.ARMORER;
            case "BUTCHER" -> Villager.Profession.BUTCHER;
            case "CARTOGRAPHER" -> Villager.Profession.CARTOGRAPHER;
            case "CLERIC" -> Villager.Profession.CLERIC;
            case "FARMER" -> Villager.Profession.FARMER;
            case "FISHERMAN" -> Villager.Profession.FISHERMAN;
            case "FLETCHER" -> Villager.Profession.FLETCHER;
            case "LEATHERWORKER" -> Villager.Profession.LEATHERWORKER;
            case "LIBRARIAN" -> Villager.Profession.LIBRARIAN;
            case "MASON", "STONE_MASON" -> Villager.Profession.MASON;
            case "NITWIT" -> Villager.Profession.NITWIT;
            case "SHEPHERD" -> Villager.Profession.SHEPHERD;
            case "NONE" -> Villager.Profession.NONE;
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!enabled) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!professions.contains(villager.getProfession())) return;

        MerchantRecipe converted = convertRecipe(event.getRecipe());
        if (converted != null) {
            event.setRecipe(converted);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerchantOpen(InventoryOpenEvent event) {
        if (!enabled) return;
        if (!(event.getInventory() instanceof MerchantInventory merchantInv)) return;
        Merchant merchant = merchantInv.getMerchant();
        if (!(merchant instanceof AbstractVillager abstractVillager)) return;
        if (!(abstractVillager instanceof Villager villager)) return;
        if (!professions.contains(villager.getProfession())) return;

        List<MerchantRecipe> recipes = merchant.getRecipes();
        if (recipes.isEmpty()) return;

        boolean changed = false;
        List<MerchantRecipe> next = new ArrayList<>(recipes.size());
        for (MerchantRecipe recipe : recipes) {
            MerchantRecipe converted = convertRecipe(recipe);
            if (converted != null) {
                next.add(converted);
                changed = true;
            } else {
                next.add(recipe);
            }
        }
        if (changed) {
            merchant.setRecipes(next);
        }
    }

    /**
     * @return a new recipe with RPGLoot result, or null if no conversion needed
     */
    private MerchantRecipe convertRecipe(MerchantRecipe recipe) {
        if (recipe == null) return null;
        ItemStack result = recipe.getResult();
        ItemStack converted = LootConvert.convertOne(result, maxRarity, rarityService, roller);
        if (converted == null) return null;

        MerchantRecipe next = new MerchantRecipe(
                converted,
                recipe.getUses(),
                recipe.getMaxUses(),
                recipe.hasExperienceReward(),
                recipe.getVillagerExperience(),
                recipe.getPriceMultiplier(),
                recipe.getDemand(),
                recipe.getSpecialPrice()
        );
        next.setIngredients(recipe.getIngredients());
        return next;
    }
}
