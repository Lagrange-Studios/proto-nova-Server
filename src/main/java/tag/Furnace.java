package tag;

import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import util.DataUtil;

public class Furnace extends TagClass {

	public String getTag() {
		return "furnace";
	}
	
	protected String getFuelType() {
		return "basicFuel";
	}
	
	protected int getFuelTimeAdded() {
		return 20 * 20;
	}
	
	protected int getTicksBetweenFrames()  {
		return 5;
	}
	
	protected String getHexColor()  {
		return "FF0000";
	}
	
	protected float getLightRange() {
		return 2;
	}
	
	/*
	 * This tag makes the entity act as a furnace
	 * It can intake fuel
	 * It can also be used in furnace recipes
	 *
	 * data:
	 * -fuelTimer: fuel time left in ticks
	 * 
	 * fuelTypeName: the type name of items that can be used for fuel
	 */
	
	public boolean hasTick() {
		return true;
	}
	
	public void tick(TagHandler tagHandler, Entity entity) {
		
		int fuelTime = DataUtil.getInt(entity, "fuelTimer", 0);
		
		if (fuelTime > 0) {
			fuelTime--;
			
			Entity.Builder builder = entity.toBuilder();
			DataUtil.setInt(builder, "fuelTimer", fuelTime);
			
			if (fuelTime == 0) {
				builder = unlight(builder);
			}
			else {
				builder.setDirection(switchFPS(tagHandler, entity));
			}
			
			tagHandler.updateEntity(builder.build());
		}
	}
	
	public Entity interact(TagHandler tagHandler, Entity interactingEntity, Entity thisEntity) {
		
		if (!interactingEntity.getSelectedSlot().equals("")) {
			String slot = interactingEntity.getSelectedSlot();
			
			if (interactingEntity.getInventorySlotsMap().containsKey(slot)) {
				Entity item = tagHandler.getEntityManager().getEntity(interactingEntity.getInventorySlotsMap().get(slot));
				
				if (item != null && item.getTagsList().contains(getFuelType())) {
					interactingEntity = tagHandler.getEntityManager().decrementSlot(interactingEntity, slot);

					int fuelTime = DataUtil.getInt(thisEntity, "fuelTimer", 0) + getFuelTimeAdded();
					
					thisEntity = light(thisEntity);
					
					thisEntity = thisEntity.toBuilder()
							.putCustomData("fuelTimer", DataUtil.newInt(fuelTime))
							.build();
					
					tagHandler.updateEntity(thisEntity);
					
				}
			}
		}
		
		return interactingEntity;
	}
	
	protected Direction switchFPS(TagHandler tagHandler, Entity entity) {
		int cycle = (int) Math.round(tagHandler.getServer().globalTicks%getTicksBetweenFrames());
		
		if (cycle == 0) {
			int direction = entity.getDirectionValue();
			direction++;
			
			if (direction > 3) direction = 0;
			
			return Direction.forNumber(direction);
		}
		else return entity.getDirection();
	}
	
	protected Entity light(Entity entity) {
		return entity.toBuilder()
				.setLightRange(getLightRange())
				.setHexColor(getHexColor())
				.setDisplayTexture(entity.getName()+" lit")
				.build();
	}
	
	protected Entity.Builder unlight(Entity.Builder entity) {
		return entity
				.clearLightRange()
				.clearHexColor()
				.clearDisplayTexture();
	}
}
