package org.milkteamc.autotreechop;

import de.cubbossa.tinytranslations.*;
import de.cubbossa.tinytranslations.libs.kyori.adventure.text.ComponentLike;
import de.cubbossa.tinytranslations.storage.properties.PropertiesMessageStorage;
import de.cubbossa.tinytranslations.storage.properties.PropertiesStyleStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.milkteamc.autotreechop.hooks.GriefPreventionHook;
import org.milkteamc.autotreechop.hooks.LandsHook;
import org.milkteamc.autotreechop.hooks.McMMOHook;
import org.milkteamc.autotreechop.hooks.CoreProtectHook;
import org.milkteamc.autotreechop.hooks.ResidenceHook;
import org.milkteamc.autotreechop.hooks.WorldGuardHook;
import org.milkteamc.autotreechop.hooks.Drop2InventoryHook;
import org.milkteamc.autotreechop.utils.CooldownManager;
import org.milkteamc.autotreechop.utils.EffectUtils;
import org.milkteamc.autotreechop.utils.PermissionUtils;
import org.milkteamc.autotreechop.utils.TreeChopUtils;
import org.milkteamc.autotreechop.SaplingManager;

import java.io.File;
import java.util.*;

public class AutoTreeChop extends JavaPlugin implements Listener, CommandExecutor {

    // We make a prefix just to be safe if sm removes our style overrides. Then each plugin message begins with prefix
    // and if none set it will look ugly. We don't need to add decoration (like "[AutoTreeChop] >"), it's done via styling
    public static final Message PREFIX = Message.unowned("prefix", "AutoTreeChop");
    public static final Message noResidencePermissions = new MessageBuilder("noResidencePermissions")
            .withDefault("<prefix_negative>You don't have permission to use AutoTreeChop here.</prefix_negative>").build();
    public static final Message ENABLED_MESSAGE = new MessageBuilder("enabled")
            .withDefault("<prefix>Auto tree chopping enabled.</prefix>").build();
    public static final Message DISABLED_MESSAGE = new MessageBuilder("disabled")
            .withDefault("<prefix_negative>Auto tree chopping disabled.</prefix_negative>").build();
    public static final Message NO_PERMISSION_MESSAGE = new MessageBuilder("no-permission")
            .withDefault(GlobalMessages.NO_PERM_CMD).build();
    public static final Message ONLY_PLAYERS_MESSAGE = new MessageBuilder("only-players")
            .withDefault(GlobalMessages.CMD_PLAYER_ONLY).build();
    public static final Message HIT_MAX_USAGE_MESSAGE = new MessageBuilder("hitmaxusage")
            .withDefault("<prefix_negative>You've reached the daily usage limit.</prefix_negative>").build();
    public static final Message HIT_MAX_BLOCK_MESSAGE = new MessageBuilder("hitmaxblock")
            .withDefault("<prefix_negative>You have reached your daily block breaking limit.</prefix_negative>").build();
    public static final Message USAGE_MESSAGE = new MessageBuilder("usage")
            .withDefault("<prefix>You have used the AutoTreeChop {current_uses}/{max_uses} times today.</prefix>").build();
    public static final Message BLOCKS_BROKEN_MESSAGE = new MessageBuilder("blocks-broken")
            .withDefault("<prefix>You have broken {current_blocks}/{max_blocks} blocks today.</prefix>").build();
    public static final Message ENABLED_BY_OTHER_MESSAGE = new MessageBuilder("enabledByOther")
            .withDefault("<prefix>Auto tree chopping enabled by {player}.</prefix>").build();
    public static final Message ENABLED_FOR_OTHER_MESSAGE = new MessageBuilder("enabledForOther")
            .withDefault("<prefix>Auto tree chopping enabled for {player}</prefix>").build();
    public static final Message DISABLED_BY_OTHER_MESSAGE = new MessageBuilder("disabledByOther")
            .withDefault("<prefix_negative>Auto tree chopping disabled by {player}.</prefix_negative>").build();
    public static final Message DISABLED_FOR_OTHER_MESSAGE = new MessageBuilder("disabledForOther")
            .withDefault("<prefix_negative>Auto tree chopping disabled for {player}</prefix_negative>").build();
    public static final Message STILL_IN_COOLDOWN_MESSAGE = new MessageBuilder("stillInCooldown")
            .withDefault("<prefix_negative>You are still in cooldown! Try again after {cooldown_time} seconds.</prefix_negative>").build();
    public static final Message CONSOLE_NAME = new MessageBuilder("consoleName")
            .withDefault("console").build();
    public static final Message SNEAK_ENABLED_MESSAGE = new MessageBuilder("sneakEnabled")
            .withDefault("<prefix>Auto tree chopping enabled after stop sneaking.</prefix>").build();
    public static final Message SNEAK_DISABLED_MESSAGE = new MessageBuilder("sneakDisabled")
            .withDefault("<prefix_negative>Auto tree chopping disabled while sneaking.</prefix_negative>").build();

