package tag;


import protonova.protobuf.EntityProto.Entity;

public class Enemy extends TagClass {

	public String getTag() {
		return "enemy";
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
		
		if (entity.getAlive() && !tagHandler.getPathfindingHandler().hasAgent(entity.getId()))
			tagHandler.getPathfindingHandler().newAgent(entity.getId(), getTag());
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		return interactingEntity;
	}
	
	// getSlot(); also exists
}
