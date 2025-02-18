package org.spigotmc;

import com.google.common.base.Throwables;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import red.mohist.Mohist;
import red.mohist.MohistConfig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class SpigotConfig
{

    private static File CONFIG_FILE;
    private static final String HEADER = "This is the main configuration file for Spigot.\n"
            + "As you can see, there's tons to configure. Some options may impact gameplay, so use\n"
            + "with caution, and make sure you know what each option does before configuring.\n"
            + "For a reference for any variable inside this file, check out the Spigot wiki at\n"
            + "http://www.spigotmc.org/wiki/spigot-configuration/\n"
            + "\n"
            + "If you need help with the configuration or have any questions related to Spigot,\n"
            + "join us at the IRC or drop by our forums and leave a post.\n"
            + "\n"
            + "IRC: #spigot @ irc.spi.gt ( http://www.spigotmc.org/pages/irc/ )\n"
            + "Forums: http://www.spigotmc.org/\n";
    /*========================================================================*/
    public static YamlConfiguration config;
    static int version;
    static Map<String, Command> commands;
    /*========================================================================*/

    public static void init(File configFile)
    {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        try
        {
            config.load( CONFIG_FILE );
        } catch ( IOException ex )
        {
        } catch ( InvalidConfigurationException ex )
        {
            Bukkit.getLogger().log( Level.SEVERE, "Could not load spigot.yml, please correct your syntax errors", ex );
            throw Throwables.propagate( ex );
        }

        config.options().header( HEADER );
        config.options().copyDefaults( true );

        commands = new HashMap<String, Command>();

        version = getInt( "config-version", 11 );
        set( "config-version", 11 );
        readConfig( SpigotConfig.class, null );
    }

    public static void registerCommands()
    {
        for ( Map.Entry<String, Command> entry : commands.entrySet() )
        {
            MinecraftServer.getServerInst().server.getCommandMap().register( entry.getKey(), "Spigot", entry.getValue() );
        }
    }

    static void readConfig(Class<?> clazz, Object instance)
    {
        for ( Method method : clazz.getDeclaredMethods() )
        {
            if ( Modifier.isPrivate( method.getModifiers() ) )
            {
                if ( method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE )
                {
                    try
                    {
                        method.setAccessible( true );
                        method.invoke( instance );
                    } catch ( InvocationTargetException ex )
                    {
                        throw Throwables.propagate( ex.getCause() );
                    } catch ( Exception ex )
                    {
                        Bukkit.getLogger().log( Level.SEVERE, "Error invoking " + method, ex );
                    }
                }
            }
        }

        try
        {
            config.save( CONFIG_FILE );
        } catch ( IOException ex )
        {
            Bukkit.getLogger().log( Level.SEVERE, "Could not save " + CONFIG_FILE, ex );
        }
    }

    private static void set(String path, Object val)
    {
        config.set( path, val );
    }

    private static boolean getBoolean(String path, boolean def)
    {
        config.addDefault( path, def );
        return config.getBoolean( path, config.getBoolean( path ) );
    }

    private static int getInt(String path, int def)
    {
        config.addDefault( path, def );
        return config.getInt( path, config.getInt( path ) );
    }

    private static <T> List getList(String path, T def)
    {
        config.addDefault( path, def );
        return (List<T>) config.getList( path, config.getList( path ) );
    }

    private static String getString(String path, String def)
    {
        config.addDefault( path, def );
        return config.getString( path, config.getString( path ) );
    }

    private static double getDouble(String path, double def)
    {
        config.addDefault( path, def );
        return config.getDouble( path, config.getDouble( path ) );
    }

    public static boolean logCommands;
    private static void logCommands()
    {
        logCommands = getBoolean( "commands.log", true );
    }

    public static int tabComplete;
    private static void tabComplete()
    {
        if ( version < 6 )
        {
            boolean oldValue = getBoolean( "commands.tab-complete", true );
            if ( oldValue )
            {
                set( "commands.tab-complete", 0 );
            } else
            {
                set( "commands.tab-complete", -1 );
            }
        }
        tabComplete = getInt( "commands.tab-complete", 0 );
    }

    //public static String unknownCommandMessage;
    public static String outdatedClientMessage = MohistConfig.outdatedClientMessage;
    public static String outdatedServerMessage = MohistConfig.outdatedServerMessage;
    private static String transform(String s)
    {
        return ChatColor.translateAlternateColorCodes( '&', s ).replaceAll( "\\\\n", "\n" );
    }
    private static void messages()
    {
        if (version < 8)
        {
            //set( "messages.outdated-client", outdatedClientMessage );
            //set( "messages.outdated-server", outdatedServerMessage );
        }

        //unknownCommandMessage = transform( MohistConfig.unknownCommandMessage );
        //outdatedClientMessage = transform(MohistConfig.outdatedClientMessage);
        //outdatedServerMessage = transform( MohistConfig.outdatedServerMessage );
    }

    public static int timeoutTime = 60;
    private static void watchdog()
    {
        timeoutTime = getInt( "settings.timeout-time", timeoutTime );
    }

    public static boolean bungee;
    private static void bungee() {
        if ( version < 4 )
        {
            set( "settings.bungeecord", false );
            System.out.println( "Oudated config, disabling BungeeCord support!" );
        }
        bungee = getBoolean( "settings.bungeecord", false );
    }

    private static void nettyThreads()
    {
        int count = getInt( "settings.netty-threads", 4 );
        System.setProperty( "io.netty.eventLoopThreads", Integer.toString( count ) );
        Bukkit.getLogger().log( Level.INFO, "Using {0} threads for Netty based IO", count );
    }

    public static boolean lateBind;
    private static void lateBind() {
        lateBind = getBoolean( "settings.late-bind", false );
    }

    public static boolean disableStatSaving;
    public static TObjectIntHashMap<String> forcedStats = new TObjectIntHashMap<String>();
    private static void stats()
    {
        disableStatSaving = getBoolean( "stats.disable-saving", false );

        if ( !config.contains( "stats.forced-stats" ) ) {
            config.createSection( "stats.forced-stats" );
        }

        ConfigurationSection section = config.getConfigurationSection( "stats.forced-stats" );
        for ( String name : section.getKeys( true ) )
        {
            if ( section.isInt( name ) )
            {
                if ( StatList.getOneShotStat(name) == null )
                {
                    Mohist.LOGGER.warn( "Ignoring non existent stats.forced-stats " + name);
                    continue;
                }
                forcedStats.put( name, section.getInt( name ) );
            }
        }
    }

    private static void tpsCommand()
    {
        commands.put( "tps", new TicksPerSecondCommand( "tps" ) );
    }

    public static int playerSample;
    private static void playerSample()
    {
        playerSample = Math.max(getInt( "settings.sample-count", 12 ), 0); // Paper - Avoid negative counts
        LogManager.getLogger("Spigot").info( "Server Ping Player Sample Count: " + playerSample );
    }

    public static int playerShuffle;
    private static void playerShuffle()
    {
        playerShuffle = getInt( "settings.player-shuffle", 0 );
    }

    public static List<String> spamExclusions;
    private static void spamExclusions()
    {
        spamExclusions = getList( "commands.spam-exclusions", Arrays.asList( new String[]
        {
                "/skill"
        } ) );
    }

    public static boolean silentCommandBlocks;
    private static void silentCommandBlocks()
    {
        silentCommandBlocks = getBoolean( "commands.silent-commandblock-console", false );
    }

    public static boolean filterCreativeItems;
    private static void filterCreativeItems()
    {
        filterCreativeItems = getBoolean( "settings.filter-creative-items", true );
    }

    public static Set<String> replaceCommands;
    private static void replaceCommands()
    {
        if ( config.contains( "replace-commands" ) )
        {
            set( "commands.replace-commands", config.getStringList( "replace-commands" ) );
            config.set( "replace-commands", null );
        }
        replaceCommands = new HashSet<String>( (List<String>) getList( "commands.replace-commands",
                Arrays.asList( "setblock", "summon", "testforblock", "tellraw" ) ) );
    }

    public static int userCacheCap;
    private static void userCacheCap()
    {
        userCacheCap = getInt( "settings.user-cache-size", 1000 );
    }

    public static boolean saveUserCacheOnStopOnly;
    private static void saveUserCacheOnStopOnly()
    {
        saveUserCacheOnStopOnly = getBoolean( "settings.save-user-cache-on-stop-only", false );
    }

    public static int intCacheLimit;
    private static void intCacheLimit()
    {
        intCacheLimit = getInt( "settings.int-cache-limit", 1024 );
    }

    public static double movedWronglyThreshold;
    private static void movedWronglyThreshold()
    {
        movedWronglyThreshold = getDouble( "settings.moved-wrongly-threshold", 0.0625D );
    }

    public static double movedTooQuicklyMultiplier;
    private static void movedTooQuicklyMultiplier()
    {
        movedTooQuicklyMultiplier = getDouble( "settings.moved-too-quickly-multiplier", 10.0D );
    }

    public static double maxHealth = 2048;
    public static double movementSpeed = 2048;
    public static double attackDamage = 2048;
    private static void attributeMaxes()
    {
        maxHealth = getDouble( "settings.attribute.maxHealth.max", maxHealth );
        ( (RangedAttribute) SharedMonsterAttributes.MAX_HEALTH ).maximumValue = maxHealth;
        movementSpeed = getDouble( "settings.attribute.movementSpeed.max", movementSpeed );
        ( (RangedAttribute) SharedMonsterAttributes.MOVEMENT_SPEED ).maximumValue = movementSpeed;
        attackDamage = getDouble( "settings.attribute.attackDamage.max", attackDamage );
        ( (RangedAttribute) SharedMonsterAttributes.ATTACK_DAMAGE ).maximumValue = attackDamage;
    }

    public static int itemDirtyTicks;
    private static void itemDirtyTicks() {
        itemDirtyTicks = getInt("settings.item-dirty-ticks", 20);
    }

    public static boolean disableAdvancementSaving;
    public static List<String> disabledAdvancements;
    private static void disabledAdvancements() {
        disableAdvancementSaving = getBoolean("advancements.disable-saving", false);
        disabledAdvancements = getList("advancements.disabled", Arrays.asList(new String[]{"minecraft:story/disabled"}));
    }
}
