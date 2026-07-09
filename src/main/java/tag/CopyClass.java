package tag;

import protonova.protobuf.EntityProto.Entity;

public class CopyClass extends TagClass {

	public String getTag() {
		return "null";
	}
	
	public void tick(TagHandler tagHandler, Entity entity) {
		
	}
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		return interactingEntity;
	}
	
	// getSlot(); also exists
}
