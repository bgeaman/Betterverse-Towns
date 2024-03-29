package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.ChatTools;

public class JoinTownTask extends ResidentTownQuestionTask {
	
	public JoinTownTask(Resident resident, Town town) {
		super(resident, town);
	}

	@Override
	public void run() {
		try {
			town.addResident(resident);
			towny.deleteCache(resident.getName());
			TownyUniverse.getDataSource().saveResident(resident);
			TownyUniverse.getDataSource().saveTown(town);
			
			TownyMessaging.sendTownMessage(town,  ChatTools.color(String.format(TownySettings.getLangString("msg_join_town"), resident.getName())));
		} catch (AlreadyRegisteredException e) {
			try {
				TownyMessaging.sendResidentMessage(resident, e.getError());
			} catch (TownyException e1) {
			}
		}
	}
}
