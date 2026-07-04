package com.ricardo.rpgloot;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;

import java.util.logging.Logger;

/**
 * All direct references to WorldGuard classes live in this file only. If WorldGuard
 * is not installed, this class is never loaded/verified by the JVM as long as callers
 * guard every entry point with a presence check first (see RPGLootPlugin).
 */
public final class WorldGuardHook {

    /** Custom region flag: rpgloot-drops, defaults to ALLOW. Null until registerFlag() succeeds. */
    private static StateFlag RPGLOOT_DROPS;

    private WorldGuardHook() {}

    /** Must be called from onLoad() — WorldGuard locks its FlagRegistry once it enables. */
    public static void registerFlag(Logger logger) {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("rpgloot-drops", true);
            registry.register(flag);
            RPGLOOT_DROPS = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("rpgloot-drops");
            if (existing instanceof StateFlag stateFlag) {
                RPGLOOT_DROPS = stateFlag;
            } else {
                logger.warning("Another plugin registered a non-boolean 'rpgloot-drops' WorldGuard flag — region-based drop control disabled.");
            }
        }
    }

    /** True if RPGLoot drops/injections are allowed at this location (region flag not denied). */
    public static boolean isDropsAllowed(Location location) {
        if (RPGLOOT_DROPS == null) return true; // flag registration failed or never ran
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        return query.testState(BukkitAdapter.adapt(location), (com.sk89q.worldguard.LocalPlayer) null, RPGLOOT_DROPS);
    }
}
