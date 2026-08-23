package health;

import entity.EntityManager;
import entity.LootTableManager;
import health.Health.TraumaState;
import main.Console;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.OrganSlots;
import socket.Player;
import util.ItemDropVelocity;

public class HealthManager {

	public CombatManager combatManager;
	private final EntityManager entityManager;
	private final LootTableManager lootTableManager;

	public HealthManager(EntityManager entityManager, Console console, LootTableManager lootTableManager) {
		this.entityManager = entityManager;
		this.lootTableManager = lootTableManager;
	}


	public Entity prepareEntityState(Entity entity) {
		if (entity == null) return null;

		double maximumSpeed = entity.getMaxSpeed() > 0 ? entity.getMaxSpeed() : 7.5;
		if (!usesHealthSystem(entity)) {
			return entity.toBuilder()
					.setMaxSpeed(maximumSpeed)
					.build();
		}

		double totalDamage = Health.getDamage(entity);
		boolean alive = entity.getAlive() && totalDamage < entity.getMaxHealth();
		double speedMultiplier = calculateHealthSpeedMultiplier(entity, totalDamage, alive);

		return entity.toBuilder()
				.setAlive(alive)
				.setMaxSpeed(maximumSpeed)
				.setSpeed(maximumSpeed * speedMultiplier)
				.build();
	}

	public Entity entityCheck(Entity entity) {
		if (entity == null) return null;
		Entity stored = entityManager.getEntity(entity.getId());
		if (stored == null) return prepareEntityState(entity);

		Entity checked = prepareEntityState(stored);
		if (!checked.equals(stored)) entityManager.updateEntity(checked);

		Player player = entityManager.getPlayerEntityFromEntity(checked);
		if (checkCrit(checked) && player != null) {
			Entity replacement = entityManager.makeNewEntity("human", checked.getMap());
			entityManager.setPlayerEntity(player, replacement);
			entityManager.dropEntityItems(checked);
			//entityManager.removeEntity(checked);
			return replacement;
		}

		if (checkDeath(checked)) {
			if (!checked.getDropsABody()) {
				lootTableManager.dropLoot(checked);
				entityManager.removeEntity(checked);
			} else if (Health.getDamage(checked) >= checked.getMaxHealth() * 2.5) {
				gibEntity(checked);
			}
		}

		return checked;
	}

	public boolean checkCrit(Entity entity) {
		return usesHealthSystem(entity)
				&& entity.getCritHealth() <= Health.getDamage(entity);
	}

	public boolean canPerformActions(Entity entity) {
		return entity != null && entity.getAlive() && !checkCrit(entity);
	}

	public Entity changeDeathState(Entity entity, boolean alive) {
		if (entity == null) return null;
		Entity stored = entityManager.getEntity(entity.getId());
		Entity source = stored != null ? stored : entity;
		return source.toBuilder().setAlive(alive).build();
	}

	public boolean checkDeath(Entity entity) {
		return usesHealthSystem(entity)
				&& entity.getMaxHealth() <= Health.getDamage(entity);
	}

	private boolean usesHealthSystem(Entity entity) {
		if (entity == null) {
			return false;
		}
		if (entity.getMaxHealth() <= 0) {
			return false;
		}
		return !entity.getIsItem() || entity.getCanDestroy();
	}

	private static double calculateHealthSpeedMultiplier(Entity entity, double damage, boolean alive) {
		if (!alive || damage >= entity.getCritHealth()) return 0;

		double criticalThreshold = entity.getCritHealth();
		if (damage >= criticalThreshold * TraumaState.MORTALLY_WOUNDED.getTraumaPercentAsDecimal()) return 0.3;
		if (damage >= criticalThreshold * TraumaState.SEVERELY_INJURED.getTraumaPercentAsDecimal()) return 0.6;
		if (damage >= criticalThreshold * TraumaState.INJURED.getTraumaPercentAsDecimal()) return 0.75;
		if (damage >= criticalThreshold * TraumaState.MINOR_INJURIES.getTraumaPercentAsDecimal()) return 0.9;
		return 1;
	}

	private void gibEntity(Entity entity) {
		lootTableManager.dropEverythingIncludingInsides(entity);
		entityManager.dropEntityItems(entity);
		dropOrgans(entity);
		Entity bodyWithoutInstalledOrgans = entity.toBuilder()
				.clearOrganSlots()
				.build();
		entityManager.removeEntity(bodyWithoutInstalledOrgans);
	}

	private void dropOrgans(Entity entity) {
		if (!entity.hasOrganSlots()) {
			return;
		}

		OrganSlots installedOrgans = entity.getOrganSlots();

		if (installedOrgans.hasHeartEntityId()) {
			dropOrgan(installedOrgans.getHeartEntityId(), entity);
		}
		if (installedOrgans.hasLungsEntityId()) {
			dropOrgan(installedOrgans.getLungsEntityId(), entity);
		}
		if (installedOrgans.hasLiverEntityId()) {
			dropOrgan(installedOrgans.getLiverEntityId(), entity);
		}
		if (installedOrgans.hasBrainEntityId()) {
			dropOrgan(installedOrgans.getBrainEntityId(), entity);
		}
		if (installedOrgans.hasStomachEntityId()) {
			dropOrgan(installedOrgans.getStomachEntityId(), entity);
		}
	}

	private void dropOrgan(int organEntityId, Entity body) {
		Entity organ = entityManager.getEntity(organEntityId);
		if (organ == null) {
			return;
		}

		Entity.Builder droppedOrgan = organ.toBuilder()
				.setMap(body.getMap())
				.setPosition(body.getPosition())
				.setVelocity(ItemDropVelocity.createRandomScatterVelocity())
				.setAnchored(false)
				.setAmount(1);

		if (organ.hasOrganComponent()) {
			droppedOrgan.setOrganComponent(
					organ.getOrganComponent().toBuilder().clearInstalledInEntityId());
		}

		entityManager.updateEntity(droppedOrgan.build());
	}

}
