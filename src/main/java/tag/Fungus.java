package tag;

import java.util.Arrays;
import java.util.HashSet;

import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.TileProto.Tile;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;
import util.DebugPrinter;
import util.Random;
import util.VectorMath;

public class Fungus extends TagClass {

	private final int chanceOfGrowthPerSecond = 15; // one in ten chance
	private final int evolveChancePerSecond = chanceOfGrowthPerSecond*2; // one in ten chance
	private final int fungusMonsterChance = 20; // 1 in x amount 
	
	// tile info
	public static final HashSet<String> allowedTiles = new HashSet<>(Arrays.asList("grass", "stone", "sand"));
	public static final HashSet<String> conversionTiles = new HashSet<>(Arrays.asList("grass"));
	
	// fungus names
	public static final HashSet<String> fungusNames = new HashSet<>(Arrays.asList("fungus vein", "fungus spore", "fortifeid fungus vein"));
	
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
	 * 
	 * name: if name is fortified fungus vein then none of these changes will occur
	 */
	
	public void secondTick(TagHandler tagHandler, Entity entity) {
		
		// check to see if parent is still alive
		if (tagHandler.getEntityManager().getEntity(entity.getInventorySlotsMap().get("parentSpore")) != null ) {
			
			if (Random.randomInt(1, chanceOfGrowthPerSecond) == 1 &&
					!entity.getName().equals("fortifeid fungus vein")) {
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

				// check to see if we can spread to this tile
				if (tile != null && allowedTiles.contains(tile.getName())) {
					boolean open = true;
					
					for (Entity foundEntity : tagHandler.getEntityFinder().getAllEntitiesInRadius(newPosition, entity.getMap(), .99)) {
						if (!foundEntity.getIsItem() && foundEntity.getId() != entity.getId()) {
							
							if (foundEntity.getTagsList().contains("plant")) {	
								tagHandler.getCombatManager().attemptToDamage(entity,foundEntity);
								tagHandler.getHealthManager().entityCheck(foundEntity);
							}
							
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
				}
			}
			// chance to fortify
			else if (entity.getName().equals("fungus vein") && Random.randomInt(1,evolveChancePerSecond) == 1 && 
					surroundedByVeins(entity, tagHandler)) {
				
				// chance to spawn a monster along with fortifying
				if (Random.randomInt(1, fungusMonsterChance) == 1) {
					Entity newMonster = tagHandler.getAssetManager().getEntity("fungus monster", entity.getMap());
					
					newMonster = newMonster.toBuilder()
							.setPosition(entity.getPosition())
							.build();
					
					tagHandler.updateEntity(newMonster);
					
					System.out.println("new monster spawned at: ");
					DebugPrinter.print(entity.getPosition());
				}
				entity = entity.toBuilder()
						.setName("fortifeid fungus vein")
						.build();
				
				tagHandler.updateEntity(entity);
				
				
			}
		}
		else {
			Entity.Builder entityBuilder = entity.toBuilder()
					.setName("dead fungus vein")
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
	
	private boolean surroundedByVeins(Entity centralVein, TagHandler tagHandler) {
		Vector start = centralVein.getPosition();
		int mapId = centralVein.getMap();
		
		Vector positionUp = start.toBuilder().setY(start.getY()+1).build();
		Vector positionDown = start.toBuilder().setY(start.getY()-1).build();
		Vector positionRight = start.toBuilder().setX(start.getX()+1).build();
		Vector positionLeft = start.toBuilder().setX(start.getX()-1).build();
		
		return containsVein(positionUp, mapId, tagHandler) && 
				containsVein(positionDown, mapId, tagHandler) && 
				containsVein(positionLeft, mapId, tagHandler) && 
				containsVein(positionRight, mapId, tagHandler);
	}
	
	private boolean containsVein(Vector position, int mapId, TagHandler tagHandler) {
		for (Entity vein : tagHandler.getEntityFinder().getAllEntitiesInRadius(position, mapId, 0.5)) {
			if (fungusNames.contains(vein.getName())) return true;
		}
		return false;
	}
}
