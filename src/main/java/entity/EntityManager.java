package entity;

import java.util.ArrayList;
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
	private final HashMap<Integer, Integer> removedEntities = new HashMap<>(); // Entity ids to old maps

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
		Vector one = Vector.newBuilder()
				.setX(0.8f)
				.setY(0.8f)
				.build();
		
		Entity entity = Entity.newBuilder()
				.setName(name)
				.setMap(mapId)
				.setPosition(vector)
				.setSize(one)
				.setId(currentId)
				.setSpeed(7.5)
				.setVelocity(vector.toBuilder().build())
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
	
	/**
	 * Updates the entity list with the new value and checks for movement both position wise and map change then updates the chunk manager
	 * @param entity
	 */
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
	
	/**
	 * Force fully updates the entity list with the new value and DOES NOT update the chunk manager
	 * @param entity
	 */
	private void forceUpdateEntity(Entity entity) {
		entities.put(entity.getId(), entity);
	}
	
	/**
	 * Removes the given entity next tick
	 * @param entity
	 */
	public void removeEntity(Entity entity) {
		removedEntities.put(entity.getId(),entity.getMap());
		forceUpdateEntity(entity.toBuilder().setMap(0).build());
	}
	
	public void clearRemovedEntities() {
		
		for (int id : removedEntities.keySet()) {
			Entity entity = entities.get(id);
			if (entity != null) entities.remove(id);
			chunkManager.removeEntityFromChunk(entity.toBuilder().setMap(removedEntities.get(id)).build());
		}
		removedEntities.clear();
	}

	public void setChunkManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
	}
}
