package com.palmergames.bukkit.townywar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUtil;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.townywar.event.CellAttackCanceledEvent;
import com.palmergames.bukkit.townywar.event.CellAttackEvent;
import com.palmergames.bukkit.townywar.event.CellDefendedEvent;
import com.palmergames.bukkit.townywar.event.CellWonEvent;


public class TownyWar {
	private static Map<Cell, CellUnderAttack> cellsUnderAttack;
	private static Map<String, List<CellUnderAttack>> cellsUnderAttackByPlayer;
	
	public static void onEnable() {
		cellsUnderAttack = new HashMap<Cell, CellUnderAttack>();
		cellsUnderAttackByPlayer = new HashMap<String, List<CellUnderAttack>>();
	}
	
	public static void onDisable() {
		for (CellUnderAttack cell : cellsUnderAttack.values()) {
			cell.destroyFlag();
		}
	}
	
	public static void registerAttack(CellUnderAttack cell) throws Exception {
		CellUnderAttack currentData = cellsUnderAttack.get(cell); 
		if (currentData != null)
			throw new Exception(String.format(TownySettings.getLangString("msg_err_enemy_war_cell_already_under_attack"), currentData.getNameOfFlagOwner()));
		String playerName = cell.getNameOfFlagOwner();
		List<CellUnderAttack> activeFlags = cellsUnderAttackByPlayer.get(playerName);
		int activeFlagCount = activeFlags == null ? 0 : activeFlags.size(); 
		if (activeFlagCount + 1 > TownyWarConfig.getMaxActiveFlagsPerPerson())
			throw new Exception(String.format(TownySettings.getLangString("msg_err_enemy_war_reached_max_active_flags"), TownyWarConfig.getMaxActiveFlagsPerPerson()));
		
		addFlagToPlayerCount(playerName, cell);
		cellsUnderAttack.put(cell, cell);
		cell.begin();
	}

	public static boolean isUnderAttack(Cell cell) {
		return cellsUnderAttack.containsKey(cell);
	}

	public static CellUnderAttack getAttackData(Cell cell) {
		return cellsUnderAttack.get(cell);
	}
	
	public static void removeCellUnderAttack(CellUnderAttack cell) {
		removeFlagFromPlayerCount(cell.getNameOfFlagOwner(), cell);
		cellsUnderAttack.remove(cell);
	}

	public static void attackWon(CellUnderAttack cell) {
		CellWonEvent cellWonEvent = new CellWonEvent(cell);
		Bukkit.getServer().getPluginManager().callEvent(cellWonEvent);
		cell.cancel();
		removeCellUnderAttack(cell);
	}
	
	public static void attackDefended(Player player, CellUnderAttack cell) {
		CellDefendedEvent cellDefendedEvent = new CellDefendedEvent(player, cell);
		Bukkit.getServer().getPluginManager().callEvent(cellDefendedEvent);
		cell.cancel();
		removeCellUnderAttack(cell);
	}
	
	public static void attackCanceled(CellUnderAttack cell) {
		CellAttackCanceledEvent cellAttackCanceledEvent = new CellAttackCanceledEvent(cell);
		Bukkit.getServer().getPluginManager().callEvent(cellAttackCanceledEvent);
		cell.cancel();
		removeCellUnderAttack(cell);
	}
	
	public static void removeAttackerFlags(String playerName) {
		List<CellUnderAttack> cells = cellsUnderAttackByPlayer.get(playerName);
		if (cells != null)
			for (CellUnderAttack cell : cells)
				attackCanceled(cell);
	}
	
	public static List<CellUnderAttack> getCellsUnderAttackByPlayer(String playerName) {
		List<CellUnderAttack> cells = cellsUnderAttackByPlayer.get(playerName);
		if (cells == null)
			return null;
		else
			return new ArrayList<CellUnderAttack>(cells);
	}
	
