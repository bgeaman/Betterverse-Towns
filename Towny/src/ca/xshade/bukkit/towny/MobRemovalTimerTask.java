package ca.xshade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.util.JavaUtil;


public class MobRemovalTimerTask extends TownyTimerTask {
	private Server server;
	@SuppressWarnings("unchecked")
	public static List<Class> mobsToRemove = new ArrayList<Class>();
	
	@SuppressWarnings("unchecked")
	public MobRemovalTimerTask(TownyUniverse universe, Server server) {
		super(universe);
		this.server = server;
		mobsToRemove.clear();
		for (String mob : TownySettings.getMobRemovalEntities())
			try {
				Class c = Class.forName("org.bukkit.entity."+mob);
				if (JavaUtil.isSubInterface(LivingEntity.class, c))
					mobsToRemove.add(c);
				else
					throw new Exception();
			} catch (ClassNotFoundException e) {
				plugin.sendErrorMsg(mob + " is not an acceptable class.");
			} catch (Exception e) {
				plugin.sendErrorMsg(mob + " is not an acceptable living entity.");
			}
	}
	
	@SuppressWarnings("unchecked")
	public static boolean isRemovingEntity(LivingEntity livingEntity) {
		for (Class c : mobsToRemove)
			if (c.isInstance(livingEntity))
				return true;
			else if (c.getName().contains(livingEntity.toString()))
				System.out.print(livingEntity.toString());
		return false;
	}
	
	
	@Override
	public void run() {
		int numRemoved = 0;
		int livingEntities = 0;
		
		/* OLD METHOD
		for (World world : server.getWorlds()) {
			List<LivingEntity> worldLivingEntities = new ArrayList<LivingEntity>(world.getLivingEntities());
			livingEntities += worldLivingEntities.size();
			for (LivingEntity livingEntity : worldLivingEntities)
				if (isRemovingEntity(livingEntity)) {
					Location loc = livingEntity.getLocation();
					Coord coord = Coord.parseCoord(loc);
					try {
						TownyWorld townyWorld = universe.getWorld(world.getName());
						TownBlock townBlock = townyWorld.getTownBlock(coord);
						if (!townBlock.getTown().hasMobs()) {
							//universe.getPlugin().sendDebugMsg("MobRemoval Removed: " + livingEntity.toString());
							livingEntity.teleportTo(new Location(world, loc.getX(), -50, loc.getZ()));
							numRemoved++;
						}
					} catch (TownyException x) {
					}
				}
			//universe.getPlugin().sendDebugMsg(world.getName() + ": " + StringMgmt.join(worldLivingEntities));
		}
		//universe.getPlugin().sendDebugMsg("MobRemoval (Removed: "+numRemoved+") (Total Living: "+livingEntities+")");
		*/
		
		
		for (World world : server.getWorlds()) {
			List<LivingEntity> livingEntitiesToRemove = new ArrayList<LivingEntity>();
			livingEntities += world.getLivingEntities().size();
			for (LivingEntity livingEntity : world.getLivingEntities())
				if (isRemovingEntity(livingEntity)) {
					Coord coord = Coord.parseCoord(livingEntity.getLocation());
					try {
						TownyWorld townyWorld = universe.getWorld(world.getName());
						TownBlock townBlock = townyWorld.getTownBlock(coord);
						if (!townBlock.getTown().hasMobs())
							livingEntitiesToRemove.add(livingEntity);
					} catch (TownyException x) {
					}
				}
			for (LivingEntity livingEntity : livingEntitiesToRemove) {
				universe.getPlugin().sendDebugMsg("MobRemoval Removed: " + livingEntity.toString());
				//livingEntity.teleportTo(new Location(world, livingEntity.getLocation().getX(), -50, livingEntity.getLocation().getZ()));
				livingEntity.remove();
				numRemoved++;
			}
			//universe.getPlugin().sendDebugMsg(world.getName() + ": " + StringMgmt.join(worldLivingEntities));
		}
		
	}
}
