package health;

import entity.EntityManager;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.EntityProto.Entity.Builder;
import util.VectorMath;

public class CombatManager {
	
	private EntityManager entityManager;
	
	public CombatManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
	
	public boolean atemptToDamage(Entity attacker, Entity defender) {
		if (attacker.getReach() >= VectorMath.distance(attacker.getPosition(), defender.getPosition())) {
			damage(attacker, defender);
			return true;
		} else {
			return false;
			
		}
	}
	
	private void damage(Entity attacker, Entity defender) {
		HitDamage hitDamage = attacker.getHitDamage();
		Entity.Builder defenderBuilder = defender.toBuilder();
		if (attacker.getInventorySlotsMap().containsKey(attacker.getSelectedSlot())) {
			hitDamage = attacker.getHitDamage();
		}
		
		Damage.Builder entityDamage = defender.getDamage().toBuilder();
		
		if (hitDamage.hasBruteDamage()) {
			entityDamage.setBruteDamage(defender.getDamage().getBruteDamage() + hitDamage.getBruteDamage());
		}
		if (hitDamage.hasBurnDamage()) {
			entityDamage.setBurnDamage(defender.getDamage().getBurnDamage() + hitDamage.getBurnDamage());
		}
		if (hitDamage.hasToxinDamage()) {
			entityDamage.setToxinDamage(defender.getDamage().getToxinDamage() + hitDamage.getToxinDamage());
		}
		if (hitDamage.hasAsphyxiationDamage()) {
			entityDamage.setAsphyxiationDamage(defender.getDamage().getAsphyxiationDamage() + hitDamage.getAsphyxiationDamage());
		}
		if (hitDamage.hasGeneticDamage()) {
			entityDamage.setGeneticDamage(defender.getDamage().getGeneticDamage() + hitDamage.getGeneticDamage());
		}
		if (hitDamage.hasStructuralDamage()) {
			entityDamage.setStructuralDamage(defender.getDamage().getStructuralDamage() + hitDamage.getStructuralDamage());
		}
		if (hitDamage.hasBleedingPerTick()) {
			entityDamage.setBleedingPerTick(defender.getDamage().getBleedingPerTick() + hitDamage.getBleedingPerTick());
		}
		
		Entity defenderFinal = defenderBuilder.setDamage(entityDamage.build()).build();
		
		entityManager.updateEntity(defenderFinal);
		
	}
}
