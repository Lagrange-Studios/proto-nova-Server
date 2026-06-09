package health;

import entity.EntityManager;
import entity.LootTableManager;
import main.Console;
import protonova.protobuf.EntityProto.Entity;

public class HealthManager {
	
	public CombatManager combatManager;
	private EntityManager entityManager;
	private Console console;
	private LootTableManager lootTableManager;
	
	public HealthManager(CombatManager combatManager, EntityManager entityManager, Console console, LootTableManager lootTableManager) {
		this.combatManager = combatManager;
		this.entityManager = entityManager;
		this.console = console;
		this.lootTableManager = lootTableManager;
	}
	
	public void entityCheck(Entity entity) {
		healthCheck(entity);
		entity = entityManager.getEntity(entity.getId());
		checkDeathOfEntity(entity);
		if (!entity.getAlive()) return;
		if (checkDeath(entity)) {
			changeDeathState(entity, false);
		}
		checkCrit(entity);
	}
	
	public boolean checkCrit(Entity entity) {
		return (entity.getCritHealth() <= combatManager.getDamage(entity));
	}
	
	public void changeDeathState(Entity entity, boolean alive) {
		entity = entityManager.getEntity(entity.getId());
		Entity.Builder entityBuilder = entity.toBuilder();
		entityBuilder.setAlive(alive);
		entityManager.updateEntity(entityBuilder.build());
	}
	
	public boolean checkDeath(Entity entity) {
		return (entity.getMaxHealth() <= combatManager.getDamage(entity));
	}
	
	private void healthCheck(Entity entity) {
		changeSpeedFromHealth(entity);
	}
	
	private void changeSpeedFromHealth(Entity entity) {
		entity = entityManager.getEntity(entity.getId());
		double totalDamage = combatManager.getDamage(entity);
		double critHealthThreshold = entity.getCritHealth();
		double healthSpeedMult = 1;
		
		if (totalDamage >= critHealthThreshold || !entity.getAlive()) {
			healthSpeedMult = 0;
		} else if (totalDamage >= critHealthThreshold * 0.90) {
			healthSpeedMult = 0.1;
		} else if (totalDamage >= critHealthThreshold * 0.75) {
			healthSpeedMult = 0.3;
		} else if (totalDamage >= critHealthThreshold * 0.50) {
			healthSpeedMult = 0.5;
		} else if (totalDamage >= critHealthThreshold * 0.35) {
			healthSpeedMult = 0.6;
		} else if (totalDamage >= critHealthThreshold * 0.10) {
			healthSpeedMult = 0.9;
		}
		
		Entity.Builder entityBuilder = entity.toBuilder();
		entityBuilder.setSpeed(entity.getMaxSpeed() * healthSpeedMult);
		Entity updatedEntity = entityBuilder.build();
		entityManager.updateEntity(updatedEntity);
	}
	
	
	
	private void gibEntity() {
		return;
	}
	
	private void checkDeathOfEntity(Entity entity) {
		if (combatManager.getDamage(entity) >= entity.getMaxHealth()) {
			if (!entity.getDropsABody()) {
				lootTableManager.dropLoot(entity);
				entityManager.removeEntity(entity);
			} else {
				
			}
		}
	}
}
