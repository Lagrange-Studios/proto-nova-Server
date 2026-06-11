package health;

import entity.EntityManager;
import main.Console;
import protonova.protobuf.EntityProto.Entity;

public class HealthManager {
	
	public CombatManager combatManager;
	private EntityManager entityManager;
	private Console console;
	
	public HealthManager(CombatManager combatManager, EntityManager entityManager, Console console) {
		this.combatManager = combatManager;
		this.entityManager = entityManager;
		this.console = console;
	}
	
	public void entityCheck(Entity entity) {
		healthCheck(entity);
		entity = entityManager.getEntity(entity.getId());
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
}
