package entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ai.PathfindingHandler;
import collision.EntityCollision;
import file.AssetManager;
import file.ServerLoader;
import health.HealthManager;
import main.Console;
import main.Server;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlayerDataProto.PlayerData.Builder;
import protonova.protobuf.VectorProto.Vector;
import simulation.EntitySimulation;
import socket.Player;
import socket.ServerSocketHandler;
import tag.TagHandler;
import util.Id;

public class EntityManager {

	private ConcurrentHashMap<Integer, Entity> entities;
	private ChunkManager chunkManager;
	private Console console;
	private TagHandler tagHandler;
	private ArrayList<Player> playerList;
	private EntityFinder entityFinder;
	private final Set<Integer> velocityEntities = ConcurrentHashMap.newKeySet();
	private final Set<Integer> reservedEntityIds = ConcurrentHashMap.newKeySet();
	private Server server;
	private PathfindingHandler pathfindingHandler;
	private HealthManager healthManager;
	private AssetManager assetManager;

	public EntityManager(ServerLoader serverLoader,Console console, ArrayList<Player> playerList, Server server) {
		entities = new ConcurrentHashMap<>(serverLoader.loadEntities());
		this.console = console;
		this.playerList = playerList;
		this.server = server;
	}

	EntityManager(Map<Integer, Entity> startingEntities) {
		entities = new ConcurrentHashMap<>(startingEntities);
		playerList = new ArrayList<>();
	}
	
	public Entity makeNewEntity(String name,int mapId) {
		if (assetManager == null) throw new IllegalStateException("Asset manager has not been set");
		Entity entity = assetManager.getEntity(name, mapId);
		if (entity == null) throw new IllegalArgumentException("Missing entity asset: " + name);
		updateEntity(entity);
		Entity stored = getEntity(entity.getId());
		if (stored == null) throw new IllegalStateException("Entity failed its spawn check: " + name);
		return stored;
	}
	
	public Entity makeNewEntity(String name) {
		return makeNewEntity(name,1);
	}
	
	public Player getPlayerEntityFromEntity(Entity entity) {
		for (Player player : playerList) {
			if (player.data != null && entity.getId() == player.data.getEntityId()) {
				return player;
			}
		}
		return null;
	}
	
	public void setPlayerEntity(Player player, Entity entity) {
		Builder playerData = player.data.toBuilder();
		playerData = playerData.setEntityId(entity.getId());
		player.data = playerData.build();
	}
	
	public Entity getEntity(int id) {
		return entities.get(id);
	}
	
	public Entity getEntity(Player player) {
		if (player == null || player.data == null) return null;
		return entities.get(player.data.getEntityId());
	}
	
	public Map<Integer,Entity> getAllEntities() {
		return entities;
	}

	/** Returns a stable-enough array snapshot for read-only diagnostics on another thread. */
	public Entity[] getAllEntitiesSnapshot() {
		return entities.values().toArray(new Entity[0]);
	}
	
	public void dropEntityItems(Entity entity) {
		for (Map.Entry<String, Integer> entry : entity.getInventorySlotsMap().entrySet()) {
			Entity item = getEntity(entry.getValue());
			protonova.protobuf.EntityProto.Entity.Builder itemBuilder = item.toBuilder();
			itemBuilder.setMap(entity.getMap());
			itemBuilder.setPosition(entity.getPosition());
			updateEntity(itemBuilder.build());
		}
	}
	
