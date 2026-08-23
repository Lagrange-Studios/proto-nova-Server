package health;

import entity.EntityManager;
import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.Stomach;

public class ConsumptionManager {
	
	private ChemicalDigestionManager chemicalDigestionManager;
	private ChemicalManager chemicalManager;
	private EntityManager entityManager;
	
	public ConsumptionManager(ChemicalDigestionManager chemicalDigestionManager, ChemicalManager chemicalManager, EntityManager entityManager) {
		this.chemicalDigestionManager = chemicalDigestionManager;
		this.chemicalManager = chemicalManager;
		this.entityManager = entityManager;
	}
	
	public void consume(Entity foodItem, Entity consumingEntity) {
		if (!consumingEntity.getOrgans().hasStomach()) { return; }
		float unitsToConsumePerChemical = foodItem.getChemicalsCount()/5;
		for (int i = 0; i < foodItem.getChemicalsCount(); i++) {
			Chemical chemical = foodItem.getChemicals(i);
			chemicalManager.addToStomach(consumingEntity, chemical.getName(), unitsToConsumePerChemical);
			Chemical newChemical = chemical.toBuilder().setAmount(chemical.getAmount()-unitsToConsumePerChemical).build();
			foodItem = foodItem.toBuilder().setChemicals(i, newChemical).build();
		}
		entityManager.updateEntity(consumingEntity);
		entityManager.updateEntity(foodItem);
	}
	
}
