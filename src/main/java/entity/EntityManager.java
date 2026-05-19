package entity;

import java.util.HashMap;
import java.util.HashSet;

import file.ServerLoader;
import main.Console;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import socket.Player;
import util.Id;

public class EntityManager {

	private HashMap<Integer, Entity> entities;
	private ChunkManager chunkManager;
	private Console console;
	private final HashSet<Integer> updatedEntities = new HashSet<>();
	private final HashSet<Integer> removedEntities = new HashSet<>();

	public EntityManager(ServerLoader serverLoader,Console console) {
		entities = serverLoader.loadEntities();
		this.console = console;
	}
	
	public Entity makeNewEntity(String name,int mapId) {
		
		int currentId = Id.getNewId(entities.keySet(), removedEntities);
		
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
		updatedEntities.add(entity.getId());
		entities.put(entity.getId(), entity);
	}
	
	/**
	 * Decrements the amount of a entity
	 * Also force updates the entity with the new amount
	 * @param entity to decrement
	 */
	public Entity decrementAmount(Entity entity) {
		
		if (entity.getStackable()) {
			entity = entity.toBuilder()
					.setAmount(entity.getAmount()-1)
					.build();
			
			if (entity.getAmount() == 0) removeEntity(entity);
			else forceUpdateEntity(entity);
		}
		else removeEntity(entity);
		
		return entity;
	}
	
	/**
	 * 
	 * @return the entity with decremented item slot (it could be removed)
	 */
	public Entity decrementSlot(Entity entity, String slot) {
		
		Entity item = getEntity(entity.getInventorySlotsMap().get(slot));
		
		if (item != null) {
			item = decrementAmount(item);
			
			if (isEntityRemoved(item)) {
				entity = entity.toBuilder()
						.removeInventorySlots(slot)
						.build();
			}
		}
		else System.err.print("Error: Could not find item in slot "+slot);
		
		return entity;
	}
	
	/**
	 * Force fully updates the entity list with the new value and DOES NOT update the chunk manager
	 * @param entity
	 */
	private void forceUpdateEntity(Entity entity) {
		updatedEntities.add(entity.getId());
		entities.put(entity.getId(), entity);
	}
	
	/**
	 * Removes the given entity next tick
	 * @param entity
	 */
	public void removeEntity(Entity entity) {
		removedEntities.add(entity.getId());
		chunkManager.removeEntityFromChunk(entity);
		entities.remove(entity.getId());
	}
	
	public void clearHashSets() {
		removedEntities.clear();
		updatedEntities.clear();
	}
	
	public boolean isEntityRemoved(int id) {
		return removedEntities.contains(id);
	}
	
	public boolean isEntityRemoved(Entity entity) {
		return isEntityRemoved(entity.getId());
	}
	
	public boolean isEntityUpdated(int id) {
		return updatedEntities.contains(id);
	}
	
	public boolean isEntityUpdated(Entity entity) {
		return isEntityUpdated(entity.getId());
	}

	public void setChunkManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
	}
	
	/**
	 * Reserves a new entity id. Be cautious when using this to not make a ton of empty entities
	 * @return A entity ID that has been newly reserved
	 */
	public int reserveNewEntityId() {
		int newId = Id.getNewId(entities.keySet());
		entities.put(newId, Entity.newBuilder().build());
		
		return newId;
	}
}
