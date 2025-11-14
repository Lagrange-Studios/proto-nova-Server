package entity;

import java.util.HashMap;

import file.ServerLoader;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;

public class EntityManager {

	private HashMap<Integer, Entity> entities;
	private ServerLoader serverLoader;

	public EntityManager(ServerLoader serverLoader) {
		this.serverLoader = serverLoader;
		entities = serverLoader.loadEntities();
	}
	
	public Entity makeNewEntity(String name,int mapId) {
		
		int currentId = 1;
		
		while (true) {
			if (!entities.containsKey(currentId)) {
				break;
			}
			else {
				currentId++;
			}
		}
		
		Vector vector = Vector.newBuilder()
				.setX(0)
				.setY(0)
				.build();
		
		Entity entity = Entity.newBuilder()
				.setName(name)
				.setMap(mapId)
				.setPosition(vector)
				.setId(currentId)
				.build();
		
		entities.put(currentId, entity);
		
		return entity;
		
	}
	
	public Entity makeNewEntity(String name) {
		return makeNewEntity(name,1);
	}
	
	public Entity getEntity(int id) {
		return entities.get(id);
	}
	
	public HashMap<Integer,Entity> getAllEntities() {
		return entities;
	}
}
