package tag;

import java.util.ArrayList;

import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;
import util.VectorMath;

public class Locator extends TagClass {

	public String getTag() {
		return "locator";
	}
	
	public boolean validTarget(Entity entity) {
		return entity.getName().equals("fungus spore");
	}
	
	/*
	 * This class makes an entity locate fungi
	 * 
	 */
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		ArrayList<Entity> entities = tagHandler.getEntityFinder().getAllEntitiesInRadis(interactingEntity, 250);
		int closestIndex = 0;
		double closestDistanceSquared = 0;
		
		for (int i=0;i<entities.size();i++) {
			Entity entity = entities.get(i);
			
			if (validTarget(entity)) {
				double distanceSquared = VectorMath.distanceSquared(entity.getPosition(), thisEntity.getPosition());
				
				if (closestDistanceSquared == 0 || closestDistanceSquared >  distanceSquared) {
					closestIndex = i;
					closestDistanceSquared = distanceSquared;
				}
				
			}
		}
		
		// finding
		if (closestDistanceSquared != 0) {
			Entity closestEntity = entities.get(closestIndex);
			tagHandler.getChatManager().messageAllPlayers("[Locator] target located at x:"+closestEntity.getPosition().getX()+", y:"+closestEntity.getPosition().getY());
			
			interactingEntity = interactingEntity.toBuilder()
					.removeInventorySlots(interactingEntity.getSelectedSlot())
					.build();
			
			tagHandler.getEntityManager().removeEntity(thisEntity);
		}
		else {
			tagHandler.getChatManager().messageAllPlayers("[Locator] no target found close");
		}
		
		
		return interactingEntity;
	}
}
