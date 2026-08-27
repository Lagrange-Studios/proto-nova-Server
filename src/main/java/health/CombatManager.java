package health;

import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import entity.EntityManager;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.DamageMultiplier;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.EntityProto.Entity.Builder;
import sound.SoundManager;
import util.AudioBuilder;
import util.TimedTask;
import util.VectorMath;
import diagnostics.ResourceDiagnostics;

public class CombatManager {
	
	private EntityManager entityManager;
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4,
			ResourceDiagnostics.threadFactory("Combat-Scheduler"));
	private HealthManager healthManager;
	private SoundManager soundManager;
	private final ConcurrentHashMap<Integer, Long> attackCooldownDeadlines = new ConcurrentHashMap<>();
	
	public CombatManager(EntityManager entityManager, HealthManager healthManager, SoundManager soundManager) {
		this.entityManager = entityManager;
		this.healthManager = healthManager;
		this.soundManager = soundManager;
	}
	
	public boolean attemptToDamage(Entity attacker, Entity defender) {
		if (!entityManager.entityExist(attacker) || !entityManager.entityExist(defender)) {
			return false;
		}

		attacker = entityManager.getEntity(attacker.getId());
		defender = entityManager.getEntity(defender.getId());
		if (attacker == null || defender == null) return false;
		if (!canReceiveAttack(defender)) return false;

		if (attacker.getReach() >= VectorMath.distance(attacker.getPosition(), defender.getPosition())
				&& isAttackReady(attacker.getId())) {
			
			startEntityHitCooldown(attacker);
			damage(attacker, defender);
			playHitSound(attacker, defender);
			return true;
		} else {
			return false;
			
		}
	}

	private void playHitSound(Entity attacker, Entity defender) {
		soundManager.emit(AudioBuilder.createSoundEffect(
				"Hit", defender.getPosition(), defender.getMap()).toBuilder()
				.setOriginEntityID(attacker.getId())
				.build());
	}

	/** Items are protected from attacks unless their asset explicitly opts in. */
	public static boolean canReceiveAttack(Entity entity) {
		return entity != null && (!entity.getIsItem() || entity.getCanDestroy());
	}
	
	public void startEntityHitCooldown(Entity entity) {
		int cooldownTime = getHitCooldown(entity);
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(cooldownTime);
		attackCooldownDeadlines.put(entity.getId(), deadline);

		Entity updated = entity.toBuilder().setHitDamage(
				entity.getHitDamage().toBuilder()
				.setCanAttack(false)).build();
		entityManager.updateEntity(updated);
		startTimedCooldown(entity.getId(), cooldownTime, deadline);
	}
	
	private int getHitCooldown(Entity entity) {
		int cooldownTime = entity.getHitDamage().getHitCooldown();
		if (entity.getInventorySlotsMap().containsKey(entity.getSelectedSlot())) {
			int heldItemEntityId = entity.getInventorySlotsMap().get(entity.getSelectedSlot());
			Entity heldItem = entityManager.getEntity(heldItemEntityId);
			if (heldItem != null) {
				cooldownTime = heldItem.getHitDamage().getHitCooldown();
			}
		}
		return Math.max(0, cooldownTime);
	}

	private boolean isAttackReady(int entityId) {
		Long deadline = attackCooldownDeadlines.get(entityId);
		return deadline == null || System.nanoTime() >= deadline;
	}

	private void startTimedCooldown(int entityId, int cooldownTime, long deadline) {
		
		TimedTask task = new TimedTask(
	            () -> {
	             	resetCooldown(entityId, deadline);
	            },
	            cooldownTime,
	            TimeUnit.MILLISECONDS,
	            scheduler
	    );
	}
	
	private void resetCooldown(int entityId, long deadline) {
		if (!attackCooldownDeadlines.remove(entityId, deadline)) return;

		Entity entity = entityManager.getEntity(entityId);
		if (entity == null) {
			return;
		}
		Entity updated = entity.toBuilder().setHitDamage(
				entity.getHitDamage().toBuilder()
				.setCanAttack(true)).build();
		entityManager.updateEntity(updated);
	}
	
	private void damage(Entity attacker, Entity defender) {
		defender = entityManager.getEntity(defender.getId());
		attacker = entityManager.getEntity(attacker.getId());
		//System.out.println(Health.getDamage(defender));
		checkDamageMults(defender);
		
		HitDamage hitDamage = attacker.getHitDamage();
		if (attacker.getInventorySlotsMap().containsKey(attacker.getSelectedSlot())) {
			int itemID = attacker.getInventorySlotsMap().get(attacker.getSelectedSlot());
			hitDamage = entityManager.getEntity(itemID).getHitDamage();
		}
		
		defender = entityManager.getEntity(defender.getId());
		DamageMultiplier damageMultipliers = defender.getDamage().getDamageMultiplier();
		Damage currentDefenderDamage = defender.getDamage();
		Entity.Builder defenderBuilder = defender.toBuilder();
		
		Damage.Builder entityDamage = currentDefenderDamage.toBuilder();
		
		if (hitDamage.hasBruteDamage()) {
			
			float damage = (hitDamage.getBruteDamage() * damageMultipliers.getBrute());
			
			entityDamage.setBruteDamage(currentDefenderDamage.getBruteDamage() + damage);
		}
		if (hitDamage.hasBurnDamage()) {
			
			float damage = (hitDamage.getBurnDamage() * damageMultipliers.getBurn());
			
			entityDamage.setBurnDamage(currentDefenderDamage.getBurnDamage() + damage);
		}
		if (hitDamage.hasToxinDamage()) {
			
			float damage = (hitDamage.getToxinDamage() * damageMultipliers.getToxin());
			
			entityDamage.setToxinDamage(currentDefenderDamage.getToxinDamage() + damage);
		}
		if (hitDamage.hasAsphyxiationDamage()) {
			
			float damage = (hitDamage.getAsphyxiationDamage() * damageMultipliers.getAsphyxiation());
			
			entityDamage.setAsphyxiationDamage(currentDefenderDamage.getAsphyxiationDamage() + damage);
		}
		if (hitDamage.hasGeneticDamage()) {
			
			float damage = (hitDamage.getGeneticDamage() * damageMultipliers.getGenetic());
			
			entityDamage.setGeneticDamage(currentDefenderDamage.getGeneticDamage() + damage);
		}
		if (hitDamage.hasStructuralDamage()) {
			
			float damage = (hitDamage.getStructuralDamage() * damageMultipliers.getStructural());
			
			entityDamage.setStructuralDamage(currentDefenderDamage.getStructuralDamage() + damage);
		}
		if (hitDamage.hasBleedingPerTick()) {
			
			float damage = (hitDamage.getBleedingPerTick() * damageMultipliers.getBleeding());
			
			entityDamage.setBleedingPerSecond(currentDefenderDamage.getBleedingPerSecond() + damage);
		}
		
		Entity defenderFinal = defenderBuilder.setDamage(entityDamage.build()).build();
		entityManager.updateEntity(defenderFinal);
		healthManager.entityCheck(defenderFinal);
		
	}
	
	private void checkDamageMults(Entity entity) {
		if (!entity.getDamage().getDamageMultiplier().hasBrute()) {
			DamageMultiplier.Builder damageMultBuilder = entity.getDamage().getDamageMultiplier().toBuilder();
			damageMultBuilder.setBrute(1);
			entity = entity.toBuilder()
					.setDamage(entity.getDamage().toBuilder()
							.setDamageMultiplier(damageMultBuilder.build())
							.build())
					.build();
			entityManager.updateEntity(entity);
		}
		 if (!entity.getDamage().getDamageMultiplier().hasBurn()) {
			DamageMultiplier.Builder damageMultBuilder = entity.getDamage().getDamageMultiplier().toBuilder();
			damageMultBuilder.setBurn(1);
			entity = entity.toBuilder()
					.setDamage(entity.getDamage().toBuilder()
							.setDamageMultiplier(damageMultBuilder.build())
							.build())
					.build();
			entityManager.updateEntity(entity);
		}
		 if (!entity.getDamage().getDamageMultiplier().hasToxin()) {
			DamageMultiplier.Builder damageMultBuilder = entity.getDamage().getDamageMultiplier().toBuilder();
			damageMultBuilder.setToxin(1);
			entity = entity.toBuilder()
					.setDamage(entity.getDamage().toBuilder()
							.setDamageMultiplier(damageMultBuilder.build())
							.build())
					.build();
			entityManager.updateEntity(entity);
		}
		 if (!entity.getDamage().getDamageMultiplier().hasAsphyxiation()) {
			DamageMultiplier.Builder damageMultBuilder = entity.getDamage().getDamageMultiplier().toBuilder();
			damageMultBuilder.setAsphyxiation(1);
			entity = entity.toBuilder()
					.setDamage(entity.getDamage().toBuilder()
							.setDamageMultiplier(damageMultBuilder.build())
							.build())
					.build();
			entityManager.updateEntity(entity);
		}
		 if (!entity.getDamage().getDamageMultiplier().hasGenetic()) {
			DamageMultiplier.Builder damageMultBuilder = entity.getDamage().getDamageMultiplier().toBuilder();
			damageMultBuilder.setGenetic(1);
			entity = entity.toBuilder()
					.setDamage(entity.getDamage().toBuilder()
							.setDamageMultiplier(damageMultBuilder.build())
							.build())
					.build();
			entityManager.updateEntity(entity);
		}
		 if (!entity.getDamage().getDamageMultiplier().hasStructural()) {
			DamageMultiplier.Builder damageMultBuilder = entity.getDamage().getDamageMultiplier().toBuilder();
			damageMultBuilder.setStructural(1);
			entity = entity.toBuilder()
					.setDamage(entity.getDamage().toBuilder()
							.setDamageMultiplier(damageMultBuilder.build())
							.build())
					.build();
			entityManager.updateEntity(entity);
		}
		 if (!entity.getDamage().getDamageMultiplier().hasBleeding()) {
				DamageMultiplier.Builder damageMultBuilder = entity.getDamage().getDamageMultiplier().toBuilder();
				damageMultBuilder.setBleeding(1);
				entity = entity.toBuilder()
						.setDamage(entity.getDamage().toBuilder()
								.setDamageMultiplier(damageMultBuilder.build())
								.build())
						.build();
				entityManager.updateEntity(entity);
		 }
	}	
}
