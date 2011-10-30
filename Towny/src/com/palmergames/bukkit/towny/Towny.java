package com.palmergames.bukkit.towny;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;

import com.iConomy.iConomy;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.register.Register;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.command.ResidentCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyAdminCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.command.TownyWorldCommand;
import com.palmergames.bukkit.towny.event.TownyBlockListener;
import com.palmergames.bukkit.towny.event.TownyEntityListener;
import com.palmergames.bukkit.towny.event.TownyEntityMonitorListener;
import com.palmergames.bukkit.towny.event.TownyPlayerListener;
//import com.palmergames.bukkit.towny.event.TownyPlayerLowListener;
import com.palmergames.bukkit.towny.event.TownyWorldListener;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.BukkitPermSource;
import com.palmergames.bukkit.towny.permissions.GroupManagerSource;
import com.palmergames.bukkit.towny.permissions.NullPermSource;
import com.palmergames.bukkit.towny.permissions.PEXSource;
import com.palmergames.bukkit.towny.permissions.Perms3Source;
import com.palmergames.bukkit.towny.permissions.bPermsSource;
import com.palmergames.bukkit.towny.questioner.TownyQuestionTask;
import com.palmergames.bukkit.townywar.TownyWar;
import com.palmergames.bukkit.townywar.listener.TownyWarBlockListener;
import com.palmergames.bukkit.townywar.listener.TownyWarCustomListener;
import com.palmergames.bukkit.townywar.listener.TownyWarEntityListener;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.StringMgmt;


/**
 * Towny Plugin for Bukkit
 * 
 * Website: http://code.google.com/a/eclipselabs.org/p/towny/
 * Source: http://code.google.com/a/eclipselabs.org/p/towny/source/browse/
 * 
 * @author Shade, ElgarL
 */

public class Towny extends JavaPlugin {
        
	private String version = "2.0.0";

	public static PermissionHandler permissionHandler;

