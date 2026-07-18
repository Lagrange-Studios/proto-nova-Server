package tag;

import protonova.protobuf.EntityProto.Entity;

public class Butcherable extends TagClass{

	/**
	 * @return The tag of this specific class that should be on entities
	 */
	public String getTag() {
		return "butcherable";
	}
	
	/**
	 * occurs once per tick
	 */
	public void tick(TagHandler tagHandler, Entity entity) {
		
	}
	
	/**
	 * occurs once per second
	 */
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
	}
	
	/**
	 * 
	 * @param interactingEntity the entity interacting with a entity with this tag
	 * @param thisEntity the entity with the tag
	 * @return the new version of the interacting entity
	 */
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		return interactingEntity;
	}
	
	/**
	 * 
	 * @param entity The entity that you are reading a value from
	 * @param slotName The name of slot your are accessing
	 * @param defualtValue A fall back value in case there is not slot with that name
	 * @return Either the slot value or the defualtValue
	 */
	@SuppressWarnings("unused")
	public int getSlot(Entity entity, String slotName, int defualtValue) {
		if (entity.containsInventorySlots(slotName)) return entity.getInventorySlotsMap().get(slotName);
		else return defualtValue;
	}
}
