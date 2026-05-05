package tag;

import protonova.protobuf.EntityProto.Entity;

public class TagClass {

	/**
	 * @return The tag of this specific class that should be on entities
	 */
	public String getTag() {
		return "null";
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
}