    private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
    private final TownyBlockListener blockListener = new TownyBlockListener(this);
    private final TownyEntityListener entityListener = new TownyEntityListener(this);
    private final TownyEntityMonitorListener entityMonitorListener = new TownyEntityMonitorListener(this);
    private final TownyWorldListener worldListener = new TownyWorldListener(this);
    private final TownyWarBlockListener townyWarBlockListener = new TownyWarBlockListener(this);
    private final TownyWarCustomListener customListener = new TownyWarCustomListener(this);
    private final TownyWarEntityListener townyWarEntityListener = new TownyWarEntityListener(this);
    private TownyUniverse townyUniverse;
    private Map<String, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<String, PlayerCache>());
    private Map<String, List<String>> playerMode = Collections.synchronizedMap(new HashMap<String, List<String>>());
    
    private Register register = null;
    private iConomy iconomy = null;

    private boolean error = false;
   
  
        
    @Override
    public void onEnable() {
    	
    	
    	System.out.println("====================      Towny      ========================");
    	
    	version = this.getDescription().getVersion();
        townyUniverse = new TownyUniverse(this);

        if (!load())
        	return;
        
        // Setup bukkit command interfaces
        getCommand("townyadmin").setExecutor(new TownyAdminCommand(this));
        getCommand("townyworld").setExecutor(new TownyWorldCommand(this));
        getCommand("resident").setExecutor(new ResidentCommand(this));
        getCommand("towny").setExecutor(new TownyCommand(this));
        getCommand("town").setExecutor(new TownCommand(this));
        getCommand("nation").setExecutor(new NationCommand(this));
        getCommand("plot").setExecutor(new PlotCommand(this));
        
        TownyWar.onEnable();
        
        if (TownySettings.isTownyUpdating(getVersion()))
            update();
        
        registerEvents();
        
        TownyLogger.log.info("=============================================================");
        TownyLogger.log.info("[Towny] Version: " + version + " - Mod Enabled");
        TownyLogger.log.info("=============================================================");
        
        // Re login anyone online. (In case of plugin reloading)
        for (Player player : getServer().getOnlinePlayers())
            try {
                getTownyUniverse().onLogin(player);
            } catch (TownyException x) {
            	TownyMessaging.sendErrorMsg(player, x.getError());
            }
        //setupDatabase();
    }
        
    public boolean isOnline(String playerName) {
        
        for (Player player : getServer().getOnlinePlayers())
                if (player.getName().equalsIgnoreCase(playerName))
                        return true;
                        
        return false;
        
    }
        
    public void SetWorldFlags () {
                
    	for (Town town : getTownyUniverse().getTowns()) {
    		TownyMessaging.sendDebugMsg("[Towny] Setting flags for: " + town.getName());
            
            if (town.getWorld() == null) {
            	TownyLogger.log.warning("[Towny Error] Detected an error with the world files. Attempting to repair");
                if (town.hasHomeBlock())
                	try {
                    	TownyWorld world = town.getHomeBlock().getWorld();
                    	if (!world.hasTown(town)) {
                    		world.addTown(town);
                    		TownyUniverse.getDataSource().saveTown(town);
							TownyUniverse.getDataSource().saveWorld(world);
                    	}                                               
                    } catch (TownyException e) {
                        // Error fetching homeblock
                    	TownyLogger.log.warning("[Towny Error] Failed get world data for: " + town.getName());
                    }
                else
                	TownyLogger.log.warning("[Towny Error] No Homeblock - Failed to detect world for: " + town.getName());     
            }
        }
                        
    }

    /*
    private void setupDatabase()
    {
            try
            {
            getDatabase().find(Towny.class).findRowCount();
            }
            catch(PersistenceException ex)
            {
                    System.out.println("Installing database for " + getDescription().getName() + " due to first time usage");
                    installDDL();
            }
    }
    */
    
    @Override
    public List<Class<?>> getDatabaseClasses()
    {
            List<Class<?>> list = new ArrayList<Class<?>>();
            list.add(Towny.class);
            return list;
    }
    
    private void checkPlugins() {
        List<String> using = new ArrayList<String>();
        Plugin test;

        if (TownySettings.isUsingPermissions()) {
            test = getServer().getPluginManager().getPlugin("GroupManager");
            if (test != null) {
            	//groupManager = (GroupManager)test;
            	this.getTownyUniverse().setPermissionSource(new GroupManagerSource(this, test));
                using.add(String.format("%s v%s", "GroupManager", test.getDescription().getVersion()));
            } else {
            	test = getServer().getPluginManager().getPlugin("PermissionsEx");
            	if (test != null) {
            		//permissions = (PermissionsEX)test;
            		getTownyUniverse().setPermissionSource(new PEXSource(this, test));
            		using.add(String.format("%s v%s", "PermissionsEX", test.getDescription().getVersion()));
            	} else {
	            	test = getServer().getPluginManager().getPlugin("bPermissions");
	            	if (test != null) {
	            		//permissions = (Permissions)test;
	            		getTownyUniverse().setPermissionSource(new bPermsSource(this, test));
	            		using.add(String.format("%s v%s", "bPermissions", test.getDescription().getVersion()));
	            	} else {
		            	test = getServer().getPluginManager().getPlugin("Permissions");
		            	if (test != null) {
		            		//permissions = (Permissions)test;
		            		getTownyUniverse().setPermissionSource(new Perms3Source(this, test));
		            		using.add(String.format("%s v%s", "Permissions", test.getDescription().getVersion()));
		            	} else {
		            		getTownyUniverse().setPermissionSource(new BukkitPermSource(this));
		            		using.add("BukkitPermissions");
		            	}
	            	}
            	}
            }
        } else {
        	// Not using Permissions
        	getTownyUniverse().setPermissionSource(new NullPermSource(this));
        }
        
        
        if (TownySettings.isUsingEconomy()) {
            test = getServer().getPluginManager().getPlugin("Register");
            if (test != null) {
                register = (Register)test;
                using.add(String.format("%s v%s", "Register", test.getDescription().getVersion()));
            } else {
            	test = getServer().getPluginManager().getPlugin("iConomy");
                if (test == null) {
                    //TownySettings.setUsingIConomy(false);
                	TownyMessaging.sendErrorMsg("No compatible Economy plugins found. You need iConomy 5.01, or the Register.jar with iConomy, BOSE or Essentials Eco.");
                } else {
                	if (!test.getDescription().getVersion().matches("5.01")) {
                		TownyMessaging.sendErrorMsg("Towny does not have native support for iConomy " + test.getDescription().getVersion() + ". You need the Register.jar.");
                	} else {
                        iconomy = (iConomy)test;
                        using.add(String.format("%s v%s", "iConomy", test.getDescription().getVersion()));
                	}
                }
            }
        }
        
        test = getServer().getPluginManager().getPlugin("Essentials");
        if (test == null)
            TownySettings.setUsingEssentials(false);
        else if (TownySettings.isUsingEssentials())
            using.add(String.format("%s v%s", "Essentials", test.getDescription().getVersion()));
        
        test = getServer().getPluginManager().getPlugin("Questioner");
        if (test == null)
            TownySettings.setUsingQuestioner(false);
        else if (TownySettings.isUsingQuestioner())
            using.add(String.format("%s v%s", "Questioner", test.getDescription().getVersion()));
        
        if (using.size() > 0)
        	TownyLogger.log.info("[Towny] Using: " + StringMgmt.join(using, ", "));
    }
    
    // is permissions active
    public boolean isPermissions() {
            return TownySettings.isUsingPermissions();
    }
    
    // is GroupManager active
    public boolean isEcoActive() {
            return isRegister() || isIConomy();
    }
    
    // is register active
    public boolean isRegister() {
            return (TownySettings.isUsingEconomy() && register != null);
    }
    
    // is iConomy active
    public boolean isIConomy() {
            return (TownySettings.isUsingEconomy() && iconomy != null);
    }

    @Override
    public void onDisable() {
    	
    	System.out.println("==============================================================");
    	
    	if (TownyUniverse.getDataSource() != null && error == false)
            TownyUniverse.getDataSource().saveAll();
    
	    if (error == false) TownyWar.onDisable();
	    
	    if (getTownyUniverse().isWarTime())
            getTownyUniverse().getWarEvent().toggleEnd();
	    townyUniverse.toggleTownyRepeatingTimer(false);
	    townyUniverse.toggleDailyTimer(false);
	    townyUniverse.toggleMobRemoval(false);
	    townyUniverse.toggleHealthRegen(false);
	    townyUniverse.toggleTeleportWarmup(false);
	    townyUniverse.cancelProtectionRegenTasks();
	    
	    playerCache.clear();
	    playerMode.clear();
	    
	    townyUniverse = null;
	    
	    
	    System.out.println("[Towny] Version: " + version + " - Mod Disabled");
	    System.out.println("=============================================================");
	    
	    TownyLogger.shutDown();
    }

    public boolean load() {
    	
    	Pattern pattern = Pattern.compile("-b(\\d*?)jnks", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(getServer().getVersion());
        
        if (!townyUniverse.loadSettings())  {
            error = true;
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        
        setupLogger();
        
        int bukkitVer = TownySettings.getMinBukkitVersion();
        
        if (!matcher.find() || matcher.group(1) == null) {
        	error = true;
        	TownyLogger.log.severe("[Towny Error] Unable to read CraftBukkit Version.");
        	TownyLogger.log.severe("[Towny Error] Towny requires version " + bukkitVer + " or higher.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        int curBuild = Integer.parseInt(matcher.group(1));
        
        if (curBuild < bukkitVer){
            error = true;
            TownyLogger.log.severe("[Towny Error] CraftBukkit Version ("  + curBuild + ") is outdated! ");
            TownyLogger.log.severe("[Towny Error] Towny requires version " + bukkitVer + " or higher.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        
        //Coord.setCellSize(TownySettings.getTownBlockSize());
        TownyEconomyObject.setPlugin(this);
        //TownyCommand.setUniverse(townyUniverse);
        
        checkPlugins();
        
        SetWorldFlags();
        
        //make sure the timers are stopped for a reset
        townyUniverse.toggleTownyRepeatingTimer(false);
        townyUniverse.toggleDailyTimer(false);
        townyUniverse.toggleMobRemoval(false);
        townyUniverse.toggleHealthRegen(false);
        townyUniverse.toggleTeleportWarmup(false);
        
        //Start timers
        townyUniverse.toggleTownyRepeatingTimer(true);
        townyUniverse.toggleDailyTimer(true);
        townyUniverse.toggleMobRemoval(true);
        townyUniverse.toggleHealthRegen(TownySettings.hasHealthRegen());
        townyUniverse.toggleTeleportWarmup(TownySettings.getTeleportWarmupTime() > 0);
        updateCache();
        
        return true;
    }

    private void registerEvents() {
        
        final PluginManager pluginManager = getServer().getPluginManager();
        
        pluginManager.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);

        pluginManager.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Normal, this);

        pluginManager.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);

        // Have War Events get launched before regular events.
        pluginManager.registerEvent(Event.Type.BLOCK_BREAK, townyWarBlockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.CUSTOM_EVENT, customListener, Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.BLOCK_BURN, townyWarBlockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.BLOCK_PISTON_EXTEND, townyWarBlockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.ENTITY_EXPLODE, townyWarEntityListener, Priority.Lowest, this);
        
        pluginManager.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.BLOCK_IGNITE, blockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.BLOCK_BURN, blockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.BLOCK_PISTON_EXTEND, blockListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.BLOCK_PISTON_RETRACT, blockListener, Priority.Lowest, this);
        //getServer().getPluginManager().registerEvent(Event.Type.BLOCK_INTERACT, blockListener, Priority.Normal, this);

        pluginManager.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.ENDERMAN_PICKUP, entityListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.ENDERMAN_PLACE, entityListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.ENTITY_INTERACT, entityListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.ENTITY_DAMAGE, entityMonitorListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Lowest, this);
        pluginManager.registerEvent(Event.Type.PAINTING_BREAK, entityListener, Priority.Normal, this);
        
        pluginManager.registerEvent(Event.Type.WORLD_INIT, worldListener, Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Normal, this);
    }
        
    private void update() {
        try {
                List<String> changeLog = JavaUtil.readTextFromJar("/ChangeLog.txt");
                boolean display = false;
                TownyLogger.log.info("------------------------------------");
                TownyLogger.log.info("[Towny] ChangeLog up until v" + getVersion());
                String lastVersion = TownySettings.getLastRunVersion(getVersion());
                for (String line : changeLog) { //TODO: crawl from the bottom, then past from that index.
                        if (line.startsWith("v" + lastVersion))
                                display = true;
                        if (display && line.replaceAll(" ", "").replaceAll("\t", "").length() > 0)
                        	TownyLogger.log.info(line);
                }
                TownyLogger.log.info("------------------------------------");
        } catch (IOException e) {
        	TownyMessaging.sendDebugMsg("Could not read ChangeLog.txt");
        }
        TownySettings.setLastRunVersion(getVersion());
    }


    public TownyUniverse getTownyUniverse() {
        return townyUniverse;
    }


        public String getVersion() {
                return version;
        }

        public World getServerWorld(String name) throws NotRegisteredException {
                for (World world : getServer().getWorlds())
                        if (world.getName().equals(name))
                                return world;

                throw new NotRegisteredException();
        }
        
        public boolean hasCache(Player player) {
                return playerCache.containsKey(player.getName().toLowerCase());
        }
        
        public void newCache(Player player) {
                try {
                        getTownyUniverse();
						playerCache.put(player.getName().toLowerCase(), new PlayerCache(TownyUniverse.getWorld(player.getWorld().getName()), player));
                } catch (NotRegisteredException e) {
                	TownyMessaging.sendErrorMsg(player, "Could not create permission cache for this world ("+player.getWorld().getName()+".");
                }
                
        }
        
        public void deleteCache(Player player) {
                deleteCache(player.getName());
        }
        
        
        public void deleteCache(String name) {
                playerCache.remove(name.toLowerCase());
        }
        
        public PlayerCache getCache(Player player) {
                if (!hasCache(player)) {
                    newCache(player);
                    try {
                        getTownyUniverse();
						getCache(player).setLastTownBlock(new WorldCoord(TownyUniverse.getWorld(player.getWorld().getName()), Coord.parseCoord(player)));
                    } catch (NotRegisteredException e) {
                        deleteCache(player);
                    }
                }
                
                return playerCache.get(player.getName().toLowerCase());
        }
        
        public void updateCache(WorldCoord worldCoord) {
                for (Player player : getServer().getOnlinePlayers())
                        if (Coord.parseCoord(player).equals(worldCoord))
                                getCache(player).setLastTownBlock(worldCoord); //Automatically resets permissions.
        }
        
        public void updateCache() {
                for (Player player : getServer().getOnlinePlayers())
                        try {
                                getTownyUniverse();
								getCache(player).setLastTownBlock(new WorldCoord(TownyUniverse.getWorld(player.getWorld().getName()), Coord.parseCoord(player)));
                        } catch (NotRegisteredException e) {
                                deleteCache(player);
                        }
        }
        
        public boolean isTownyAdmin(Player player) {
                if (player.isOp())
                        return true;
                return TownyUniverse.getPermissionSource().hasPermission(player, "towny.admin");
        }
        
        
        
        public void setPlayerMode(Player player, String[] modes) {
                playerMode.put(player.getName(), Arrays.asList(modes));
                TownyMessaging.sendMsg(player, ("Modes set: " + StringMgmt.join(modes, ",")));
        }
        
        public void removePlayerMode(Player player) {
                playerMode.remove(player.getName());
                TownyMessaging.sendMsg(player, ("Mode removed."));
        }
        
        public List<String> getPlayerMode(Player player) {
                return playerMode.get(player.getName());
        }
        
        public boolean hasPlayerMode(Player player, String mode) {
                List<String> modes = getPlayerMode(player);
                if (modes == null)
                        return false;
                else
                        return modes.contains(mode); 
        }
        
        public List<String> getPlayerMode(String name) {
                return playerMode.get(name);
        }
        
        public boolean hasPlayerMode(String name, String mode) {
                List<String> modes = getPlayerMode(name);
                if (modes == null)
                        return false;
                else
                        return modes.contains(mode); 
        }

        /*
        public boolean checkEssentialsTeleport(Player player, Location lctn) {
                if (!TownySettings.isUsingEssentials() || !TownySettings.isAllowingTownSpawn())
                        return false;
                
                Plugin test = getServer().getPluginManager().getPlugin("Essentials");
                if (test == null)
                        return false;
                Essentials essentials = (Essentials)test;
                //essentials.loadClasses();
                sendDebugMsg("Using Essentials");
                
                try {
                        User user = essentials.getUser(player);
                        
                        if (!user.isTeleportEnabled())
                                return false;
                                                
                        if (!user.isJailed()){

                                //user.getTeleport();
                                Teleport teleport = user.getTeleport();
                                teleport.teleport(lctn, null);

                        }
                        return true;
                        
                } catch (Exception e) {
                        sendErrorMsg(player, "Error: " + e.getMessage());
                        // we still retun true here as it is a cooldown
                        return true;
                }
                
        }
        */        
        
        
        
        public String getConfigPath() {
                return getDataFolder().getPath() + FileMgmt.fileSeparator() + "settings" + FileMgmt.fileSeparator() + "config.yml";
        }
        
        //public void setSetting(String root, Object value, boolean saveYML) {
        //              TownySettings.setProperty(root, value, saveYML);
        //}
        
        public Object getSetting(String root) {
                return TownySettings.getProperty(root);
        }

        public iConomy getIConomy() throws EconomyException {
                if (iconomy == null)
                        throw new EconomyException("iConomy is not installed");
                else
                        return iconomy;    
        }

        /**
		 * @return the register
		 */
		public Register getRegister() throws EconomyException{
			if (iconomy == null)
                throw new EconomyException("Economy is not installed");
			else
				return register;
		}

		/**
		 * @param register the register to set
		 */
		public void setRegister(Register register) {
			this.register = register;
		}
        
        public void log(String msg) {
            if (TownySettings.isLogging())
            	TownyLogger.log.info(ChatColor.stripColor(msg));
        }
        
        public void setupLogger() {
        	TownyLogger.setup(getTownyUniverse().getRootFolder(), TownySettings.isAppendingToLog());
        }
        
        public boolean hasWildOverride(TownyWorld world, Player player, int blockId, TownyPermission.ActionType action) {
        	
        	//check for permissions
        	
        	if (TownyUniverse.getPermissionSource().hasPermission(player, "towny.wild." + action.toString().toLowerCase())
                	|| TownyUniverse.getPermissionSource().hasPermission(player, "towny.wild.block." + blockId + "." + action.toString().toLowerCase()))
                	return true;
            
        	// No perms found so check world settings.
        	switch(action) {
        	
        	case BUILD:
            	return world.getUnclaimedZoneBuild() || world.isUnclaimedZoneIgnoreId(blockId);
        	case DESTROY:
            	return world.getUnclaimedZoneDestroy() || world.isUnclaimedZoneIgnoreId(blockId);
        	case SWITCH:
            	return world.getUnclaimedZoneSwitch() || world.isUnclaimedZoneIgnoreId(blockId);
        	case ITEM_USE:
            	return world.getUnclaimedZoneItemUse() || world.isUnclaimedZoneIgnoreId(blockId);
            default:
            	return false;
        	}
        }
        
        public void appendQuestion(Questioner questioner, Question question) throws Exception {
                for (Option option : question.getOptions())
                        if (option.getReaction() instanceof TownyQuestionTask)
                                ((TownyQuestionTask)option.getReaction()).setTowny(this);
                questioner.appendQuestion(question);
        }
        
        public boolean parseOnOff(String s) throws Exception {
                if (s.equalsIgnoreCase("on"))
                        return true;
                else if (s.equalsIgnoreCase("off"))
                        return false;
                else
                        throw new Exception(String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
        }
}