	private static void addFlagToPlayerCount(String playerName, CellUnderAttack cell) {
		List<CellUnderAttack> activeFlags = getCellsUnderAttackByPlayer(playerName);
		if (activeFlags == null)
			activeFlags = new ArrayList<CellUnderAttack>();
		
		activeFlags.add(cell);
		cellsUnderAttackByPlayer.put(playerName, activeFlags);
	}
	
	private static void removeFlagFromPlayerCount(String playerName, Cell cell) {
		List<CellUnderAttack> activeFlags = cellsUnderAttackByPlayer.get(playerName);
		if (activeFlags != null) {
			if (activeFlags.size() <= 1)
				cellsUnderAttackByPlayer.remove(playerName);
			else {
				activeFlags.remove(cell);
				cellsUnderAttackByPlayer.put(playerName, activeFlags);
			}
		}
	}
	
	public static void checkBlock(Player player, Block block, Cancellable event) {
		if (TownyWarConfig.isAffectedMaterial(block.getType())) {
			Cell cell = Cell.parse(block.getLocation());
			if (cell.isUnderAttack()) {
				CellUnderAttack cellAttackData = cell.getAttackData();
				if (cellAttackData.isFlag(block)) {
					TownyWar.attackDefended(player, cellAttackData);
					event.setCancelled(true);
				} else if (cellAttackData.isUneditableBlock(block)) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	public static boolean callAttackCellEvent(Towny plugin, Player player, Block block, WorldCoord worldCoord) throws TownyException {
		int topY = block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()) - 1;
		if (block.getY() < topY)
			throw new TownyException(TownySettings.getLangString("msg_err_enemy_war_must_be_placed_above_ground"));
		
		TownyUniverse universe = plugin.getTownyUniverse();
		Town landOwnerTown;
		Nation landOwnerNation;
		try {
			landOwnerTown = worldCoord.getTownBlock().getTown();
			landOwnerNation = landOwnerTown.getNation();
		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_err_enemy_war_not_part_of_nation"));
		}
		
		if (landOwnerNation.isNeutral())
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_enemy_war_is_neutral"), landOwnerNation.getFormattedName()));
		
		int onlinePlayerCount = universe.getOnlinePlayers(landOwnerTown).size();
		int requiredOnline = TownyWarConfig.getMinPlayersOnlineInTownForWar();
		if (onlinePlayerCount < requiredOnline)
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_enemy_war_require_online_in_town"), requiredOnline));
		onlinePlayerCount = universe.getOnlinePlayers(landOwnerNation).size();
		requiredOnline = TownyWarConfig.getMinPlayersOnlineInNationForWar();
		if (onlinePlayerCount < requiredOnline)
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_enemy_war_require_online_in_nation"), requiredOnline));
		
		if (!TownyUtil.isOnEdgeOfOwnership(landOwnerTown, worldCoord))
			throw new TownyException(TownySettings.getLangString("msg_err_enemy_war_not_on_edge_of_town"));
		
		if (!plugin.isTownyAdmin(player)) {
			Nation attackerNation = plugin.getTownyUniverse().getResident(player.getName()).getTown().getNation();
			if (attackerNation.isNeutral())
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_enemy_war_is_neutral"), attackerNation.getFormattedName()));
		}
		
		CellAttackEvent cellAttackEvent = new CellAttackEvent(player, block);
		plugin.getServer().getPluginManager().callEvent(cellAttackEvent);
		
		if (cellAttackEvent.isCancelled()) {
			if (cellAttackEvent.hasReason())
				throw new TownyException(cellAttackEvent.getReason());
			else
				return false;
		}
		
		universe.addWarZone(worldCoord);
		plugin.updateCache(worldCoord);
		
		String playerName = player.getName();
		try {
			playerName = plugin.getTownyUniverse().getResident(player.getName()).getFormattedName();
		} catch (TownyException e) {
		}
		universe.sendGlobalMessage(String.format(TownySettings.getLangString("msg_enemy_war_area_under_attack"), landOwnerTown.getFormattedName(), worldCoord.toString(), playerName));
		return true;
	}
}
