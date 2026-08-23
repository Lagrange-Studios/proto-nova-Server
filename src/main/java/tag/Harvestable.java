package tag;

import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;

public class Harvestable extends TagClass {

	public String getTag() {
		return "harvestable";
	}
	
	/*
	 * This class makes the entity harvestable
	 * selectedSlot is the result from harvest
	 * data:
	 * -harvestTimer: starts at harvest time and decrments to 0
	 * -harvestInterval: is the amount of time in second it should take between harvests
	 * 
	 */
	
	public boolean hasSecondTick() {
		return true;
	}
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
		int harvestTimer = DataUtil.getInt(entity, "harvestTimer", 60);
		
		
		harvestTimer--;
		// also check to see if the plants still growing
		if (entity.hasDisplayTexture() && harvestTimer <= 0 && !entity.containsCustomData("currentPlantAge")) {
			tagHandler.updateEntity(
					entity.toBuilder()
					.putCustomData("harvestTimer",DataUtil.newInt(harvestTimer))
					.clearDisplayTexture()
					.build()
					);
		}
		else if (harvestTimer >= 0) tagHandler.updateEntity(
				entity.toBuilder()
				.putCustomData("harvestTimer",DataUtil.newInt(harvestTimer))
				.setDisplayTexture("empty "+entity.getName())
				.build()
				);
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		int harvestTimer = DataUtil.getInt(thisEntity, "harvestTimer", 60);
		
		if (harvestTimer <= 0) {
			int harvestInterval = DataUtil.getInt(thisEntity, "harvestInterval", 60);
			
			harvestTimer = harvestInterval;
			thisEntity = thisEntity.toBuilder()
				.putCustomData("harvestTimer", DataUtil.newInt(harvestTimer))
				.setDisplayTexture("empty "+thisEntity.getName())
				.build();
			
			tagHandler.updateEntity(thisEntity);
			
			Entity harvest = tagHandler.getAssetManager().getEntity(thisEntity.getSelectedSlot(), thisEntity.getMap(), thisEntity.getPosition());
			
			tagHandler.updateEntity(harvest);
		}
		
		return interactingEntity;
	}
}
