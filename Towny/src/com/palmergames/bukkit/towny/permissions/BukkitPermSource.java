package com.palmergames.bukkit.towny.permissions;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;


public class BukkitPermSource extends TownyPermissionSource {
	
	public BukkitPermSource(Towny towny) {
		this.plugin = towny;
	}

	@Override
	public String getPrefixSuffix(Resident resident, String node) {
		/*
		 *  Bukkit doesn't support prefix/suffix
		 *  so treat the same as bPerms
    	 */
    	
    	Player player = plugin.getServer().getPlayer(resident.getName());
    	
    	for (PermissionAttachmentInfo test: player.getEffectivePermissions()) {
    		if (test.getPermission().startsWith(node+".")) {
    			String[] split = test.getPermission().split("\\.");
    				return split[split.length-1];
    		}
    	}
		return "";
	}
	
	/**
     * 
     * @param playerName
     * @param node
     * @return -1 = can't find
     */
    @Override
	public int getGroupPermissionIntNode(String playerName, String node) {
    	/*
    	 *  Bukkit doesn't support non boolean nodes
    	 *  so treat the same as bPerms
    	 */
    	
    	Player player = plugin.getServer().getPlayer(playerName);
    	
    	for (PermissionAttachmentInfo test: player.getEffectivePermissions()) {
    		if (test.getPermission().startsWith(node+".")) {
    			String[] split = test.getPermission().split("\\.");
    			try {
    				return Integer.parseInt(split[split.length-1]);
    			} catch (NumberFormatException e) {
    			}
    		}
    	}
    	
    	return -1;
    }
	
	
    /** hasPermission
     * 
     * returns if a player has a certain permission node.
     * 
     * @param player
     * @param node
     * @return
     */
    @Override
	public boolean hasPermission(Player player, String node) {
    	return player.hasPermission(node);
    }
    
    /**
     * Returns the players Group name.
     * 
     * @param player
     * @return
     */
    @Override
	public String getPlayerGroup(Player player) {

    	//BukkitPermissions doesn't support groups.
    	return "";
		
    }
	
}