	/**
	 * Updates the entity list with the new value and checks for movement both position wise and map change then updates the chunk manager
	 * @param entity
	 */
	public synchronized void updateEntity(Entity entity) {
		boolean isFirstInsertion = !entities.containsKey(entity.getId())
				|| reservedEntityIds.remove(entity.getId());
		boolean installedOrgan = entity.hasOrganComponent()
				&& entity.getOrganComponent().hasInstalledInEntityId();

		if (entity.getMaxSpeed() <= 0) {
			entity = entity.toBuilder()
					.setMaxSpeed(7.5)
					.build();
		}

		// A spawn must be valid before it is registered with chunks and tags.
		if (isFirstInsertion && healthManager != null) {
			entity = healthManager.prepareEntityState(entity);
		}
		
		if (!isFirstInsertion && !installedOrgan) {
			Entity oldEntity = entities.get(entity.getId());
			
			if (!oldEntity.getPosition().equals(entity.getPosition()) || oldEntity.getMap() != entity.getMap()) {
				chunkManager.updateEntityChunck(oldEntity, entity);
			}
			
			tagHandler.updateEntityTag(entity);
		}
		else if (!installedOrgan) {
			chunkManager.addEntity(entity);
			tagHandler.addEntity(entity);
		}
		
		if (entity.getVelocity().getX() != 0 || entity.getVelocity().getY() != 0) {
			velocityEntities.add(entity.getId());
		} else {
			velocityEntities.remove(entity.getId());
		}
		
		// have items follow the entity
		for (int id : entity.getInventorySlotsMap().values()) {
			Entity item = getEntity(id).toBuilder()
					.setPosition(entity.getPosition())
					.build();

			entities.put(id, item);
		}
		
		sendUpdate(entity);
		entities.put(entity.getId(), entity);

		// Death side effects expect the entity to already exist in the manager.
		if (isFirstInsertion && healthManager != null) {
			healthManager.entityCheck(entity);
		}
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
			
			if (!item.getStackable() || item.getAmount() == 0) {
				entity = entity.toBuilder()
						.removeInventorySlots(slot)
						.build();
			}
		}
		else System.err.print("[Entity Manager] Error: Could not find item in slot "+slot);
		
