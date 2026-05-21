package health;

import entity.EntityManager;
import protonova.protobuf.EntityProto.Entity;

public class HealthManager {
	
	CombatManager combatManager;
	EntityManager entityManager;
	
	public HealthManager(CombatManager combatManager, EntityManager entityManager) {
		this.combatManager = combatManager;
		this.entityManager = entityManager;
	}
	
	public void entityCheck(Entity entity) {
		if (!entity.getAlive()) return;
		checkDeath(entity);
		checkCrit(entity);
		
	}
	
	public boolean checkCrit(Entity entity) {
		return (entity.getCritHealth() <= combatManager.getDamage(entity));
	}
	
	public void changeDeathState(Entity entity, boolean alive) {
		Entity.Builder entityBuilder = entity.toBuilder();
		entityBuilder.setAlive(alive);
		entityManager.updateEntity(entityBuilder.build());
	}
	
	private boolean checkDeath(Entity entity) {
		changeDeathState(entity, false);
		return (entity.getMaxHealth() <= combatManager.getDamage(entity));
	}
	
}
