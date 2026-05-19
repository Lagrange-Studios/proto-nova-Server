package tag;

import java.util.ArrayList;

import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.EntityProto.Entity.Builder;
import protonova.protobuf.TileProto.Tile;
import protonova.protobuf.VectorProto.Vector;

public class Plant extends TagClass {

	public String getTag() {
		return "plant";
	}
	
	/*
	 * This class makes the entity act as a plant. it will spread and grow
	 * 
	 * inventory:
	 * -totalPlantGrowTime: the total amount of seconds required to grow
	 * -currentPlantAge: the current plant age in seconds
	 * -randomSproutChance: determines the chance for the plant to attempt to sprout every second
	 * i.e. randomSproutChance = 10 means a 1 in ten chance every second
	 * 
	 */
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
		// check to see if it has the right tags for growing
		if (entity.containsInventorySlots("totalPlantGrowTime") && 
				entity.containsInventorySlots("currentPlantAge")) {
			
			int totalPlantGrowTime = entity.getInventorySlotsMap().get("totalPlantGrowTime");
			int currentPlantAge = entity.getInventorySlotsMap().get("currentPlantAge");
			
			
			float sizeX = entity.getSize().getX();
			float sizeY = entity.getSize().getY();
			
			float grownSizeX = (float) ((totalPlantGrowTime * sizeX) / currentPlantAge);
			float grownSizeY = (float) ((totalPlantGrowTime * sizeY) / currentPlantAge);
			
			currentPlantAge++;
			
			float newRatio =  ((float) currentPlantAge / (float) totalPlantGrowTime);
			
			float updatedSizeX =  grownSizeX * newRatio;
			float updatedSizeY = grownSizeY * newRatio;
			
			Builder entityBuilder = entity.toBuilder()
					.putInventorySlots("currentPlantAge", currentPlantAge)
					.setSize(Vector.newBuilder()
							.setX(updatedSizeX)
							.setY(updatedSizeY)
							.build());
			
			if (currentPlantAge >= totalPlantGrowTime) entityBuilder.removeInventorySlots("currentPlantAge");
			
			entity = entityBuilder.build();
			tagHandler.updateEntity(entity);
			
		}
		// check to see if its grown
		else if (!entity.containsInventorySlots("currentPlantAge")) {
			
			int randomSproutChance = getSlot(entity, "randomSproutChance", 60);
			int totalPlantGrowTime = getSlot(entity, "totalPlantGrowTime", 60);
			
			// chance check
			if (Math.round(Math.random()*randomSproutChance) == 1) {
				int offsetX = (int) (Math.round(Math.random()*10)-5);
				int offsetY = (int) (Math.round(Math.random()*10)-5);
				
				Vector spawnVector = entity.getPosition().toBuilder()
						.setX(entity.getPosition().getX()+offsetX)
						.setY(entity.getPosition().getY()+offsetY)
						.build();
				
				ArrayList<Entity> foundEntities = tagHandler.getEntityFinder().getAllEntitiesInRadius(spawnVector, entity.getMap(), Math.hypot(entity.getSize().getX(), entity.getSize().getY()));

				if (foundEntities.size() > 0) return;
				
				Tile originalTile = tagHandler.getPlaneManager().getTileAt(entity.getPosition(), entity.getMap());
				Tile newTile = tagHandler.getPlaneManager().getTileAt(spawnVector, entity.getMap());
				
				if (newTile == null || originalTile == null || !newTile.getName().equals(originalTile.getName())) return;
				
				Vector entitySize = entity.getSize();
				float spawnSizeRatio = (float) 1/totalPlantGrowTime;
				
				Entity sprout = entity.toBuilder()
						.setPosition(spawnVector)
						.setSize(
								Vector.newBuilder()
								.setX(entitySize.getX()*spawnSizeRatio)
								.setY(entitySize.getY()*spawnSizeRatio)
								.build())
						.putInventorySlots("currentPlantAge", 1)
						.putInventorySlots("totalPlantGrowTime", totalPlantGrowTime)
						.setId(tagHandler.getEntityManager().reserveNewEntityId())
						.build();
				
				if (sprout.getTagsList().contains("harvestable")) {
					sprout = sprout.toBuilder()
							.setDisplayTexture("empty "+sprout.getName())
							.build();
				}
				
				tagHandler.updateEntity(sprout);
						
			}
		}
	}
}
