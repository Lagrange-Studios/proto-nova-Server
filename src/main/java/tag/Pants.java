package tag;

import java.util.ArrayList;

import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;
import util.VectorMath;

public class Pants extends TagClass {

	public String getTag() {
		return "pants";
	}
	
	/*
	 * Makes a item equipable based on the other tag
	 * 
	 */
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		if (!interactingEntity.containsInventorySlots("pants")) {
			interactingEntity = interactingEntity.toBuilder()
					.removeInventorySlots(interactingEntity.getSelectedSlot())
					.putInventorySlots("pants", thisEntity.getId())
					.build();
		}
		
		return interactingEntity;
	}
}
