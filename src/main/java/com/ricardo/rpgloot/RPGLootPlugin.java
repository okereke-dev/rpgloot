package com.ricardo.rpgloot;

import org.bukkit.plugin.java.JavaPlugin;

public class RPGLootPlugin extends JavaPlugin {

    private ItemRarityService rarityService;
    private SetTracker setTracker;
    private BalanceConfig balanceConfig;
    private LootListener lootListener;
    private StructureLootListener structureLootListener;
    private SetListener setListener;

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

        AdminCommand adminCommand = new AdminCommand(this, rarityService, setTracker);
        getCommand("rpgloot").setExecutor(adminCommand);
        getCommand("rpgloot").setTabCompleter(adminCommand);

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
