package tag;

import java.util.Arrays;
import java.util.HashSet;

import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.TileProto.Tile;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;
import util.Random;
import util.VectorMath;

public class Fungus extends TagClass {

	private final int chanceOfGrowthPerSecond = 2; // one in ten chance
	
	// tile info
	public static final HashSet<String> allowedTiles = new HashSet<>(Arrays.asList("grass", "stone", "sand"));
	public static final HashSet<String> conversionTiles = new HashSet<>(Arrays.asList("grass"));
	
	public String getTag() {
		return "fungus";
	}
	
	public static Tile convertTile(Tile tile) {
		if (Fungus.conversionTiles.contains(tile.getName())) {
			tile = tile.toBuilder()
					.setName("dirt")
					.build();
		}
		return tile;
	}
	
	/*
	 * This class makes the entity act as a fungus by spreading and growing
	 * inventory:
	 * -parentSpore: the spore this entity relies on. if it dies so does this
	 */
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
		// check to see if parent is still alive
		if (tagHandler.getEntityManager().getEntity(entity.getInventorySlotsMap().get("parentSpore")) != null) {
			
			if (Random.randomInt(1, chanceOfGrowthPerSecond) == 1) {
				Vector offset = Vector.newBuilder().build();
				int direction = Random.randomInt(1, 4);

				switch(direction) {
					case 1:
						offset = Vector.newBuilder().setY(1).build();
						break;
					case 2:
						offset = Vector.newBuilder().setY(-1).build();
						break;
					case 3:
						offset = Vector.newBuilder().setX(1).build();
						break;
					case 4:
						offset = Vector.newBuilder().setX(-1).build();
						break;
				}
				
				Vector newPosition = VectorMath.add(offset, entity.getPosition());
				Tile tile = tagHandler.getPlaneManager().getTileAt(newPosition, entity.getMap());

				// check t osee if we can spread to this tile
				if (tile != null && allowedTiles.contains(tile.getName())) {
					boolean open = true;
					
					for (Entity foundEntity : tagHandler.getEntityFinder().getAllEntitiesInRadius(newPosition, entity.getMap(), .99)) {
						if (!foundEntity.getIsItem() && foundEntity.getId() != entity.getId()) {
							open = false;
							break;
						}
					}

					if (open) {
						Entity newClone = tagHandler.getAssetManager().getEntity("fungus vein", entity.getMap());
						
						newClone = newClone.toBuilder()
								.setDirectionValue(Random.randomInt(0, 3))
								.setPosition(newPosition)
								.putInventorySlots("parentSpore", entity.getInventorySlotsMap().get("parentSpore"))
								.build();
						
						tagHandler.updateEntity(newClone);
						
						tile = convertTile(tile);
						tagHandler.getPlaneManager().updateTile(tile, entity.getMap());
					}
					else {
						// TODO: damage structure/entity
					}
				}
			}
		}
		else {
			Entity.Builder entityBuilder = entity.toBuilder()
					.setDisplayTexture("dead fungus vein")
					.removeInventorySlots("parentSpore")
					.clearTags();
			
			for (int i=0;i<entity.getTagsCount()-1;i++) {
				if (!entityBuilder.getTags(i).equals("fungus")) {
					entityBuilder.addTags(entity.getTags(i));
				}
			}
			
			entity = entityBuilder.build();
			tagHandler.updateEntity(entity);
				
		}
	}
}
