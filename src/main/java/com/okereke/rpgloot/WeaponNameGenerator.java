package com.okereke.rpgloot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WeaponNameGenerator {

    private static final List<String> SHARED_ADJECTIVES = List.of(
            "Ancient", "Cursed", "Runic", "Hollow", "Forsaken",
            "Blazing", "Shadow", "Void", "Sacred", "Wicked");

    private static final Map<WeaponType, List<String>> ADJECTIVES = new HashMap<>();
    private static final Map<WeaponType, List<String>> NOUNS = new HashMap<>();

    static {
        // Weapons
        ADJECTIVES.put(WeaponType.SWORD,     List.of("Obsidian", "Crimson", "Ghostly", "Ashen", "Venomous"));
        ADJECTIVES.put(WeaponType.AXE,       List.of("Savage", "Brutal", "Darkened", "Iron", "Cleaving"));
        ADJECTIVES.put(WeaponType.TRIDENT,   List.of("Tidal", "Storm", "Abyssal", "Tempest", "Drowned"));
        ADJECTIVES.put(WeaponType.MACE,      List.of("Heavy", "Divine", "Shattered", "Bone", "Crushing"));
        ADJECTIVES.put(WeaponType.BOW,       List.of("Silent", "Swift", "Phantom", "Twisted", "Elven"));
        ADJECTIVES.put(WeaponType.CROSSBOW,  List.of("Rapid", "Runed", "Cracked", "Iron", "Enchanted"));
        // Armor
        ADJECTIVES.put(WeaponType.HELMET,     List.of("Reinforced", "Shielded", "Warlord's", "Infernal", "Warden's"));
        ADJECTIVES.put(WeaponType.CHESTPLATE, List.of("Tempered", "Bulwark", "Guardian's", "Indestructible", "Bastion"));
        ADJECTIVES.put(WeaponType.LEGGINGS,   List.of("Strider's", "Phantom", "Iron-Clad", "Swift", "Shadow"));
        ADJECTIVES.put(WeaponType.BOOTS,      List.of("Drifter's", "Stalker's", "Wandering", "Hollow", "Ghost"));
        // Tools
        ADJECTIVES.put(WeaponType.AXE_TOOL,   List.of("Lumbering", "Seasoned", "Forester's", "Rugged", "Veteran"));
        ADJECTIVES.put(WeaponType.PICKAXE,     List.of("Deep", "Seasoned", "Miner's", "Rugged", "Veteran"));
        ADJECTIVES.put(WeaponType.SHOVEL,      List.of("Earthen", "Delver's", "Gravedigger's", "Seasoned", "Sturdy"));
        ADJECTIVES.put(WeaponType.HOE,         List.of("Farmer's", "Fertile", "Verdant", "Ancient", "Seasoned"));
        ADJECTIVES.put(WeaponType.FISHING_ROD, List.of("Lucky", "Patient", "Deep-Sea", "Phantom", "Ancient"));

        // Weapons
        NOUNS.put(WeaponType.SWORD,     List.of("Blade", "Edge", "Saber", "Fang", "Cleaver", "Reaper", "Cutter"));
        NOUNS.put(WeaponType.AXE,       List.of("Splitter", "Crusher", "Hewer", "Breaker", "Carver", "Cleaver"));
        NOUNS.put(WeaponType.TRIDENT,   List.of("Spear", "Tide", "Piercer", "Lance", "Fork", "Prong"));
        NOUNS.put(WeaponType.MACE,      List.of("Crusher", "Hammer", "Basher", "Grinder", "Smiter", "Pounder"));
        NOUNS.put(WeaponType.BOW,       List.of("Longbow", "Arc", "Whisper", "Stringer", "Recurve", "Shortbow"));
        NOUNS.put(WeaponType.CROSSBOW,  List.of("Bolt", "Repeater", "Striker", "Launcher", "Buster", "Caster"));
        // Armor
        NOUNS.put(WeaponType.HELMET,     List.of("Helm", "Crown", "Visor", "Cap", "Mask", "Hood"));
        NOUNS.put(WeaponType.CHESTPLATE, List.of("Aegis", "Plate", "Cuirass", "Breastplate", "Hauberk", "Shell"));
        NOUNS.put(WeaponType.LEGGINGS,   List.of("Greaves", "Legs", "Tassets", "Plates", "Guards", "Cuisses"));
        NOUNS.put(WeaponType.BOOTS,      List.of("Treads", "Sabatons", "Boots", "Stompers", "Walkers", "Steps"));
        // Tools
        NOUNS.put(WeaponType.AXE_TOOL,   List.of("Hatchet", "Woodcutter", "Feller", "Chopper", "Cutter"));
        NOUNS.put(WeaponType.PICKAXE,     List.of("Drill", "Excavator", "Breaker", "Borer", "Delver"));
        NOUNS.put(WeaponType.SHOVEL,      List.of("Digger", "Spade", "Trowel", "Excavator", "Burrower"));
        NOUNS.put(WeaponType.HOE,         List.of("Harvester", "Tiller", "Cultivator", "Rake", "Ploughshare"));
        NOUNS.put(WeaponType.FISHING_ROD, List.of("Line", "Lure", "Rod", "Caster", "Reel"));
    }

    private final Random random = new Random();

    public String generate(WeaponType type) {
        List<String> typeAdj = ADJECTIVES.getOrDefault(type, List.of());
        List<String> allAdj = new ArrayList<>(SHARED_ADJECTIVES);
        allAdj.addAll(typeAdj);

        List<String> nouns = NOUNS.getOrDefault(type, List.of("Item"));

        String adjective = allAdj.get(random.nextInt(allAdj.size()));
        String noun = nouns.get(random.nextInt(nouns.size()));
        return adjective + " " + noun;
    }
}
