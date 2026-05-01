package tag;

import protonova.protobuf.EntityProto.Entity;

public class TagClass {

	
	/**
	 * occurs once per tick
	 */
	public static void tick(TagHandler tagHandler, Entity entity) {
		
	}
	
	/**
	 * occurs once per second
	 */
	public static void secondTick(TagHandler tagHandler, Entity entity) {
		
	}
	
	/**
	 * 
	 * @param interactingEntity the entity interacting with a entity with this tag
	 * @param thisEntity the entity with the tag
	 * @return the new version of the interacting entity
	 */
	public static Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		return interactingEntity;
	}
}
