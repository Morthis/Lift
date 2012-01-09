package net.croxis.plugins.lift;

import java.util.ArrayList;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.block.Sign;
import org.getspout.spoutapi.SpoutManager;

public class LiftRedstoneListener  extends BlockListener {
	private final Lift plugin; 
	public LiftRedstoneListener(Lift instance){
		this.plugin = instance;
	} 
	public void onBlockRedstoneChange(BlockRedstoneEvent event){
		Block block = event.getBlock();
		Elevator elevator = null;
		if ((block.getType() == Material.STONE_BUTTON) 
				&& (!block.isBlockIndirectlyPowered())
				&& block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN){
			long startTime = System.currentTimeMillis();
			elevator = new Elevator(this.plugin, block);
			
			//See if lift is in use
			for (Elevator e : plugin.lifts){
				if (e.floorBlocks.contains(elevator.floorBlocks.toArray()[0]))
					return;
			}
			
			if (elevator.getTotalFloors() < 2)
				return;
			
			int y = block.getY();
			Floor startFloor = elevator.getFloormap().get(y);
			elevator.startFloor = startFloor;
			String line = ((Sign) block.getRelative(BlockFace.UP).getState()).getLine(2);
			if (line.isEmpty())
				return;
			String[] splits = line.split(": ");
			if (splits.length != 2)
				return;
			int destination = Integer.parseInt(splits[1]);	
			
			Floor destFloor = elevator.getFloorFromN(destination);

			//Get all players in elevator shaft (at floor of button pusher if possible)
			//And set their gravity to 0
			elevator.destFloor = destFloor;
			
			if (plugin.debug){
				System.out.println("Elevator start floor:" + startFloor.getFloor());
				System.out.println("Elevator destination floor:" + destination);
				System.out.println("Elevator destination y:" + destination);
			}
			
			for(Chunk chunk : elevator.chunks){
				for(Entity e : chunk.getEntities()){
					if (elevator.isInShaftAtFloor(e, startFloor) && e instanceof LivingEntity)
						elevator.addPassenger((LivingEntity) e);
				}
			}
			
			//Disable all glass inbetween players and destination
			ArrayList<Floor> glassfloors = new ArrayList<Floor>();
			//Going up
			if (startFloor.getY() < destFloor.getY()){
				for(int i = startFloor.getFloor() + 1; i<= destFloor.getFloor(); i++){
					glassfloors.add(elevator.getFloormap2().get(i));
				}
			}
			//Going down
			else {
				for(int i = destFloor.getFloor() + 1; i<= startFloor.getFloor(); i++){
					glassfloors.add(elevator.getFloormap2().get(i));
				}
			}
			for (Floor f : glassfloors){
				for (Block b : elevator.floorBlocks){
					Block gb = block.getWorld().getBlockAt(b.getX(), f.getY()-2, b.getZ());
					gb.setType(Material.AIR);
					elevator.glassBlocks.add(gb);
				}
			}
			//Apply impulse to players
			for (Entity p : elevator.getPassengers()){
				if (plugin.useSpout){
					if (p instanceof Player){
						SpoutManager.getPlayer((Player) p).setGravityMultiplier(0);
						SpoutManager.getPlayer((Player) p).setCanFly(true);
					}
						
				}
				if (destFloor.getY() > startFloor.getY()){
					elevator.goingUp = true;
				} else {
					plugin.fallers.add(p);
				}
			}
			elevator.taskid = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, elevator, 2, 2);
			plugin.lifts.add(elevator);

			if (plugin.debug)
				System.out.println("Total time: " + Long.toString(System.currentTimeMillis() - startTime));
		}
		
	}
	
}