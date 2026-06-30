package com.ricardo.rpgloot;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WeaponNameGenerator {

    private static final List<String> SHARED_ADJECTIVES = List.of(
            "Ancient", "Cursed", "Runic", "Hollow", "Forsaken",
            "Blazing", "Shadow", "Void", "Sacred", "Wicked");

    private static final Map<WeaponType, List<String>> ADJECTIVES = Map.of(
            WeaponType.SWORD, List.of("Obsidian", "Crimson", "Ghostly", "Ashen", "Venomous"),
            WeaponType.AXE, List.of("Savage", "Brutal", "Darkened", "Iron", "Cleaving"),
            WeaponType.TRIDENT, List.of("Tidal", "Storm", "Abyssal", "Tempest", "Drowned"),
            WeaponType.MACE, List.of("Heavy", "Divine", "Shattered", "Bone", "Crushing"),
            WeaponType.BOW, List.of("Silent", "Swift", "Phantom", "Twisted", "Elven"),
            WeaponType.CROSSBOW, List.of("Rapid", "Runed", "Cracked", "Iron", "Enchanted")
    );

    private static final Map<WeaponType, List<String>> NOUNS = Map.of(
            WeaponType.SWORD, List.of("Blade", "Edge", "Saber", "Fang", "Cleaver", "Reaper", "Cutter"),
            WeaponType.AXE, List.of("Splitter", "Crusher", "Hewer", "Breaker", "Carver", "Cleaver"),
            WeaponType.TRIDENT, List.of("Spear", "Tide", "Piercer", "Lance", "Fork", "Prong"),
            WeaponType.MACE, List.of("Crusher", "Hammer", "Basher", "Grinder", "Smiter", "Pounder"),
            WeaponType.BOW, List.of("Longbow", "Arc", "Whisper", "Stringer", "Recurve", "Shortbow"),
            WeaponType.CROSSBOW, List.of("Bolt", "Repeater", "Striker", "Launcher", "Buster", "Caster")
    );

    private final Random random = new Random();

    public String generate(WeaponType weaponType) {
        List<String> typeAdjectives = ADJECTIVES.getOrDefault(weaponType, List.of());
        List<String> allAdjectives = new java.util.ArrayList<>(SHARED_ADJECTIVES);
        allAdjectives.addAll(typeAdjectives);

        List<String> nouns = NOUNS.getOrDefault(weaponType, List.of("Weapon"));

        String adjective = allAdjectives.get(random.nextInt(allAdjectives.size()));
        String noun = nouns.get(random.nextInt(nouns.size()));

        return adjective + " " + noun;
    }
}
