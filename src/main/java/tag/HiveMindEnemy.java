package tag;


import protonova.protobuf.EntityProto.Entity;

public class HiveMindEnemy extends TagClass {

	public String getTag() {
		return "hiveMindEnemy";
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
			if (!tagHandler.getPathfindingHandler().hasAgent(entity.getId()))
				tagHandler.getPathfindingHandler().newAgent(entity.getId(),"fungusMind");
			
			Entity spore = tagHandler.getEntityManager().getEntity(entity.getInventorySlotsMap().get("parentSpore"));
			if (spore.getName().equals("fungus spore") ) {;
			
				if (spore.containsInventorySlots("target"))
					tagHandler.getPathfindingHandler().changeGoal(entity.getId(), spore.getInventorySlotsMap().get("target"));
				else
					tagHandler.getPathfindingHandler().changeGoal(entity.getId(), 0);
					
			}
			else {
				tagHandler.getEntityManager().removeEntity(entity);
			}
		}
			
		
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		return interactingEntity;
	}
	
	// getSlot(); also exists
}