    private static final String SPIGOT_RESOURCE_ID = "113071";
    private static final List<String> SUPPORTED_VERSIONS = Arrays.asList(
            "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
            "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.20",
            "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19",
            "1.18.2", "1.18.1", "1.18",
            "1.17.1", "1.17"
    );

    private Config config; // Instance of your Config class
    private AutoTreeChopAPI autoTreeChopAPI;
    private Map<UUID, PlayerConfig> playerConfigs;
    private final Set<Location> checkedLocations = new HashSet<>();
    private final Set<Location> processingLocations = new HashSet<>();
    private String bukkitVersion = this.getServer().getBukkitVersion();
    private Metrics metrics;
    private MessageTranslator translations;

    private boolean worldGuardEnabled = false;
    private boolean residenceEnabled = false;
    private boolean griefPreventionEnabled = false;
    private boolean landsEnabled = false;
    private boolean mcMMOEnabled = false;
    private boolean coreProtectEnabled = false;
    private boolean drop2InventoryEnabled = false;
    private WorldGuardHook worldGuardHook = null;
    private ResidenceHook residenceHook = null;
    private GriefPreventionHook griefPreventionHook = null;
    private LandsHook landsHook = null;
    private McMMOHook mcMMOHook = null;
    private CoreProtectHook coreProtectHook = null;
    private Drop2InventoryHook drop2InventoryHook = null;

    private CooldownManager cooldownManager;
    private SaplingManager saplingManager;
    private boolean enableSneakToggle = true; // Configuration option for the sneak toggle feature

