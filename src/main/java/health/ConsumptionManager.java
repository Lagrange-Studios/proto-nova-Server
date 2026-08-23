package health;

import entity.EntityManager;
import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.EntityProto.Entity;

public class ConsumptionManager {

	private final ChemicalManager chemicalManager;
	private final EntityManager entityManager;

	public ConsumptionManager(ChemicalManager chemicalManager, EntityManager entityManager) {
		this.chemicalManager = chemicalManager;
		this.entityManager = entityManager;
	}
	
	public void consume(Entity foodItem, Entity consumingEntity) {
		if (foodItem == null || consumingEntity == null) return;
		if (chemicalManager.getRemainingStomachCapacity(consumingEntity) <= 0) return;

		for (int i = 0; i < foodItem.getChemicalsCount(); i++) {
			Chemical chemical = foodItem.getChemicals(i);
			float requestedUnits = Math.max(0, chemical.getAmount()) / 5.0f;
			float acceptedUnits = Math.min(
					requestedUnits,
					chemicalManager.getRemainingStomachCapacity(consumingEntity));
			if (acceptedUnits <= 0) continue;

			consumingEntity = chemicalManager.addToStomach(
					consumingEntity,
					chemical.getName(),
					acceptedUnits);
			Chemical newChemical = chemical.toBuilder()
					.setAmount(Math.max(0, chemical.getAmount() - acceptedUnits))
					.build();
			foodItem = foodItem.toBuilder().setChemicals(i, newChemical).build();
		}
		entityManager.updateEntity(consumingEntity);
		entityManager.updateEntity(foodItem);
	}
}
