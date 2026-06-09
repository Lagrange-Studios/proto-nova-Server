package health;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import entity.EntityManager;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.DamageMultiplier;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.EntityProto.Entity.Builder;
import util.TimedTask;
import util.VectorMath;

public class CombatManager {
	
	private EntityManager entityManager;
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
	
	public CombatManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
	
	public boolean attemptToDamage(Entity attacker, Entity defender) {
		if (!entityManager.entityExist(attacker)) {
			return false;
		}
		if (attacker.getReach() >= VectorMath.distance(attacker.getPosition(), defender.getPosition()) && attacker.getHitDamage().getCanAttack()) {
			startEntityHitCooldown(attacker);
			damage(attacker, defender);
			return true;
		} else {
			return false;
			
		}
	}
	
	public void startEntityHitCooldown(Entity entity) {
		Entity updated = entity.toBuilder().setHitDamage(
				entity.getHitDamage().toBuilder()
				.setCanAttack(false)).build();
		entityManager.updateEntity(updated);
		startTimedCooldown(entity);
	}
	
	private void startTimedCooldown(Entity entity) {
		
		int cooldownTime =  entity.getHitDamage().getHitCooldown();
		
		if (entity.getInventorySlotsMap().containsKey(entity.getSelectedSlot())) {
			cooldownTime = entity.getHitDamage().getHitCooldown();
		}
		
		TimedTask task = new TimedTask(
	            () -> {
	            	resetCooldown(entity);
	            },
	            cooldownTime,
	            TimeUnit.MILLISECONDS,
	            scheduler
	    );
	}
	
	private void resetCooldown(Entity entity) {
		Entity updated = entity.toBuilder().setHitDamage(
				entity.getHitDamage().toBuilder()
				.setCanAttack(true)).build();
		entityManager.updateEntity(updated);
	}
	
	private void damage(Entity attacker, Entity defender) {
		defender = entityManager.getEntity(defender.getId());
		attacker = entityManager.getEntity(attacker.getId());
		checkDamageMults(defender);
		
		HitDamage hitDamage = attacker.getHitDamage();
		if (attacker.getInventorySlotsMap().containsKey(attacker.getSelectedSlot())) {
			hitDamage = attacker.getHitDamage();
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
			
			entityDamage.setBleedingPerTick(currentDefenderDamage.getBleedingPerTick() + damage);
		}
		
		Entity defenderFinal = defenderBuilder.setDamage(entityDamage.build()).build();
		entityManager.updateEntity(defenderFinal);
		
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
	
	public double getDamage(Entity entity) {
		float brute = entity.getDamage().getBruteDamage();
		float burn = entity.getDamage().getBurnDamage();
		float toxin = entity.getDamage().getToxinDamage();
		float asphyxiation = entity.getDamage().getAsphyxiationDamage();
		float genetic = entity.getDamage().getGeneticDamage();
		float structural = entity.getDamage().getStructuralDamage();
		double totalDamage = brute + burn + toxin + asphyxiation + genetic + structural;
		return totalDamage;
		
	}
	
}
