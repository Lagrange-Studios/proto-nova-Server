package tag;

import java.util.ArrayList;

import protonova.protobuf.EntityProto.Entity;
import util.VectorMath;

public class Enemy extends TagClass {

	public String getTag() {
		return "enemy";
	}
	
	public double getRange() {
		return 25;
	}
	
	public boolean canTarget(Entity target) {
		return target.getName().equals("human");
	}
	
	
	/*
	 * This tag will make the entity act as an enemy to targets
	 * 
	 * getRange() should return the range at which this enemy can seek targets
	 * canTarget() inputs possible target and should return true if this enemy can target them
	 */
	
	public void tick(TagHandler tagHandler, Entity entity) {
		
	}
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
		if (entity.getAlive()) {
			ArrayList<Entity> entities = tagHandler.getEntityFinder().getAllEntitiesInRadis(entity, getRange());
			
			Entity closestTarget = null;
			double closestDistanceSquared = 0;
			
			for (Entity target : entities) {
				if (canTarget(target)) {
					double distanceSquared = VectorMath.distance(entity.getPosition(), target.getPosition());
					
					if (closestTarget == null || closestDistanceSquared > distanceSquared) {
						closestTarget = target;
						closestDistanceSquared = distanceSquared;
					}
				}
			}
			
			if (closestTarget != null) {
				tagHandler.getPathfindingHandler().pathTo(entity.getId(), closestTarget.getId());
			}
		}
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		return interactingEntity;
	}
	
	// getSlot(); also exists
}
