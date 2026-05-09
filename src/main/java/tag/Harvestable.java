package tag;

import protonova.protobuf.EntityProto.Entity;

public class Harvestable extends TagClass {

	public String getTag() {
		return "harvestable";
	}
	
	/*
	 * This class makes the entity harvestable
	 * selectedSlot is the result from harvest
	 * inventory:
	 * -harvestTimer: starts at harvest time and decrments to 0
	 * -harvestInterval: is the amount of time in second it should take between harvests
	 * 
	 */
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
		int harvestTimer = getSlot(entity, "harvestTimer", 60);
		
		
		harvestTimer--;
		// also check to see if the plants still growing
		if (harvestTimer <= 0 && !entity.containsInventorySlots("currentPlantAge")) {
			tagHandler.updateEntity(
					entity.toBuilder()
					.putInventorySlots("harvestTimer", harvestTimer)
					.setDisplayTexture("")
					.build()
					);
		}
		else tagHandler.updateEntity(
				entity.toBuilder()
				.putInventorySlots("harvestTimer", harvestTimer)
				.setDisplayTexture("empty "+entity.getName())
				.build()
				);
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		int harvestTimer = getSlot(thisEntity, "harvestTimer", 60);
		
		if (harvestTimer <= 0) {
			int harvestInterval = getSlot(thisEntity, "harvestInterval", 60);
			
			harvestTimer = harvestInterval;
			thisEntity = thisEntity.toBuilder()
				.putInventorySlots("harvestTimer", harvestInterval)
				.setDisplayTexture("empty "+thisEntity.getName())
				.build();
			
			tagHandler.updateEntity(thisEntity);
			
			Entity harvest = tagHandler.getAssetManager().getEntity(thisEntity.getSelectedSlot(), thisEntity.getMap());
			harvest = harvest.toBuilder()
					.setPosition(thisEntity.getPosition())
					.build();
			
			tagHandler.updateEntity(harvest);
		}
		
		return interactingEntity;
	}
}
