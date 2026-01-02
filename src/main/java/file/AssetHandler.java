package file;

import java.util.HashMap;

import entity.EntityManager;
import protonova.protobuf.EntityProto.Entity;
import util.Id;

public class AssetHandler {
	private HashMap<String, Entity> entityAssets;
	private EntityManager entityManager;
	
	public AssetHandler(EntityManager entityManager, HashMap<String, Entity> entityAssets) {
		this.entityAssets = entityAssets;
		this.entityManager = entityManager;
	}
	
	public Entity getEntity(String name, int mapId) {
		
		if (entityAssets.containsKey(name)) {
			
			Entity clone = entityAssets.get(name).toBuilder()
					.setId(Id.getNewId(entityManager.getAllEntities().keySet()))
					.setMap(mapId)
					.build();
			
			return clone;
		}
		return null;
	}
}
