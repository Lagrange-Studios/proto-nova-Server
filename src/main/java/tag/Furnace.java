package tag;

import protonova.protobuf.EntityProto.Entity;

public class Furnace extends TagClass {

	public String getTag() {
		return "furnace";
	}
	
	private String fuelTypeName = "basicFuel";
	
	/*
	 * This tag makes the entity act as a furnace
	 * It can intake fuel
	 * It can also be used in furnace recipes
	 *
	 * inventory:
	 * -fuelTimer: fuel time left in ticks
	 * 
	 * fuelTypeName: the type name of items that can be used for fuel
	 */
	
	public void tick(TagHandler tagHandler, Entity entity) {
		
		int fuelTime = getSlot(entity, "fuelTimer", 0);
		
		if (fuelTime > 0) {
			fuelTime--;
			
			Entity.Builder builder = entity.toBuilder();
			builder.putInventorySlots("fuelTimer", fuelTime);
			
			if (fuelTime == 0) {
				builder.setDisplayTexture("");
			}
			
			tagHandler.updateEntity(builder.build());
		}
	}
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		
		
		return interactingEntity;
	}
	
	@SuppressWarnings("unused")
	public int getSlot(Entity entity, String slotName, int defualtValue) {
		if (entity.containsInventorySlots(slotName)) return entity.getInventorySlotsMap().get(slotName);
		else return defualtValue;
	}
}