		return entity;
	}
	
	/**
	 * Removes the given entity next tick
	 * @param entity
	 */
	public synchronized void removeEntity(Entity entity) {
		reservedEntityIds.remove(entity.getId());
		if (entity.hasOrganSlots()) {
			removeInstalledOrgan(entity.getOrganSlots().hasHeartEntityId() ? entity.getOrganSlots().getHeartEntityId() : -1);
			removeInstalledOrgan(entity.getOrganSlots().hasLungsEntityId() ? entity.getOrganSlots().getLungsEntityId() : -1);
			removeInstalledOrgan(entity.getOrganSlots().hasLiverEntityId() ? entity.getOrganSlots().getLiverEntityId() : -1);
			removeInstalledOrgan(entity.getOrganSlots().hasBrainEntityId() ? entity.getOrganSlots().getBrainEntityId() : -1);
			removeInstalledOrgan(entity.getOrganSlots().hasStomachEntityId() ? entity.getOrganSlots().getStomachEntityId() : -1);
		}
		if (velocityEntities.contains(entity.getId())) velocityEntities.remove(entity.getId());
		sendDeletion(entity);
		tagHandler.removeEntity(entity);
		chunkManager.removeEntityFromChunk(entity);
		pathfindingHandler.removeEntity(entity);
		entities.remove(entity.getId());
	}

	private void removeInstalledOrgan(int id) {
		if (id < 0) return;
		Entity organ = entities.get(id);
		if (organ != null) removeEntity(organ);
	}

	public void setClasses(ChunkManager chunkManager, TagHandler tagHandler, EntityFinder entityFinder, PathfindingHandler pathfindingHandler) {
		this.chunkManager = chunkManager;
		this.tagHandler = tagHandler;
		this.entityFinder = entityFinder;
		this.pathfindingHandler = pathfindingHandler;
	}

	public void setAssetManager(AssetManager assetManager) {
		this.assetManager = assetManager;
	}

	/**
	 * Installs health validation and normalizes entities loaded from an existing
	 * world before the server starts ticking.
	 */
	public synchronized void setHealthManager(HealthManager healthManager) {
		this.healthManager = healthManager;
		if (healthManager == null) return;
		entities.replaceAll((id, entity) -> healthManager.prepareEntityState(
				entity.toBuilder()
						.setHitDamage(entity.getHitDamage().toBuilder().setCanAttack(true))
						.build()));
	}
	
	/**
	 * Reserves a new entity id. Be cautious when using this to not make a ton of empty entities
	 * @return A entity ID that has been newly reserved
	 */
	public synchronized int reserveNewEntityId() {
		Set<Integer> unavailableIds = new HashSet<>(entities.keySet());
		unavailableIds.addAll(reservedEntityIds);
		int newId = Id.getNewId(unavailableIds);
		reservedEntityIds.add(newId);
		entities.put(newId, Entity.newBuilder().build());
		
		return newId;
	}
	
	private void sendUpdate(Entity entity) {
		for (Player player : playerList) {
			player.updateList.add(entity.getId());
		}
	}
	
	private void sendDeletion(Entity entity) {
		for (Player player : playerList) {
			player.deleteList.add(entity.getId());
		}
	}
	
	public boolean entityExist(Entity entity) {
	    if (entity == null) return false;

	    Entity stored = entities.get(entity.getId());
	    return stored != null;
	}
	
	public boolean entityExist(int id) {
		return entities.containsKey(id);
	}
	
	/**
	 * Ticks all the velocity for all entities
	 * @return stop reading this
	 */
	public synchronized void tick() {
		diagnostics.ResourceDiagnostics resourceDiagnostics = server.getDiagnostics();
		boolean measureCpu = resourceDiagnostics != null && resourceDiagnostics.isCapturing();
		for (int id : velocityEntities) {
			if (entities.containsKey(id)) {
				Entity entity = entities.get(id);
				long started = measureCpu ? System.nanoTime() : 0;
				
				try {
					entity = simulateVelocity(entity, server.TPS);
					updateEntity(entity);
				} finally {
					if (measureCpu) {
						resourceDiagnostics.recordEntityCpu(id, System.nanoTime() - started);
					}
				}
				
			}
			else velocityEntities.remove(id);
		}
	}
	
	
	/**
	 * Force fully updates the entity list with the new value and DOES NOT update the chunk manager
	 * @param entity
	 */
	private synchronized void forceUpdateEntity(Entity entity) {
		sendUpdate(entity);
		entities.put(entity.getId(), entity);
	}
	
	/**
	 * Simulates the velocity of the given entity
	 * @param entity the given entity
	 * @return entity with modified position based on velocity
	 */
	public Entity simulateVelocity(Entity entity, int tps) {
		return simulateVelocity(entity, tps, 1.0f);
	}

	public Entity simulateVelocity(Entity entity, int tps, float speedMultiplier) {
		
		ArrayList<Entity> closeEntities = entityFinder.getAllEntitiesInRadis(entity, 10);
		
		Entity entityXAxis = checkCollision(EntitySimulation.simulateVelocityXAxis(entity,tps,speedMultiplier),entity,closeEntities);
		
		// check if we actualy did anything
		if (entityXAxis.getPosition().equals(entity.getPosition())) {
			// if not then just remove the veloicty
			
			Vector newVeloicty = entity.getVelocity().toBuilder()
					.setX(0)
					.build();
			
			entity = entity.toBuilder()
					.setVelocity(newVeloicty)
					.build();
		}
		else entity = entityXAxis;
		
		Entity entityYAxis = checkCollision(EntitySimulation.simulateVelocityYAxis(entity,tps,speedMultiplier),entity,closeEntities);
		
		// check if we actualy did anything
		if (entityYAxis.getPosition().equals(entity.getPosition())) {
			// if not then just remove the veloicty
			
			Vector newVeloicty = entity.getVelocity().toBuilder()
					.setY(0)
					.build();
			
			entity = entity.toBuilder()
					.setVelocity(newVeloicty)
					.build();
		}
		else entity = entityYAxis;

		return EntitySimulation.slowItemVelocity(entity, tps);
	}
	
	private Entity checkCollision(Entity updatedEntity, Entity originalEntity, ArrayList<Entity> closeEntities) {
		if (originalEntity.getPosition().equals(updatedEntity.getPosition())) return originalEntity;
		
		for (Entity entity : closeEntities) {
			if (entity.getId() != originalEntity.getId() && entity.getMap() == originalEntity.getMap()) {
				boolean alreadyOverlapping = EntityCollision.checkCollision(originalEntity, entity);
				if (entity.getCanCollide() && EntityCollision.checkCollision(updatedEntity, entity)) {
					boolean movingAway = EntityCollision.isMovingAway(originalEntity, updatedEntity, entity);
					if (!alreadyOverlapping || !movingAway) return originalEntity;
				}
			}
		}
		
		return updatedEntity;
	}
	
	public void recalculateEntity(Entity entity) {
		if (entity == null) return;
		Entity stored = getEntity(entity.getId());
		if (stored == null) return;
		Entity recalculated = healthManager == null
				? stored
				: healthManager.prepareEntityState(stored);
		updateEntity(recalculated);
	}
}
