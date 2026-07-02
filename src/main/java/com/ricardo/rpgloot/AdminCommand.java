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

    private final Plugin plugin;
    private final ItemRarityService rarityService;
    private final SetTracker setTracker;
    private final Random random = new Random();

    public AdminCommand(Plugin plugin, ItemRarityService rarityService, SetTracker setTracker) {
        this.plugin       = plugin;
        this.rarityService = rarityService;
        this.setTracker   = setTracker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            case "get" -> {
                if (!sender.hasPermission("rpgloot.command.get")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                Rarity rarity   = parseRarity(args.length > 1 ? args[1] : null);
                String typeArg  = args.length > 2 ? args[2].toLowerCase() : null;
                String matArg   = args.length > 3 ? args[3].toUpperCase() : null;

                WeaponType type  = typeArg != null ? TYPE_MAP.get(typeArg) : null;
                Material material = matArg != null ? parseMat(matArg) : null;
                if (material == null) material = randomMaterialFor(type);
                if (material == null) material = ALL_POOL.get(random.nextInt(ALL_POOL.size()));
                if (type == null)     type = WeaponType.of(material);
                if (type == null) {
                    sender.sendMessage(err("Unsupported material."));
                    return true;
                }

                ItemStack item = new ItemStack(material);
                rarityService.applyRarity(item, rarity, type);
                player.getInventory().addItem(item);
                sender.sendMessage(Component.text("Generated: ", NamedTextColor.GRAY)
                        .append(Component.text(rarity.getDisplayName() + " " + material.name(), rarity.getColor())
                                .decoration(TextDecoration.ITALIC, false)));
            }

            case "getall" -> {
                if (!sender.hasPermission("rpgloot.command.getall")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                String typeArg   = args.length > 1 ? args[1].toLowerCase() : null;
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

                // Set info from PDC
                if (held.hasItemMeta()) {
                    String setName = held.getItemMeta().getPersistentDataContainer()
                            .get(Keys.SET_NAME, org.bukkit.persistence.PersistentDataType.STRING);
                    if (setName != null) {
                        SetBonus setBonus = SetBonus.fromName(setName);
                        sender.sendMessage(Component.text("  Set: ", NamedTextColor.GRAY)
                                .append(Component.text("◈ " + setName, rarity.getColor())
                                        .decoration(TextDecoration.ITALIC, false)));
                        if (setBonus != null) {
                            sender.sendMessage(Component.text(
                                    "  Bonus: +" + fmtVal(setBonus.getBaseValue(rarity))
                                    + setBonus.getBonusStat().getUnit() + " " + setBonus.getBonusStat().getLabel() + " at 5 pcs",
                                    NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                        }
                    }
                }
            }

            case "set" -> {
                if (!sender.hasPermission("rpgloot.command.set")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { playerOnly(sender); return true; }

                SetTracker.ActiveSet active = setTracker.getActiveSet(player);
                if (active == null) {
                    sender.sendMessage(Component.text("No active set bonus. Equip 2+ matching items (same set name, rarity and material).", NamedTextColor.GRAY));
                    return true;
                }

                sender.sendMessage(Component.text("─── Active Set ───", NamedTextColor.GOLD));
                sender.sendMessage(label("Set").append(
                        Component.text("◈ " + active.bonus().getDisplayName(), active.rarity().getColor())
                                .decoration(TextDecoration.ITALIC, false)));
                sender.sendMessage(label("Rarity").append(
                        Component.text(active.rarity().getDisplayName(), active.rarity().getColor())));
                sender.sendMessage(label("Material").append(
                        Component.text(active.material().name(), NamedTextColor.WHITE)));
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

            case "reload" -> {
                if (!sender.hasPermission("rpgloot.command.reload")) { noPermission(sender); return true; }
                ((RPGLootPlugin) plugin).reloadAll();
                sender.sendMessage(Component.text("RPGLoot config reloaded.", NamedTextColor.GREEN));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("rpgloot.command.get"))    subs.add("get");
            if (sender.hasPermission("rpgloot.command.getall")) subs.add("getall");
            if (sender.hasPermission("rpgloot.command.stats"))  subs.add("stats");
            if (sender.hasPermission("rpgloot.command.set"))    subs.add("set");
            if (sender.hasPermission("rpgloot.command.reload")) subs.add("reload");
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("get")) {
            if (args.length == 2) return filter(rarityNames(), args[1]);
            if (args.length == 3) return filter(new ArrayList<>(TYPE_MAP.keySet()), args[2]);
            if (args.length == 4) {
                WeaponType type = TYPE_MAP.get(args[2].toLowerCase());
                if (type != null) {
                    return filter(type.getMaterials().stream()
                            .map(m -> m.name().toLowerCase()).collect(Collectors.toList()), args[3]);
                }
            }
        }

        if (sub.equals("getall") && args.length == 2) {
            return filter(new ArrayList<>(TYPE_MAP.keySet()), args[1]);
        }

        return List.of();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("─── RPGLoot ───", NamedTextColor.GOLD));
        if (sender.hasPermission("rpgloot.command.get"))
            helpLine(sender, "/rpgloot get [rarity] [type] [material]", "Generate an item");
        if (sender.hasPermission("rpgloot.command.getall"))
            helpLine(sender, "/rpgloot getall [type]", "One item per rarity tier");
        if (sender.hasPermission("rpgloot.command.stats"))
            helpLine(sender, "/rpgloot stats", "Inspect held item stats");
        if (sender.hasPermission("rpgloot.command.set"))
            helpLine(sender, "/rpgloot set", "View active set bonus");
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
