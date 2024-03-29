package com.palmergames.bukkit.towny.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;

import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.war.War;
import com.palmergames.bukkit.towny.war.WarSpoils;

public class TownyEntityMonitorListener extends EntityListener {
        private final Towny plugin;

        public TownyEntityMonitorListener(Towny instance) {
                plugin = instance;
        }
        
        @Override
        public void onEntityDamage(EntityDamageEvent event) {
                if (event instanceof EntityDamageByEntityEvent) {
                        EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent)event;
                        Entity attackerEntity = entityEvent.getDamager();
                        Entity defenderEntity = entityEvent.getEntity();
                        
                        if (defenderEntity instanceof Player) {
                                Player defenderPlayer = (Player) defenderEntity;
                                Player attackerPlayer = null;
                                if (defenderPlayer.getHealth() > 0)
                                        return;
                                
                                Resident attackerResident = null;
                                Resident defenderResident = null;
                                
                                try {
                                        defenderResident = plugin.getTownyUniverse().getResident(defenderPlayer.getName());
                                } catch (NotRegisteredException e) {
                                        return;
                                }
                                
                                if (attackerEntity instanceof Player) {
                                        attackerPlayer = (Player) attackerEntity;
                                        try {
                                                attackerResident = plugin.getTownyUniverse().getResident(attackerPlayer.getName());
                                        } catch (NotRegisteredException e) {
                                        }
                                }
                                
                                deathPayment(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
                                wartimeDeathPoints(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
                                
                                if (TownySettings.isRemovingOnMonarchDeath())
                                        monarchDeath(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
                        }
                }
        }
        
        private void wartimeDeathPoints(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
                if (attackerPlayer != null && plugin.getTownyUniverse().isWarTime())
                        try {
                                if (attackerResident == null)
                                        throw new NotRegisteredException();
                                
                                Town town = attackerResident.getTown();
                                if (TownySettings.getWarPointsForKill() > 0)
                                        plugin.getTownyUniverse().getWarEvent().townScored(town, TownySettings.getWarPointsForKill());
                        } catch (NotRegisteredException e) {
                        }
        }

        private void monarchDeath(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
                if (plugin.getTownyUniverse().isWarTime()) {
                        War warEvent = plugin.getTownyUniverse().getWarEvent();
                        try {
                                Nation defenderNation = defenderResident.getTown().getNation();
                                if (warEvent.isWarringNation(defenderNation))
                                        if (defenderResident.isMayor())
                                                if (defenderResident.isKing()) {
                                                        if (attackerResident != null && attackerResident.hasTown())
                                                                warEvent.remove(attackerResident.getTown(), defenderNation);
                                                        else
                                                                warEvent.remove(defenderNation);
                                                        TownyMessaging.sendGlobalMessage(defenderNation.getName() + "'s king was killed. Nation removed from war.");
                                                } else {
                                                        if (attackerResident != null && attackerResident.hasTown())
                                                                warEvent.remove(attackerResident.getTown(), defenderResident.getTown());
                                                        else
                                                                warEvent.remove(defenderResident.getTown());
                                                        TownyMessaging.sendGlobalMessage(defenderResident.getTown() + "'s mayor was killed. Town removed from war.");
                                                }
                        } catch (NotRegisteredException e) {
                        }
                }
        }

        public void deathPayment(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
                if (attackerPlayer != null && plugin.getTownyUniverse().isWarTime() && TownySettings.getWartimeDeathPrice() > 0)
                        try {
                                if (attackerResident == null)
                                        throw new NotRegisteredException();
                                
                                double price = TownySettings.getWartimeDeathPrice();
                                double townPrice = 0;
                                if (!defenderResident.canPayFromHoldings(price)) 
                                {
                                        townPrice = price - defenderResident.getHoldingBalance();
                                        price = defenderResident.getHoldingBalance();
                                }
                                
                                if (price > 0) {
                                        defenderResident.payTo(price, attackerResident, "Death Payment (War)");
                                        TownyMessaging.sendMsg(attackerPlayer, "You robbed " + defenderResident.getName() +" of " + price + " " + TownyEconomyObject.getEconomyCurrency() + ".");
                                        TownyMessaging.sendMsg(defenderPlayer, attackerResident.getName() + " robbed you of " + price + " " + TownyEconomyObject.getEconomyCurrency() + ".");
                                }
                                
                                // Resident doesn't have enough funds.
                                if (townPrice > 0) {
                                        Town town = defenderResident.getTown();
                                        if (!town.canPayFromHoldings(townPrice)) {
                                                // Town doesn't have enough funds.
                                                townPrice = town.getHoldingBalance();
                                                try {
                                                        plugin.getTownyUniverse().getWarEvent().remove(attackerResident.getTown(), town);
                                                } catch (NotRegisteredException e) {
                                                        plugin.getTownyUniverse().getWarEvent().remove(town);
                                                }
                                        } else
                                                TownyMessaging.sendTownMessage(town, defenderResident.getName() + "'s wallet couldn't satisfy " + attackerResident.getName() + ". "
                                                                + townPrice + " taken from town bank.");
                                        town.payTo(townPrice, attackerResident, String.format("Death Payment (War) (%s couldn't pay)", defenderResident.getName()));
                                }
                        } catch (NotRegisteredException e) {
                        } catch (EconomyException e) {
                        	TownyMessaging.sendErrorMsg(attackerPlayer, "Could not take wartime death funds.");
                        	TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take wartime death funds.");
                        }
                else if (TownySettings.getDeathPrice() > 0)
                        try {
                                double price = TownySettings.getDeathPrice();
                                if (!defenderResident.canPayFromHoldings(price))
                                        price = defenderResident.getHoldingBalance();
                        
                                defenderResident.payTo(price, new WarSpoils(), "Death Payment");
                                TownyMessaging.sendMsg(defenderPlayer, "You lost " + price + " " + TownyEconomyObject.getEconomyCurrency() + ".");
                        } catch (EconomyException e) {
                        	TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take death funds.");
                        }
        }

        
}
