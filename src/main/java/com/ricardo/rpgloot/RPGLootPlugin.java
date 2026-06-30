package com.ricardo.rpgloot;

import org.bukkit.plugin.java.JavaPlugin;

public class RPGLootPlugin extends JavaPlugin {

    private ItemRarityService rarityService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        rarityService = new ItemRarityService(getConfig());

        getServer().getPluginManager().registerEvents(new LootListener(this, rarityService), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, rarityService), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this, rarityService), this);

        TestCommand testCommand = new TestCommand(rarityService);
        getCommand("rpgloot").setExecutor(testCommand);
        getCommand("rpgloot").setTabCompleter(testCommand);

        getLogger().info("RPGLoot enabled");
    }

    @Override
    public void onDisable() {
        DamageNumbers.cleanup(getServer());
        getLogger().info("RPGLoot disabled");
    }

    public ItemRarityService getRarityService() {
        return rarityService;
    }
}
