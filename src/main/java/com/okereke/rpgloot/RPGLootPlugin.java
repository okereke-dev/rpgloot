package com.okereke.rpgloot;

import org.bukkit.plugin.java.JavaPlugin;

public class RPGLootPlugin extends JavaPlugin {

    private ItemRarityService rarityService;
    private SetTracker setTracker;
    private BalanceConfig balanceConfig;
    private PlayerStats playerStats;
    private LootListener lootListener;
    private StructureLootListener structureLootListener;
    private CraftListener craftListener;
    private VillagerTradeListener villagerTradeListener;
    private SetListener setListener;
    private ChestLootDebug chestLootDebug;
    private boolean worldGuardActive;

    @Override
    public void onLoad() {
        // WorldGuard locks its FlagRegistry once it enables, so custom flags must be
        // registered here. onLoad() runs for every plugin (in dependency order) before
        // any plugin's onEnable() runs, so softdepend ordering is respected.
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                WorldGuardHook.registerFlag(getLogger());
                worldGuardActive = true;
                getLogger().info("WorldGuard found — registered 'rpgloot-drops' region flag");
            } catch (NoClassDefFoundError e) {
                getLogger().warning("WorldGuard detected but its API could not be loaded — region-based drop control disabled.");
            }
        }
    }

    /** True if drops/injections are allowed at this location. Always true if WorldGuard isn't installed. */
    public boolean isDropsAllowed(org.bukkit.Location location) {
        if (!worldGuardActive) return true;
        try {
            return WorldGuardHook.isDropsAllowed(location);
        } catch (NoClassDefFoundError e) {
            return true;
        }
    }

    @Override
    public void onEnable() {
        ConfigMerge.mergeAndSave(this, "config.yml");
        reloadConfig();
        getConfig().options().copyDefaults(true);
        Keys.init(this);

        balanceConfig = new BalanceConfig(getConfig(), getLogger());
        rarityService = new ItemRarityService(this, balanceConfig);
        playerStats   = new PlayerStats(getDataFolder(), getLogger());
        setTracker    = new SetTracker(playerStats);

        lootListener          = new LootListener(this, rarityService, playerStats);
        structureLootListener = new StructureLootListener(this, rarityService);
        ToolCrafting toolCrafting = new ToolCrafting(this, rarityService);
        craftListener         = new CraftListener(this, toolCrafting, rarityService);
        villagerTradeListener = new VillagerTradeListener(this, rarityService);
        setListener           = new SetListener(this, setTracker);
        chestLootDebug        = new ChestLootDebug(this, structureLootListener);
        structureLootListener.setChestDebug(chestLootDebug);

        getServer().getPluginManager().registerEvents(lootListener, this);
        getServer().getPluginManager().registerEvents(new MobEquipListener(this, rarityService, lootListener), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, rarityService, setTracker), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this, rarityService), this);
        getServer().getPluginManager().registerEvents(structureLootListener, this);
        getServer().getPluginManager().registerEvents(craftListener, this);
        getServer().getPluginManager().registerEvents(villagerTradeListener, this);
        getServer().getPluginManager().registerEvents(new ArmorListener(this, rarityService, setTracker), this);
        getServer().getPluginManager().registerEvents(new ToolListener(this, rarityService, setTracker), this);
        getServer().getPluginManager().registerEvents(setListener, this);
        getServer().getPluginManager().registerEvents(new SetsMenuListener(), this);
        getServer().getPluginManager().registerEvents(chestLootDebug, this);

        AdminCommand adminCommand = new AdminCommand(this, rarityService, setTracker, playerStats);
        getCommand("rpgloot").setExecutor(adminCommand);
        getCommand("rpgloot").setTabCompleter(adminCommand);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RPGLootExpansion(this).register();
            getLogger().info("PlaceholderAPI found — registered %rpgloot_*% placeholders");
        }

        getLogger().info("RPGLoot enabled");
    }

    @Override
    public void onDisable() {
        // onEnable may have failed mid-way; never NPE during disable/reload.
        if (setListener != null) {
            setListener.cancelAllPending();
        }
        DamageNumbers.cleanup(getServer());
        getLogger().info("RPGLoot disabled");
    }

    /** Called by AdminCommand on /rpgloot reload. Reloads config and propagates to all consumers. */
    public void reloadAll() {
        ConfigMerge.mergeAndSave(this, "config.yml");
        reloadConfig();
        getConfig().options().copyDefaults(true);
        balanceConfig.reload(getConfig());
        rarityService.clearStatCache();
        lootListener.reload();
        structureLootListener.reload();
        craftListener.reload();
        villagerTradeListener.reload();
    }

    public ItemRarityService getRarityService() { return rarityService; }
    public SetTracker getSetTracker()           { return setTracker; }
    public PlayerStats getPlayerStats()         { return playerStats; }
    public ChestLootDebug getChestLootDebug()   { return chestLootDebug; }
}
