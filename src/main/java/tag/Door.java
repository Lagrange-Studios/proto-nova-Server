package tag;

import protonova.protobuf.EntityProto.Entity;

public class Door extends TagClass {

	/*
	 * Makes the entity act as a door
	 * When open the collision of the entity will be turned off and the display texture will be:
	 * ENTITY_NAME + " open"
	 * state: 
	 * 0 = closed
	 * 1 = open
	 */
	
	public String getTag() {
		return "door";
	}
	
	public void tick(TagHandler tagHandler, Entity entity) {
		
	}
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		int state = getSlot(thisEntity, "doorState", 0);
		
		// starts closed
		if (state == 0) {
			thisEntity = thisEntity.toBuilder()
					.putInventorySlots("doorState", 1)
					.setDisplayTexture(thisEntity.getName()+" open")
					.setCanCollide(false)
					.setCastShadow(false)
					.build();
		}
		// starts open
		else if (state == 1) {
			thisEntity = thisEntity.toBuilder()
					.putInventorySlots("doorState", 0)
					.clearDisplayTexture()
					.setCanCollide(true)
					.setCastShadow(true)
					.build();
		}
		
		tagHandler.updateEntity(thisEntity);
		
		return interactingEntity;
	}
	
}
