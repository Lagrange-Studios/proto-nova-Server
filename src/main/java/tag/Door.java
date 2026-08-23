package tag;

import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;

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
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		boolean isOpen = DataUtil.getBoolean(thisEntity, "isOpen", false);
		Entity.Builder builder = thisEntity.toBuilder();
		
		// starts closed
		if (!isOpen) {
			builder = thisEntity.toBuilder()
					.putInventorySlots("doorState", 1)
					.setDisplayTexture(thisEntity.getName()+" open")
					.setCanCollide(false)
					.setCastShadow(false);
			
			DataUtil.setBoolean(builder, "isOpen", true);
		}
		// starts open
		else if (isOpen) {
			builder = thisEntity.toBuilder()
					.putInventorySlots("doorState", 0)
					.clearDisplayTexture()
					.setCanCollide(true)
					.setCastShadow(true);
			
			DataUtil.setBoolean(builder, "isOpen", false);
		}
		
		tagHandler.updateEntity(builder.build());
		
		return interactingEntity;
	}
	
}
