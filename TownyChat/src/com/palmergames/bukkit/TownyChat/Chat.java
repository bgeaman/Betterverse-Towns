package com.palmergames.bukkit.TownyChat;

import java.util.logging.Logger;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.palmergames.bukkit.TownyChat.CraftIRCHandler;
import com.palmergames.bukkit.TownyChat.event.TownyPlayerHighestListener;
import com.palmergames.bukkit.TownyChat.tasks.onLoadedTask;
import com.palmergames.bukkit.towny.Towny;

/**
 * TownyChat plugin to manage all Towny chat
 * 
 * Website: http://code.google.com/a/eclipselabs.org/p/towny/
 * 
 * @author ElgarL
 */

public class Chat extends JavaPlugin {

	private Logger logger = Logger.getLogger("com.palmergames.bukkit.TownyChat");
	private TownyPlayerHighestListener TownyPlayerListener;

	protected PluginManager pm;
	private Towny towny = null;
	private CraftIRC craftIRC = null;
	
	private CraftIRCHandler irc = null;

	@Override
	public void onEnable() {

		pm = getServer().getPluginManager();

		checkPlugins();

		/*
		 * This executes the task with a 1 tick delay avoiding the bukkit
		 * depends bug.
		 */
		if ((towny == null) || (getServer().getScheduler().scheduleSyncDelayedTask(this, new onLoadedTask(this), 1) == -1)) {
			/*
			 * We either failed to find Towny or the Scheduler failed to
			 * register the task.
			 */
			logger.severe("Could not schedule onLoadedTask.");
			logger.severe("disabling TownyChat");
			pm.disablePlugin(this);
		}

	}

	@Override
	public void onDisable() {
		// reset any handles
		towny = null;
		pm = null;
		logger = null;
	}

	private void checkPlugins() {
		Plugin test;

		test = pm.getPlugin("Towny");
		if (test != null && test instanceof Towny)
			towny = (Towny) test;
		
		test = pm.getPlugin("CraftIRC");
		if (test != null) {
			try {
				if (Double.valueOf(test.getDescription().getVersion()) >= 3.1) {
					craftIRC = (CraftIRC) test;
					irc = new CraftIRCHandler(towny, craftIRC, "towny");
				} else
					logger.warning("TownyChat requires CraftIRC version 3.1 or higher to relay chat.");
			} catch (NumberFormatException e) {
				logger.warning("Non number format found for craftIRC version string!");
			}
		}

	}

	public void registerEvents() {
		TownyPlayerListener = new TownyPlayerHighestListener(towny, irc);

		pm.registerEvent(Event.Type.PLAYER_CHAT, TownyPlayerListener, Priority.Highest, this); //Run this lower so we go before herochat? ( it needs to see us cancel).
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, TownyPlayerListener, Priority.Highest, this);
	}

	public Logger getLogger() {
		return logger;
	}

	public Towny getTowny() {
		return towny;
	}

}