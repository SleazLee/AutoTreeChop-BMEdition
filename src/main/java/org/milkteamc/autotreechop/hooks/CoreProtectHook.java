package org.milkteamc.autotreechop.hooks;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Simple wrapper around the CoreProtect API so AutoTreeChop can log
 * blocks it removes. This allows server admins to roll back changes
 * if needed.
 */
public class CoreProtectHook {
    private final CoreProtectAPI api;

    public CoreProtectHook() {
        CoreProtect plugin = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");
        if (plugin != null && plugin.isEnabled()) {
            api = plugin.getAPI();
        } else {
            api = null;
        }
    }

    /**
     * Record the removal of a block by a player.
     *
     * @param player Player responsible for the break
     * @param block  The block being removed
     */
    public void logRemoval(Player player, Block block) {
        if (api != null && api.isEnabled()) {
            api.logRemoval(player.getName(), block.getLocation(), block.getType(), block.getBlockData());
        }
    }

    /**
     * Determine if the specified block was placed by a player according to
     * CoreProtect's records.
     *
     * @param block the block to check
     * @return {@code true} if CoreProtect logged the block, otherwise {@code false}
     */
    public boolean isPlayerPlaced(Block block) {
        if (api != null && api.isEnabled()) {
            List<String[]> history = api.blockLookup(block, -1);
            if (history != null) {
                for (String[] entry : history) {
                    CoreProtectAPI.ParseResult result = api.parseResult(entry);
                    if (result.getActionId() == 1) { // 1 = block placed
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
