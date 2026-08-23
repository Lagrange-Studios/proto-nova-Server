package tag;


import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;

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
	
	public boolean hasSecondTick() {
		return true;
	}
	
	public void secondTick(TagHandler tagHandler, Entity entity) {

		if (entity.getAlive()) {
			if (!tagHandler.getPathfindingHandler().hasAgent(entity.getId()))
				tagHandler.getPathfindingHandler().newAgent(entity.getId(),"fungusMind");
			
			Entity spore = tagHandler.getEntityManager().getEntity(DataUtil.getInt(entity,"parentSpore",0));
			if (spore != null && spore.getName().equals("fungus spore") ) {;

				tagHandler.getPathfindingHandler().changeGoal(entity.getId(), DataUtil.getInt(spore, "target", 0));
					
			}
			else {
				tagHandler.getPathfindingHandler().removeEntity(entity);
				
				tagHandler.getEntityManager().removeEntity(entity);
			}
		}
			
		
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		return interactingEntity;
	}
	
	// getSlot(); also exists
}
