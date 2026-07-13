package health;

import entity.EntityManager;
import entity.LootTableManager;
import main.Console;
import protonova.protobuf.EntityProto.Entity;
import socket.Player;

public class HealthManager {
	
	public CombatManager combatManager;
	private EntityManager entityManager;
	private Console console;
	private LootTableManager lootTableManager;
	
	public HealthManager(EntityManager entityManager, Console console, LootTableManager lootTableManager) {
		this.entityManager = entityManager;
		this.console = console;
		this.lootTableManager = lootTableManager;
	}
	
	public Entity entityCheck(Entity entity) {
		entityManager.recalculateEntity(entity);
		entity = entityManager.getEntity(entity.getId());
		Entity newEntity = entity;
		newEntity = checkDeathOfEntity(entity);
		if (!entity.getAlive()) return newEntity;
		if (checkDeath(entity)) {
			newEntity = changeDeathState(entity, false);
		}
		Player player = entityManager.getPlayerEntityFromEntity(entity);
		if (checkCrit(entity) && player != null) {
			newEntity = entityManager.makeNewEntity("human", entity.getMap());
			System.out.println("new id: "+newEntity.getId());
			entityManager.setPlayerEntity(player, newEntity);
			entityManager.dropEntityItems(entity);
			entityManager.removeEntity(entity);
		}
		return newEntity;
	}
	
	public boolean checkCrit(Entity entity) {
		return (entity.getCritHealth() <= Health.getDamage(entity));
	}
	
	public Entity changeDeathState(Entity entity, boolean alive) {
		entity = entityManager.getEntity(entity.getId());
		Entity.Builder entityBuilder = entity.toBuilder();
		entityBuilder.setAlive(alive);
		return entityBuilder.build();
	}
	
	public boolean checkDeath(Entity entity) {
		return (entity.getMaxHealth() <= Health.getDamage(entity));
	}
	
	private void gibEntity(Entity entity) {
		dropOrgans(entity);
		entityManager.removeEntity(entity);
	}
	
	private void dropOrgans(Entity entity) {
		// TODO: Add organs droping after adding organs
	}
	
	private Entity checkDeathOfEntity(Entity entity) {
		if (Health.getDamage(entity) >= entity.getMaxHealth()) {
			if (!entity.getDropsABody()) {
				lootTableManager.dropLoot(entity);
				entityManager.removeEntity(entity);
			} else {
				if (entity.getMaxHealth() * 2.5 <= Health.getDamage(entity)) {
					gibEntity(entity);
				}
			}
		}
		return entity;
	}
}
