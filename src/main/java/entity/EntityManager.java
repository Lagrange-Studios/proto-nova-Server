package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import collision.EntityCollision;
import file.ServerLoader;
import main.Console;
import main.Server;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.DamageMultiplier;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import simulation.EntitySimulation;
import socket.Player;
import tag.TagHandler;
import util.Id;

public class EntityManager {

	private HashMap<Integer, Entity> entities;
	private ChunkManager chunkManager;
	private Console console;
	private TagHandler tagHandler;
	private ArrayList<Player> playerList;
	private EntityFinder entityFinder;
	private final HashSet<Integer> velocityEntities = new HashSet<>();
	private Server server;

	public EntityManager(ServerLoader serverLoader,Console console, ArrayList<Player> playerList, Server server) {
		entities = serverLoader.loadEntities();
		this.console = console;
		this.playerList = playerList;
		this.server = server;
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
		DamageMultiplier damageMult = DamageMultiplier.newBuilder()
			    .setBrute(1)
			    .setAsphyxiation(1)
			    .setBurn(1)
			    .setToxin(1)
			    .setGenetic(1)
			    .setStructural(1)
			    .setBleeding(1)
			    .build();
		HitDamage hitDamage = HitDamage.newBuilder()
			    .setBruteDamage(1)
			    .setAsphyxiationDamage(0)
			    .setBurnDamage(0)
			    .setToxinDamage(0)
			    .setGeneticDamage(0)
			    .setStructuralDamage(0)
			    .setBleedingPerTick(0)
			    .setHitCooldown(1000)
			    .setCanAttack(true)
			    .build();
		Damage damage = Damage.newBuilder()
				.setBruteDamage(0)
				.setAsphyxiationDamage(0)
				.setBurnDamage(0)
				.setToxinDamage(0)
				.setGeneticDamage(0)
				.setStructuralDamage(0)
				.setBleedingPerTick(0)
				.setDamageMultiplier(damageMult)
				.build();
		
		Entity entity = Entity.newBuilder()
				.setName(name)
				.setMap(mapId)
				.setPosition(vector)
				.setSize(one)
				.setId(currentId)
				.setSpeed(7.5)
				.setMaxSpeed(7.5)
				.setMaxHealth(200)
				.setCritHealth(100)
				.setAlive(true)
				.setVelocity(vector.toBuilder().build())
				.setDirection(Direction.Down)
				.setSelectedSlot("leftHand")
				.setDamage(damage)
				.setHitDamage(hitDamage)
				.setReach(1.5)
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
		if (entity.getMaxSpeed() <= 0) {
			entity = entity.toBuilder()
					.setMaxSpeed(7.5)
					.build();
		}
		
		if (entities.containsKey(entity.getId())) {
			Entity oldEntity = entities.get(entity.getId());
			
			if (!oldEntity.getPosition().equals(entity.getPosition()) || oldEntity.getMap() != entity.getMap()) {
				chunkManager.updateEntityChunck(oldEntity, entity);
			}
			
			tagHandler.updateEntity(oldEntity, entity);
		}
		else {
			chunkManager.addEntity(entity);
			tagHandler.addEntity(entity);
		}
		
		if ((entity.getVelocity().getX() != 0 || entity.getVelocity().getY() != 0) &&
				!velocityEntities.contains(entity.getId())) {
			velocityEntities.add(entity.getId());
		}
		
		sendUpdate(entity);
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
			
			if (!entity.getStackable() || item.getAmount() == 0) {
				entity = entity.toBuilder()
						.removeInventorySlots(slot)
						.build();
			}
		}
		else System.err.print("Error: Could not find item in slot "+slot);
		
		return entity;
	}
	
	/**
	 * Removes the given entity next tick
	 * @param entity
	 */
	public void removeEntity(Entity entity) {
		sendDeletion(entity);
		chunkManager.removeEntityFromChunk(entity);
		entities.remove(entity.getId());
	}

	public void setClasses(ChunkManager chunkManager, TagHandler tagHandler, EntityFinder entityFinder) {
		this.chunkManager = chunkManager;
		this.tagHandler = tagHandler;
		this.entityFinder = entityFinder;
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
	
	/**
	 * Ticks all the velocity for all entities
	 * @return stop reading this
	 */
	public void tick() {
		for (int id : velocityEntities.toArray(new Integer[0])) {
			Entity entity = entities.get(id);
			entity = simulateVelocity(entity);
			updateEntity(entity);
		}
	}
	
	
	/**
	 * Force fully updates the entity list with the new value and DOES NOT update the chunk manager
	 * @param entity
	 */
	private void forceUpdateEntity(Entity entity) {
		sendUpdate(entity);
		entities.put(entity.getId(), entity);
	}
	
	/**
	 * Simulates the velocity of the given entity
	 * @param entity the given entity
	 * @return entity with modified position based on velocity
	 */
	public Entity simulateVelocity(Entity entity) {
		
		ArrayList<Entity> closeEntities = entityFinder.getAllEntitiesInRadis(entity, 10);
		
		Entity entityXAxis = checkCollision(EntitySimulation.simulateVelocityXAxis(entity,server.TPS),entity,closeEntities);
		
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
		
		Entity entityYAxis = checkCollision(EntitySimulation.simulateVelocityYAxis(entity,server.TPS),entity,closeEntities);
		
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

		return entity;
	}
	
	private Entity checkCollision(Entity updatedEntity, Entity originalEntity, ArrayList<Entity> closeEntities) {
		if (originalEntity.getPosition().equals(updatedEntity.getPosition())) return originalEntity;
		
		for (Entity entity : closeEntities) {
			if (entity.getId() != originalEntity.getId()) {
				//if (EntityCollision.checkCollision(updatedEntity, entity)) System.out.println(true);
				if (entity.getCanCollide() && EntityCollision.checkCollision(updatedEntity, entity)) return originalEntity;
			}
		}
		
		return updatedEntity;
	}
}
