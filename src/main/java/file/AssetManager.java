package file;

import java.util.HashMap;

import entity.EntityManager;
import main.Console;
import protonova.protobuf.EntityProto.Entity;
import util.Id;

public class AssetManager {
	private HashMap<String, Entity> entityAssets;
	private EntityManager entityManager;
	
	public AssetManager(EntityManager entityManager, HashMap<String, Entity> entityAssets, Console console) {
		this.entityAssets = entityAssets;
		this.entityManager = entityManager;
		
		// just checking the entities so they don't load with improper values
		for (Entity entity : entityAssets.values()) {
			if (entity.getSize().getX() == 0) {
				System.err.println("Asset: "+entity.getName()+" has a size of zero on x axis");
			}
			if (entity.getSize().getY() == 0) {
				System.err.println("Asset: "+entity.getName()+" has a size of zero on y axis");
			}
		}
	}
	
	public Entity getEntity(String name, int mapId) {
		
		if (entityAssets.containsKey(name)) {
			
			Entity clone = entityAssets.get(name).toBuilder()
					.setId(Id.getNewId(entityManager.getAllEntities().keySet()))
					.setMap(mapId)
					.build();
			
			return clone;
		}
		else System.err.print("Error: Could not find asset: "+name);
		return null;
	}
	
	public boolean containsEntity(String name) {
		return entityAssets.containsKey(name);
	}
}
