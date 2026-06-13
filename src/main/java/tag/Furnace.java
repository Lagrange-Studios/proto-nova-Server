package tag;

import protonova.protobuf.EntityProto.Entity;

public class Furnace {

	public String getTag() {
		return "furnace";
	}
	
	/*
	 This tag makes the entity act as a furnace
	 It can intake fuel
	 It can also be used in furnace recipes
	 */
	
	public void tick(TagHandler tagHandler, Entity entity) {
		
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
