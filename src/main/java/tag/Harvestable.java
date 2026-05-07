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
		
		int harvestTimer;
		if (!entity.containsInventorySlots("harvestTimer")) harvestTimer = 0;
		else harvestTimer = entity.getInventorySlotsMap().get("harvestTimer");
		
		
		harvestTimer--;
		if (harvestTimer <= 0) {
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
				.build()
				);
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		int harvestTimer;
		if (!thisEntity.containsInventorySlots("harvestTimer")) harvestTimer = 0;
		else harvestTimer = thisEntity.getInventorySlotsMap().get("harvestTimer");
		
		if (harvestTimer <= 0) {
			int harvestInterval;
			if (!thisEntity.containsInventorySlots("harvestInterval")) harvestInterval = 60;
			else harvestInterval = thisEntity.getInventorySlotsMap().get("harvestInterval");
			
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