    public static void sendMessage(CommandSender sender, ComponentLike message) {
        BukkitTinyTranslations.sendMessageIfNotEmpty(sender, message);
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onEnable() {
        // Initialize Config and sapling data
        config = new Config(this);
        saplingManager = new SaplingManager(this);

        // Bukkit version checker
        // Put your version check *after* loading the config, in case you add version-specific settings.
        if (bukkitVersion.length() > 14) {
            bukkitVersion = bukkitVersion.substring(0, bukkitVersion.length() - 14);
            if (!SUPPORTED_VERSIONS.contains(bukkitVersion)) {
                getLogger().warning("Your Minecraft version didn't fully tested yet.");
                getLogger().warning("IF you have any issues, feel free to report it at our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
            }
        }

        metrics = new Metrics(this, 20053); //bstats
        getServer().getPluginManager().registerEvents(this, this);

        // Register command and tab completer
        org.milkteamc.autotreechop.command.Command command = new org.milkteamc.autotreechop.command.Command(this);
        Objects.requireNonNull(getCommand("autotreechop")).setExecutor(command);
        Objects.requireNonNull(getCommand("atc")).setExecutor(command);
        Objects.requireNonNull(getCommand("autotreechop")).setTabCompleter(new org.milkteamc.autotreechop.command.TabCompleter());
        Objects.requireNonNull(getCommand("atc")).setTabCompleter(new org.milkteamc.autotreechop.command.TabCompleter());

        translations = BukkitTinyTranslations.application(this);
        translations.setMessageStorage(new PropertiesMessageStorage(new File(getDataFolder(), "/lang/")));
        translations.setStyleStorage(new PropertiesStyleStorage(new File(getDataFolder(), "/lang/styles.properties")));
        translations.addMessages(TinyTranslations.messageFieldsFromClass(AutoTreeChop.class));

        loadLocale(); //Still needs to be called to *use* the locale.

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AutoTreeChopExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion for AutoTreeChop has been registered.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholder expansion for AutoTreeChop will not work.");
        }

        autoTreeChopAPI = new AutoTreeChopAPI(this);
        playerConfigs = new HashMap<>();
        initializeHooks(); // Initialize protection plugin hooks

        cooldownManager = new CooldownManager(this);

        // Load the enableSneakToggle option from config
        enableSneakToggle = config.getSneakToggle();
    }


    private void initializeHooks() {
        // Residence hook initialization
        if (Bukkit.getPluginManager().getPlugin("Residence") != null) {
            try {
                residenceHook = new ResidenceHook(config.getResidenceFlag());
                residenceEnabled = true;
                getLogger().info("Residence support enabled");
            } catch (Exception e) {
                getLogger().warning("Residence can't be hook, please report this to our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
                residenceEnabled = false;
            }
        } else {
            residenceEnabled = false;
        }
        // GriefPrevention hook initialization
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
            try {
                griefPreventionHook = new GriefPreventionHook(config.getGriefPreventionFlag());
                griefPreventionEnabled = true;
                getLogger().info("GriefPrevention support enabled");
            } catch (Exception e) {
                getLogger().warning("GriefPrevention can't be hook, please report this to our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
                griefPreventionEnabled = false;
            }
        } else {
            griefPreventionEnabled = false;
        }
        // Lands hook initialization
        if (Bukkit.getPluginManager().getPlugin("Lands") != null) {
            try {
                landsHook = new LandsHook(this);
                landsEnabled = true;
                getLogger().info("Lands support enabled");
            } catch (Exception e) {
                getLogger().warning("Lands can't be hook, please report this to our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
                landsEnabled = false;
            }
        } else {
            landsEnabled = false;
        }
        // mcMMO hook initialization
        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            try {
                mcMMOHook = new McMMOHook();
                mcMMOEnabled = true;
                getLogger().info("mcMMO support enabled");
            } catch (Exception e) {
                getLogger().warning("mcMMO can't be hook, please report this to our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
                mcMMOEnabled = false;
            }
        } else {
            mcMMOEnabled = false;
        }
        // CoreProtect hook initialization
        if (Bukkit.getPluginManager().getPlugin("CoreProtect") != null) {
            try {
                coreProtectHook = new CoreProtectHook();
                coreProtectEnabled = true;
                getLogger().info("CoreProtect support enabled");
            } catch (Exception e) {
                getLogger().warning("CoreProtect can't be hook, please report this to our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
                coreProtectEnabled = false;
            }
        } else {
            coreProtectEnabled = false;
        }
        // Drop2Inventory-Plus hook initialization
        if (Bukkit.getPluginManager().getPlugin("Drop2Inventory") != null ||
                Bukkit.getPluginManager().getPlugin("Drop2InventoryPlus") != null) {
            try {
                drop2InventoryHook = new Drop2InventoryHook();
                drop2InventoryEnabled = true;
                getLogger().info("Drop2Inventory support enabled");
            } catch (Exception e) {
                getLogger().warning("Drop2Inventory can't be hook, please report this to our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
                drop2InventoryEnabled = false;
            }
        } else {
            drop2InventoryEnabled = false;
        }
        // Initialize WorldGuard support
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                worldGuardHook = new WorldGuardHook();
                getLogger().info("WorldGuard support enabled");
            } catch (NoClassDefFoundError e) {
                getLogger().warning("WorldGuard can't be hook, please report this to our GitHub: https://github.com/milkteamc/AutoTreeChop/issues");
                worldGuardEnabled = false;
            }
        } else {
            worldGuardEnabled = false;
        }
    }


    private void loadLocale() {
        saveResourceIfNotExists("lang/styles.properties");
        saveResourceIfNotExists("lang/de.properties");
        saveResourceIfNotExists("lang/es.properties");
        saveResourceIfNotExists("lang/fr.properties");
        saveResourceIfNotExists("lang/ja.properties");
        saveResourceIfNotExists("lang/ru.properties");
        saveResourceIfNotExists("lang/zh.properties");
        translations.setUseClientLocale(config.isUseClientLocale());
        translations.defaultLocale(config.getLocale() == null ? Locale.getDefault() : config.getLocale());
        translations.loadStyles();
        translations.loadLocales();
    }

    private void saveResourceIfNotExists(String resourcePath) {
        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    @Override
    public void onDisable() {
        translations.close();
        metrics.shutdown();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        PlayerConfig playerConfig = getPlayerConfig(playerUUID);
        Block block = event.getBlock();

        // Skip if this block is already being processed
        if (processingLocations.contains(block.getLocation())) {
            return;
        }

        if (mcMMOEnabled && !mcMMOHook.isNatural(block)) {
            // Let normal block break occur but don't trigger tree chopping
            return;
        }

        if (cooldownManager.isInCooldown(playerUUID)) {
            sendMessage(player, STILL_IN_COOLDOWN_MESSAGE
                    .insertNumber("cooldown_time", cooldownManager.getRemainingCooldown(playerUUID))
            );
            event.setCancelled(true);
            return;
        }

        Material material = block.getType();
        Location location = block.getLocation();
        BlockData blockData = block.getBlockData();

        if (config.getMustUseTool() && !TreeChopUtils.isTool(player)) {
            return;
        }

        if (playerConfig.isAutoTreeChopEnabled() && TreeChopUtils.isLog(material, config)) {
            if (!PermissionUtils.hasVipBlock(player, playerConfig, config)) {
                if (playerConfig.getDailyBlocksBroken() >= config.getMaxBlocksPerDay()) {
                    EffectUtils.sendMaxBlockLimitReachedMessage(player, block, HIT_MAX_BLOCK_MESSAGE);
                    event.setCancelled(true);
                    return;
                }
            }
            if (!PermissionUtils.hasVipUses(player, playerConfig, config) && playerConfig.getDailyUses() >= config.getMaxUsesPerDay()) {
                BukkitTinyTranslations.sendMessage(player, HIT_MAX_USAGE_MESSAGE);
                return;
            }

            if (config.isVisualEffect()) {  // Use the getter from the Config object
                EffectUtils.showChopEffect(player, block);
            }

            event.setCancelled(true);
            checkedLocations.clear();
            ItemStack tool = player.getInventory().getItemInMainHand();
            TreeChopUtils.chopTree(block, player, tool, config.isStopChoppingIfNotConnected(), location, material, blockData, this, processingLocations, checkedLocations, config, playerConfig, worldGuardEnabled, residenceEnabled, griefPreventionEnabled, landsEnabled, landsHook, residenceHook, griefPreventionHook, worldGuardHook, mcMMOEnabled, mcMMOHook, coreProtectEnabled, coreProtectHook, drop2InventoryEnabled, drop2InventoryHook); // Pass config values
            checkedLocations.clear();
            playerConfig.incrementDailyUses();
            cooldownManager.setCooldown(player, playerUUID, config); // Pass config values
        }
    }

    /**
     * Event handler for player sneak toggle
     * When a player toggles sneak, enable or disable AutoTreeChop
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        // Skip if the sneak toggle feature is disabled in config
        if (!enableSneakToggle) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if player has permission to use the plugin
        if (!player.hasPermission("autotreechop.use")) {
            return;
        }

        PlayerConfig playerConfig = getPlayerConfig(playerUUID);

        if (event.isSneaking()) {
            // Player started sneaking - disable auto tree chop
            playerConfig.setAutoTreeChopEnabled(false);
            if (config.getSneakMessage()) { sendMessage(player, SNEAK_DISABLED_MESSAGE); }
        } else {
            // Player stopped sneaking - enable auto tree chop
            playerConfig.setAutoTreeChopEnabled(true);
            if (config.getSneakMessage()) { sendMessage(player, SNEAK_ENABLED_MESSAGE); }
        }
    }

    public PlayerConfig getPlayerConfig(UUID playerUUID) {
        PlayerConfig playerConfig = playerConfigs.get(playerUUID);
        if (playerConfig == null) {
            playerConfig = new PlayerConfig(playerUUID, config.isUseMysql(), config.getHostname(), config.getDatabase(), config.getPort(), config.getUsername(), config.getPassword(), config.getDefaultTreeChop());
            playerConfigs.put(playerUUID, playerConfig);
        }
        return playerConfig;
    }

    public int getPlayerDailyUses(UUID playerUUID) {
        return getPlayerConfig(playerUUID).getDailyUses();
    }

    public int getPlayerDailyBlocksBroken(UUID playerUUID) {
        return getPlayerConfig(playerUUID).getDailyBlocksBroken();
    }

    public AutoTreeChopAPI getAutoTreeChopAPI() {
        return autoTreeChopAPI;
    }

    // Add a getter for the Config instance
    public Config getPluginConfig() {
        return config;
    }

    public SaplingManager getSaplingManager() {
        return saplingManager;
    }
}