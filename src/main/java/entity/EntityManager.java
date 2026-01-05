package entity;

import java.util.HashMap;

import file.ServerLoader;
import main.Console;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import socket.Player;
import util.Id;

public class EntityManager {

	private HashMap<Integer, Entity> entities;
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;

	public EntityManager(ServerLoader serverLoader,Console console) {
		this.serverLoader = serverLoader;
		entities = serverLoader.loadEntities();
		this.console = console;
	}
	
	public Entity makeNewEntity(String name,int mapId) {
		
		int currentId = Id.getNewId(entities.keySet());
		
		Vector vector = Vector.newBuilder()
				.setX(0)
				.setY(0)
				.build();
		
		Vector vector2 = Vector.newBuilder()
				.setX(0)
				.setY(0)
				.build();
		
		Entity entity = Entity.newBuilder()
				.setName(name)
				.setMap(mapId)
				.setPosition(vector)
				.setId(currentId)
				.setSpeed(15)
				.setVelocity(vector2)
				.setDirection(Direction.Down)
				.setSelectedSlot("leftHand")
				.build();
		
		entities.put(currentId, entity);
		
		if (chunkManager != null) {
			chunkManager.addEntity(entity);
		}
		else {
			console.print("WARNING: created entity without adding it to the chunk manager");
		}
		
		return entity;
		
	}
	
	public Entity makeNewEntity(String name) {
		return makeNewEntity(name,1);
	}
	
	public Entity getEntity(int id) {
		return entities.get(id);
	}
	
	public Entity getEntity(Player player) {
		return entities.get(player.data.getEntityId());
	}
	
	public HashMap<Integer,Entity> getAllEntities() {
		return entities;
	}
	
	public void updateEntity(Entity entity) {
		if (entities.containsKey(entity.getId())) {
			Entity oldEntity = entities.get(entity.getId());
			
			if (!oldEntity.getPosition().equals(entity.getPosition()) || oldEntity.getMap() != entity.getMap()) {
				chunkManager.updateEntityChunck(oldEntity, entity);
			}
		}
		else {
			chunkManager.addEntity(entity);
		}
		entities.put(entity.getId(), entity);
	}

	public void setChunkManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
	}
}
