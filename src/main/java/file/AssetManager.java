package file;

import java.util.HashMap;
import java.util.HashSet;

import entity.EntityManager;
import main.Console;
import protonova.protobuf.EntityProto.Entity;
import util.Id;

public class AssetManager {
	private HashMap<String, Entity> entityAssets;
	private EntityManager entityManager;
	private HashMap<String, HashSet<String>> typeMap;
	
	public AssetManager(EntityManager entityManager, HashMap<String, Entity> entityAssets, Console console, HashMap<String, HashSet<String>> typeMap) {
		this.entityAssets = entityAssets;
		this.entityManager = entityManager;
		this.typeMap = typeMap;
		
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
					.setId(entityManager.reserveNewEntityId())
					.setMap(mapId)
					.build();
			
			return clone;
		}
		else System.err.print("Error: Could not find asset: "+name);
		return null;
	}
	
	public final Entity getReadOnlyEntity(String name) {
		return entityAssets.get(name);
	}
	
	public boolean containsEntity(String name) {
		return entityAssets.containsKey(name);
	}
	
	public HashSet<String> getTypes(String typeName) {
		return typeMap.get(typeName);
	}
}
