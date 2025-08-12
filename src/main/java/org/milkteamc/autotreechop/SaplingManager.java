package org.milkteamc.autotreechop;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SaplingManager {
    private final AutoTreeChop plugin;
    private final Map<Material, Material> saplingMap = new HashMap<>();
    private static final Set<Material> SOIL_BLOCKS = EnumSet.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.COARSE_DIRT,
            Material.MYCELIUM,
            Material.ROOTED_DIRT,
            Material.FARMLAND,
            Material.MOSS_BLOCK,
            Material.MUD,
            Material.MUDDY_MANGROVE_ROOTS
    );

    private static final int SEARCH_RADIUS = 5;

    public SaplingManager(AutoTreeChop plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        saplingMap.clear();
        File file = new File(plugin.getDataFolder(), "saplings.yml");
        if (!file.exists()) {
            plugin.saveResource("saplings.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            Material log = Material.getMaterial(key);
            Material sapling = Material.getMaterial(config.getString(key));
            if (log != null && sapling != null) {
                saplingMap.put(log, sapling);
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save saplings.yml: " + e.getMessage());
        }
    }

    public void plantSapling(Material originalLog, Block brokenBlock) {
        Material sapling = saplingMap.get(originalLog);
        if (sapling == null) {
            return;
        }
        Block below = brokenBlock.getRelative(0, -1, 0);
        if (!SOIL_BLOCKS.contains(below.getType()) || brokenBlock.getType() != Material.AIR) {
            return;
        }

        if (isTwoByTwo(originalLog, sapling, brokenBlock)) {
            brokenBlock.setType(sapling);
            return;
        }

        if (hasNearbySapling(brokenBlock, sapling, SEARCH_RADIUS)) {
            return;
        }

        brokenBlock.setType(sapling);
    }

    private boolean hasNearbySapling(Block origin, Material sapling, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block check = origin.getRelative(dx, 0, dz);
                if (check.getType() == sapling) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTwoByTwo(Material log, Material sapling, Block center) {
        int[][] anchors = {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}};
        for (int[] anchor : anchors) {
            boolean matches = true;
            for (int dx = 0; dx <= 1 && matches; dx++) {
                for (int dz = 0; dz <= 1 && matches; dz++) {
                    Block block = center.getRelative(anchor[0] + dx, 0, anchor[1] + dz);
                    if (block.equals(center)) {
                        if (!SOIL_BLOCKS.contains(block.getRelative(0, -1, 0).getType())) {
                            matches = false;
                        }
                        continue;
                    }
                    Material type = block.getType();
                    if (type != log && type != sapling) {
                        matches = false;
                        break;
                    }
                    if (!SOIL_BLOCKS.contains(block.getRelative(0, -1, 0).getType())) {
                        matches = false;
                    }
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }
}
