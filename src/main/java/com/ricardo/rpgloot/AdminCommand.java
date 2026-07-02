package com.ricardo.rpgloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public final class AdminCommand implements CommandExecutor, TabCompleter {

    // ── Item type lookup ──────────────────────────────────────────────────

    private static final Map<String, WeaponType> TYPE_MAP = new LinkedHashMap<>();
    static {
        TYPE_MAP.put("sword",       WeaponType.SWORD);
        TYPE_MAP.put("axe",         WeaponType.AXE);
        TYPE_MAP.put("axe_tool",    WeaponType.AXE_TOOL);
        TYPE_MAP.put("trident",     WeaponType.TRIDENT);
        TYPE_MAP.put("mace",        WeaponType.MACE);
        TYPE_MAP.put("bow",         WeaponType.BOW);
        TYPE_MAP.put("crossbow",    WeaponType.CROSSBOW);
        TYPE_MAP.put("helmet",      WeaponType.HELMET);
        TYPE_MAP.put("chestplate",  WeaponType.CHESTPLATE);
        TYPE_MAP.put("leggings",    WeaponType.LEGGINGS);
        TYPE_MAP.put("boots",       WeaponType.BOOTS);
        TYPE_MAP.put("pickaxe",     WeaponType.PICKAXE);
        TYPE_MAP.put("shovel",      WeaponType.SHOVEL);
        TYPE_MAP.put("hoe",         WeaponType.HOE);
        TYPE_MAP.put("fishing_rod", WeaponType.FISHING_ROD);
    }

    private static final List<Material> ALL_POOL;
    static {
        ALL_POOL = new ArrayList<>();
        ALL_POOL.addAll(ItemPools.WEAPONS);
        ALL_POOL.addAll(ItemPools.ARMOR);
        ALL_POOL.addAll(ItemPools.TOOLS);
    }

    // ── Set testing: material tier → ordered list of pieces (sword first for mainhand) ──

    private static final Map<String, List<Material>> TIER_SET_ITEMS = new LinkedHashMap<>();
    static {
        TIER_SET_ITEMS.put("netherite", List.of(
                Material.NETHERITE_SWORD, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS));
        TIER_SET_ITEMS.put("diamond", List.of(
                Material.DIAMOND_SWORD, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS));
        TIER_SET_ITEMS.put("iron", List.of(
                Material.IRON_SWORD, Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                Material.IRON_LEGGINGS, Material.IRON_BOOTS));
        TIER_SET_ITEMS.put("golden", List.of(
                Material.GOLDEN_SWORD, Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
                Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS));
        TIER_SET_ITEMS.put("chainmail", List.of(
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS));
        TIER_SET_ITEMS.put("leather", List.of(
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS));
    }

    private static final List<String> SET_NAMES = Arrays.stream(SetBonus.values())
            .map(s -> s.getDisplayName().toLowerCase())
            .toList();

    // ── Fields ────────────────────────────────────────────────────────────

    private final Plugin plugin;
    private final ItemRarityService rarityService;
    private final SetTracker setTracker;
    private final Random random = new Random();

    public AdminCommand(Plugin plugin, ItemRarityService rarityService, SetTracker setTracker) {
        this.plugin        = plugin;
        this.rarityService = rarityService;
        this.setTracker    = setTracker;
    }

    // ── Command dispatch ──────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            // /rpgloot get [rarity] [type] [material] [set]
            case "get" -> {
                if (!sender.hasPermission("rpgloot.command.get")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                Rarity rarity    = parseRarity(args.length > 1 ? args[1] : null);
                String typeArg   = args.length > 2 ? args[2].toLowerCase() : null;
                String matArg    = args.length > 3 ? args[3].toUpperCase() : null;
                String setArg    = args.length > 4 ? args[4] : null;

                WeaponType type  = typeArg != null ? TYPE_MAP.get(typeArg) : null;
                Material material = matArg != null ? parseMat(matArg) : null;
                if (material == null) material = randomMaterialFor(type);
                if (material == null) material = ALL_POOL.get(random.nextInt(ALL_POOL.size()));
                if (type == null)     type = WeaponType.of(material);
                if (type == null) { sender.sendMessage(err("Unsupported material.")); return true; }

                SetBonus forcedSet = null;
                if (setArg != null) {
                    forcedSet = SetBonus.fromName(setArg);
                    if (forcedSet == null) {
                        sender.sendMessage(err("Unknown set: \"" + setArg + "\". Use /rpgloot sets to see available sets."));
                        return true;
                    }
                    if (!type.isWeapon() && !type.isArmor()) {
                        sender.sendMessage(err("Tools cannot belong to sets."));
                        return true;
                    }
                }

                ItemStack item = new ItemStack(material);
                rarityService.applyRarity(item, rarity, type, forcedSet);
                player.getInventory().addItem(item);
                sender.sendMessage(Component.text("Generated: ", NamedTextColor.GRAY)
                        .append(Component.text(rarity.getDisplayName() + " " + material.name()
                                + (forcedSet != null ? " [" + forcedSet.getDisplayName() + "]" : ""),
                                rarity.getColor()).decoration(TextDecoration.ITALIC, false)));
            }

            // /rpgloot getall [type]
            case "getall" -> {
                if (!sender.hasPermission("rpgloot.command.getall")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                String typeArg       = args.length > 1 ? args[1].toLowerCase() : null;
                WeaponType fixedType = typeArg != null ? TYPE_MAP.get(typeArg) : null;

                for (Rarity rarity : Rarity.values()) {
                    WeaponType type  = fixedType;
                    Material material = randomMaterialFor(type);
                    if (material == null) material = ALL_POOL.get(random.nextInt(ALL_POOL.size()));
                    if (type == null)     type = WeaponType.of(material);
                    if (type == null) continue;
                    ItemStack item = new ItemStack(material);
                    rarityService.applyRarity(item, rarity, type);
                    player.getInventory().addItem(item);
                }
                sender.sendMessage(Component.text("Generated one item per rarity.", NamedTextColor.GREEN));
            }

            // /rpgloot getset <set> <rarity> <tier> [pieces]
            case "getset" -> {
                if (!sender.hasPermission("rpgloot.command.getset")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }
                if (args.length < 4) {
                    sender.sendMessage(err("Usage: /rpgloot getset <set> <rarity> <tier> [pieces 2-5]"));
                    return true;
                }

                SetBonus setBonus = SetBonus.fromName(args[1]);
                if (setBonus == null) {
                    sender.sendMessage(err("Unknown set: \"" + args[1] + "\". Use /rpgloot sets to see available sets."));
                    return true;
                }

                Rarity rarity = parseRarityStrict(args[2]);
                if (rarity == null) {
                    sender.sendMessage(err("Unknown rarity: \"" + args[2] + "\". Valid: common uncommon rare hero legendary"));
                    return true;
                }

                String tierArg = args[3].toLowerCase();
                List<Material> tierItems = TIER_SET_ITEMS.get(tierArg);
                if (tierItems == null) {
                    sender.sendMessage(err("Unknown tier: \"" + tierArg + "\". Valid: " + String.join(" ", TIER_SET_ITEMS.keySet())));
                    return true;
                }

                int maxPieces = tierItems.size();
                int pieces = maxPieces; // default: full set
                if (args.length > 4) {
                    try { pieces = Math.min(Math.max(Integer.parseInt(args[4]), 2), maxPieces); }
                    catch (NumberFormatException e) {
                        sender.sendMessage(err("Pieces must be a number between 2 and " + maxPieces + "."));
                        return true;
                    }
                }

                for (int i = 0; i < pieces; i++) {
                    Material mat = tierItems.get(i);
                    WeaponType type = WeaponType.of(mat);
                    if (type == null) continue;
                    ItemStack item = new ItemStack(mat);
                    rarityService.applyRarity(item, rarity, type, setBonus);
                    player.getInventory().addItem(item);
                }
                sender.sendMessage(Component.text("Generated ", NamedTextColor.GRAY)
                        .append(Component.text(pieces + "× " + rarity.getDisplayName()
                                + " " + setBonus.getDisplayName() + " (" + tierArg + ")", rarity.getColor())
                                .decoration(TextDecoration.ITALIC, false)));
            }

            // /rpgloot sets [set]
            case "sets" -> {
                if (!sender.hasPermission("rpgloot.command.sets")) { noPermission(sender); return true; }

                if (args.length > 1) {
                    // Detail view for one set
                    SetBonus sb = SetBonus.fromName(args[1]);
                    if (sb == null) {
                        sender.sendMessage(err("Unknown set: \"" + args[1] + "\"."));
                        return true;
                    }
                    sender.sendMessage(Component.text("─── ◈ " + sb.getDisplayName()
                            + " — " + sb.getBonusStat().getLabel() + " ───", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text(
                            String.format("  %-10s %6s %6s %6s %6s", "Rarity", "2pcs", "3pcs", "4pcs", "5pcs"),
                            NamedTextColor.GRAY));
                    for (Rarity r : Rarity.values()) {
                        String line = String.format("  %-10s %6s %6s %6s %6s",
                                r.getDisplayName(),
                                fmtVal(sb.getValueForPieces(r, 2)) + sb.getBonusStat().getUnit(),
                                fmtVal(sb.getValueForPieces(r, 3)) + sb.getBonusStat().getUnit(),
                                fmtVal(sb.getValueForPieces(r, 4)) + sb.getBonusStat().getUnit(),
                                fmtVal(sb.getValueForPieces(r, 5)) + sb.getBonusStat().getUnit());
                        sender.sendMessage(Component.text(line, r.getColor())
                                .decoration(TextDecoration.ITALIC, false));
                    }
                } else {
                    // Overview: all sets
                    sender.sendMessage(Component.text("─── Available Sets ───", NamedTextColor.GOLD));
                    for (SetBonus sb : SetBonus.values()) {
                        sender.sendMessage(Component.text("  ◈ ", NamedTextColor.YELLOW)
                                .append(Component.text(sb.getDisplayName(), NamedTextColor.WHITE)
                                        .decoration(TextDecoration.ITALIC, false))
                                .append(Component.text("  →  " + sb.getBonusStat().getLabel(),
                                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                    }
                    sender.sendMessage(Component.text(
                            "  Use /rpgloot sets <name> for the full value table.", NamedTextColor.DARK_GRAY));
                }
            }

            // /rpgloot stats
            case "stats" -> {
                if (!sender.hasPermission("rpgloot.command.stats")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                ItemStack held = player.getInventory().getItemInMainHand();
                Rarity rarity = rarityService.getRarity(held);
                if (rarity == null) {
                    sender.sendMessage(err("This item has no rarity data."));
                    return true;
                }

                sender.sendMessage(Component.text("─── Item Stats ───", NamedTextColor.GOLD));
                sender.sendMessage(label("Rarity").append(Component.text(rarity.getDisplayName(), rarity.getColor())));

                List<RolledStat> stats = rarityService.getBonusStats(held);
                if (stats.isEmpty()) {
                    sender.sendMessage(Component.text("  No bonus stats.", NamedTextColor.DARK_GRAY));
                } else {
                    for (RolledStat rolled : stats) {
                        sender.sendMessage(Component.text(
                                "  " + rolled.stat().getLabel() + ": +" + fmtVal(rolled.value()) + rolled.stat().getUnit(),
                                NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                    }
                }

                if (held.hasItemMeta()) {
                    String setName = held.getItemMeta().getPersistentDataContainer()
                            .get(Keys.SET_NAME, org.bukkit.persistence.PersistentDataType.STRING);
                    if (setName != null) {
                        SetBonus setBonus = SetBonus.fromName(setName);
                        sender.sendMessage(label("Set").append(
                                Component.text("◈ " + setName, rarity.getColor())
                                        .decoration(TextDecoration.ITALIC, false)));
                        sender.sendMessage(label("Tier").append(
                                Component.text(SetTracker.getMaterialTier(held.getType()), NamedTextColor.WHITE)));
                        if (setBonus != null) {
                            sender.sendMessage(Component.text(
                                    "  Bonus at 5 pcs: +" + fmtVal(setBonus.getBaseValue(rarity))
                                    + setBonus.getBonusStat().getUnit() + " " + setBonus.getBonusStat().getLabel(),
                                    NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                        }
                    }
                }
            }

            // /rpgloot set
            case "set" -> {
                if (!sender.hasPermission("rpgloot.command.set")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                SetTracker.ActiveSet active = setTracker.getActiveSet(player);
                if (active == null) {
                    sender.sendMessage(Component.text(
                            "No active set bonus. Equip 2+ items sharing the same set name, rarity, and material tier.",
                            NamedTextColor.GRAY));
                    return true;
                }

                sender.sendMessage(Component.text("─── Active Set ───", NamedTextColor.GOLD));
                sender.sendMessage(label("Set").append(
                        Component.text("◈ " + active.bonus().getDisplayName(), active.rarity().getColor())
                                .decoration(TextDecoration.ITALIC, false)));
                sender.sendMessage(label("Rarity").append(
                        Component.text(active.rarity().getDisplayName(), active.rarity().getColor())));
                sender.sendMessage(label("Tier").append(
                        Component.text(SetTracker.getMaterialTier(active.material()), NamedTextColor.WHITE)));
                sender.sendMessage(label("Pieces").append(
                        Component.text(active.pieces() + "/5", NamedTextColor.WHITE)));
                sender.sendMessage(label("Bonus").append(
                        Component.text("+" + fmtVal(active.value()) + active.bonus().getBonusStat().getUnit()
                                + " " + active.bonus().getBonusStat().getLabel(), NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false)));

                if (active.pieces() < 5) {
                    int next = active.pieces() + 1;
                    double nextVal = active.bonus().getValueForPieces(active.rarity(), next);
                    sender.sendMessage(Component.text(
                            "  Next (" + next + " pcs): +" + fmtVal(nextVal)
                            + active.bonus().getBonusStat().getUnit(),
                            NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                } else {
                    sender.sendMessage(Component.text("  Full set active!", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }

            // /rpgloot clear
            case "clear" -> {
                if (!sender.hasPermission("rpgloot.command.clear")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                int removed = 0;
                var inv = player.getInventory();
                ItemStack[] contents = inv.getContents();
                for (int i = 0; i < contents.length; i++) {
                    if (rarityService.getRarity(contents[i]) != null) {
                        inv.setItem(i, null);
                        removed++;
                    }
                }
                // Also check armor slots
                for (ItemStack piece : inv.getArmorContents()) {
                    if (rarityService.getRarity(piece) != null) removed++;
                }
                inv.setArmorContents(Arrays.stream(inv.getArmorContents())
                        .map(p -> rarityService.getRarity(p) != null ? null : p)
                        .toArray(ItemStack[]::new));

                sender.sendMessage(Component.text("Removed " + removed + " RPGLoot item(s) from inventory.",
                        NamedTextColor.GREEN));
            }

            // /rpgloot reload
            case "reload" -> {
                if (!sender.hasPermission("rpgloot.command.reload")) { noPermission(sender); return true; }
                ((RPGLootPlugin) plugin).reloadAll();
                sender.sendMessage(Component.text("RPGLoot config reloaded.", NamedTextColor.GREEN));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    // ── Tab complete ──────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("rpgloot.command.get"))    subs.add("get");
            if (sender.hasPermission("rpgloot.command.getall")) subs.add("getall");
            if (sender.hasPermission("rpgloot.command.getset")) subs.add("getset");
            if (sender.hasPermission("rpgloot.command.sets"))   subs.add("sets");
            if (sender.hasPermission("rpgloot.command.stats"))  subs.add("stats");
            if (sender.hasPermission("rpgloot.command.set"))    subs.add("set");
            if (sender.hasPermission("rpgloot.command.clear"))  subs.add("clear");
            if (sender.hasPermission("rpgloot.command.reload")) subs.add("reload");
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        // get [rarity] [type] [material] [set]
        if (sub.equals("get")) {
            if (args.length == 2) return filter(rarityNames(), args[1]);
            if (args.length == 3) return filter(new ArrayList<>(TYPE_MAP.keySet()), args[2]);
            if (args.length == 4) {
                WeaponType type = TYPE_MAP.get(args[2].toLowerCase());
                if (type != null) return filter(type.getMaterials().stream()
                        .map(m -> m.name().toLowerCase()).collect(Collectors.toList()), args[3]);
            }
            if (args.length == 5) return filter(SET_NAMES, args[4]);
        }

        // getall [type]
        if (sub.equals("getall") && args.length == 2) {
            return filter(new ArrayList<>(TYPE_MAP.keySet()), args[1]);
        }

        // getset <set> <rarity> <tier> [pieces]
        if (sub.equals("getset")) {
            if (args.length == 2) return filter(SET_NAMES, args[1]);
            if (args.length == 3) return filter(rarityNames(), args[2]);
            if (args.length == 4) return filter(new ArrayList<>(TIER_SET_ITEMS.keySet()), args[3]);
            if (args.length == 5) {
                String tierArg = args[3].toLowerCase();
                List<Material> items = TIER_SET_ITEMS.get(tierArg);
                int max = items != null ? items.size() : 5;
                List<String> counts = new ArrayList<>();
                for (int i = 2; i <= max; i++) counts.add(String.valueOf(i));
                return filter(counts, args[4]);
            }
        }

        // sets [set]
        if (sub.equals("sets") && args.length == 2) {
            return filter(SET_NAMES, args[1]);
        }

        return List.of();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("─── RPGLoot Commands ───", NamedTextColor.GOLD));
        if (sender.hasPermission("rpgloot.command.get"))
            helpLine(sender, "/rpgloot get [rarity] [type] [material] [set]", "Generate an item");
        if (sender.hasPermission("rpgloot.command.getall"))
            helpLine(sender, "/rpgloot getall [type]", "One item per rarity tier");
        if (sender.hasPermission("rpgloot.command.getset"))
            helpLine(sender, "/rpgloot getset <set> <rarity> <tier> [pieces]", "Generate a full matching set");
        if (sender.hasPermission("rpgloot.command.sets"))
            helpLine(sender, "/rpgloot sets [set]", "List all sets or inspect one");
        if (sender.hasPermission("rpgloot.command.stats"))
            helpLine(sender, "/rpgloot stats", "Inspect held item stats");
        if (sender.hasPermission("rpgloot.command.set"))
            helpLine(sender, "/rpgloot set", "View active set bonus");
        if (sender.hasPermission("rpgloot.command.clear"))
            helpLine(sender, "/rpgloot clear", "Remove all RPGLoot items from inventory");
        if (sender.hasPermission("rpgloot.command.reload"))
            helpLine(sender, "/rpgloot reload", "Reload config.yml");
    }

    private void helpLine(CommandSender sender, String cmd, String desc) {
        sender.sendMessage(Component.text(cmd, NamedTextColor.YELLOW)
                .append(Component.text(" — " + desc, NamedTextColor.GRAY)));
    }

    private Component label(String text) {
        return Component.text("  " + text + ": ", NamedTextColor.GRAY);
    }

    private Component err(String msg) {
        return Component.text(msg, NamedTextColor.RED);
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(err("You don't have permission to use this command."));
    }

    private void playerOnly(CommandSender sender) {
        sender.sendMessage(err("This command can only be used by players."));
    }

    private Rarity parseRarity(String input) {
        if (input == null) return Rarity.values()[random.nextInt(Rarity.values().length)];
        try { return Rarity.valueOf(input.toUpperCase()); }
        catch (IllegalArgumentException e) { return Rarity.COMMON; }
    }

    private Rarity parseRarityStrict(String input) {
        try { return Rarity.valueOf(input.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private Material parseMat(String input) {
        try { return Material.valueOf(input); }
        catch (IllegalArgumentException e) { return null; }
    }

    private Material randomMaterialFor(WeaponType type) {
        if (type == null) return null;
        List<Material> mats = List.copyOf(type.getMaterials());
        return mats.get(random.nextInt(mats.size()));
    }

    private List<String> rarityNames() {
        return Arrays.stream(Rarity.values()).map(r -> r.name().toLowerCase()).toList();
    }

    private List<String> filter(List<String> options, String partial) {
        String p = partial.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(p)).collect(Collectors.toList());
    }

    private String fmtVal(double val) {
        return val < 10 ? String.format("%.1f", val) : String.valueOf((int) Math.round(val));
    }
}
