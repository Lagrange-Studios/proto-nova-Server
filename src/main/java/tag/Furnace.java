package tag;

import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;

public class Furnace extends TagClass {

	public String getTag() {
		return "furnace";
	}
	
	private String fuelTypeName = "basicFuel";
	private int fuelTimeAddedPerFuel = 100;
	private int animationFPS = 5;
	
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
			builder.setDirection(switchFPS(tagHandler));
			
			tagHandler.updateEntity(builder.build());
		}
	}
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		if (!interactingEntity.getSelectedSlot().equals("")) {
			String slot = interactingEntity.getSelectedSlot();
			
			if (interactingEntity.getInventorySlotsMap().containsKey(slot)) {
				Entity item = tagHandler.getEntityManager().getEntity(interactingEntity.getInventorySlotsMap().get(slot));
				
				if (item != null && item.getTagsList().contains(fuelTypeName)) {
					tagHandler.getEntityManager().decrementSlot(interactingEntity, slot);
					
					int fuelTimer = getSlot(thisEntity, "fuelTimer", 0) + fuelTimeAddedPerFuel;
					
					thisEntity = thisEntity.toBuilder()
							.putInventorySlots("fuelTimer", fuelTimer)
							.setDisplayTexture("lit "+thisEntity.getName())
							.build();
					
					tagHandler.updateEntity(thisEntity);
					
				}
			}
		}
		
		return interactingEntity;
	}
	
	@SuppressWarnings("unused")
	public int getSlot(Entity entity, String slotName, int defualtValue) {
		if (entity.containsInventorySlots(slotName)) return entity.getInventorySlotsMap().get(slotName);
		else return defualtValue;
	}
	
	private Direction switchFPS(TagHandler tagHandler) {
		switch(tagHandler.getTPS()%animationFPS) {
			case 0:
				return Direction.Down;
			case 1:
				return Direction.Up;
			case 2:
				return Direction.Left;
			case 3:
				return Direction.Right;
			default:
				return Direction.Down;
		}
	}
}
