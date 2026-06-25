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
		entityManager.recalculateEntity(entity);
		entity = entityManager.getEntity(entity.getId());
		checkDeathOfEntity(entity);
		if (!entity.getAlive()) return;
		if (checkDeath(entity)) {
			changeDeathState(entity, false);
		}
		checkCrit(entity);
	}
	
	public boolean checkCrit(Entity entity) {
		return (entity.getCritHealth() <= Health.getDamage(entity));
	}
	
	public void changeDeathState(Entity entity, boolean alive) {
		entity = entityManager.getEntity(entity.getId());
		Entity.Builder entityBuilder = entity.toBuilder();
		entityBuilder.setAlive(alive);
		entityManager.updateEntity(entityBuilder.build());
	}
	
	public boolean checkDeath(Entity entity) {
		return (entity.getMaxHealth() <= Health.getDamage(entity));
	}
	
	private void gibEntity(Entity entity) {
		dropOrgans(entity);
		entityManager.removeEntity(entity);
		return;
	}
	
	private void dropOrgans(Entity entity) {
		// TODO: Add organs droping after adding organs
	}
	
	private void checkDeathOfEntity(Entity entity) {
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
	}
}
