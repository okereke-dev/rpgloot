package com.ricardo.rpgloot;

import org.bukkit.plugin.java.JavaPlugin;

public class RPGLootPlugin extends JavaPlugin {

    private ItemRarityService rarityService;
    private SetTracker setTracker;
    private BalanceConfig balanceConfig;
    private LootListener lootListener;
    private StructureLootListener structureLootListener;
    private SetListener setListener;
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
        saveDefaultConfig();
        Keys.init(this);

        balanceConfig = new BalanceConfig(getConfig(), getLogger());
        rarityService = new ItemRarityService(this, balanceConfig);
        setTracker    = new SetTracker();

        lootListener          = new LootListener(this, rarityService);
        structureLootListener = new StructureLootListener(this, rarityService);
        setListener           = new SetListener(this, setTracker);

        getServer().getPluginManager().registerEvents(lootListener, this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, rarityService, setTracker), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this, rarityService), this);
        getServer().getPluginManager().registerEvents(structureLootListener, this);
        getServer().getPluginManager().registerEvents(new ArmorListener(this, rarityService, setTracker), this);
        getServer().getPluginManager().registerEvents(new ToolListener(this, rarityService, setTracker), this);
        getServer().getPluginManager().registerEvents(setListener, this);
        getServer().getPluginManager().registerEvents(new SetsMenuListener(), this);

        AdminCommand adminCommand = new AdminCommand(this, rarityService, setTracker);
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
        setListener.cancelAllPending();
        DamageNumbers.cleanup(getServer());
        getLogger().info("RPGLoot disabled");
    }

    /** Called by AdminCommand on /rpgloot reload. Reloads config and propagates to all consumers. */
    public void reloadAll() {
        reloadConfig();
        balanceConfig.reload(getConfig());
        rarityService.clearStatCache();
        lootListener.reload();
        structureLootListener.reload();
    }

    public ItemRarityService getRarityService() { return rarityService; }
    public SetTracker getSetTracker()           { return setTracker; }
}
