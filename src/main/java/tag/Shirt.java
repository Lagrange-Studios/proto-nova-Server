package tag;

import java.util.ArrayList;

import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;
import util.VectorMath;

public class Shirt extends TagClass {

	public String getTag() {
		return "shirt";
	}
	
	/*
	 * Makes a item equipable into the shirt slot
	 * 
	 */
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		if (!interactingEntity.containsInventorySlots("shirt")) {
			interactingEntity = interactingEntity.toBuilder()
					.removeInventorySlots(interactingEntity.getSelectedSlot())
					.putInventorySlots("shirt", thisEntity.getId())
					.build();
		}
		
		return interactingEntity;
	}
}
