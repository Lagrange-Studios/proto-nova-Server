package health;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

public final class ChemicalManager {

	public Entity addToStomach(Entity entity, int chemicalId, float requestedUnits) {
		Objects.requireNonNull(entity, "entity");
		if (!entity.hasOrgans() || !entity.getOrgans().hasStomach()) return entity;

		Stomach stomach = entity.getOrgans().getStomach();
		float capacity = stomach.hasChemicalCapacity()
				? positive(stomach.getChemicalCapacity())
				: ChemicalUnits.DEFAULT_STOMACH_CAPACITY;
		Map<Integer, Float> contents = stomachContents(stomach);
		float accepted = Math.min(positive(requestedUnits), remaining(capacity, contents));
		if (accepted <= 0) return entity;

		contents.merge(chemicalId, accepted, Float::sum);
		Stomach.Builder updatedStomach = stomach.toBuilder()
				.clearChemicals()
				.clearContents()
				.setChemicalCapacity(capacity);
		addChemicals(updatedStomach, contents);

		return entity.toBuilder()
				.setOrgans(entity.getOrgans().toBuilder().setStomach(updatedStomach))
				.build();
	}

	public Entity injectIntoCirculation(Entity entity, int chemicalId, float requestedUnits) {
		Objects.requireNonNull(entity, "entity");
		if (!entity.hasOrgans()) return entity;

		Organs organs = entity.getOrgans();
		CardiovascularSystem cardiovascular = organs.hasCardiovascularSystem()
				? organs.getCardiovascularSystem()
				: CardiovascularSystem.getDefaultInstance();
		float maximumBlood = organs.hasHeart() ? positive(organs.getHeart().getMaxBlood()) : 0;
		float currentBlood = organs.hasHeart()
				? clamp(organs.getHeart().getBlood(), 0, maximumBlood)
				: 0;
		float capacity = cardiovascular.hasFluidCapacity()
				? Math.max(maximumBlood, positive(cardiovascular.getFluidCapacity()))
				: maximumBlood + ChemicalUnits.DEFAULT_INJECTION_RESERVE;
		Map<Integer, Float> chemicals = chemicalContents(cardiovascular);
		float availableChemicalSpace = Math.max(0, capacity - currentBlood - total(chemicals));
		float accepted = Math.min(positive(requestedUnits), availableChemicalSpace);
		if (accepted <= 0) return entity;

		chemicals.merge(chemicalId, accepted, Float::sum);
		CardiovascularSystem.Builder updatedCardiovascular = cardiovascular.toBuilder()
				.clearChemicals()
				.setFluidCapacity(capacity);
		for (Map.Entry<Integer, Float> entry : chemicals.entrySet()) {
			updatedCardiovascular.addChemicals(chemical(entry.getKey(), entry.getValue()));
		}

		return entity.toBuilder()
				.setOrgans(organs.toBuilder().setCardiovascularSystem(updatedCardiovascular))
				.build();
	}

	public float getRemainingStomachCapacity(Entity entity) {
		if (entity == null || !entity.hasOrgans() || !entity.getOrgans().hasStomach()) return 0;
		Stomach stomach = entity.getOrgans().getStomach();
		float capacity = stomach.hasChemicalCapacity()
				? positive(stomach.getChemicalCapacity())
				: ChemicalUnits.DEFAULT_STOMACH_CAPACITY;
		return remaining(capacity, stomachContents(stomach));
	}

	public float getRemainingCirculationCapacity(Entity entity) {
		if (entity == null || !entity.hasOrgans()) return 0;
		Organs organs = entity.getOrgans();
		CardiovascularSystem cardiovascular = organs.hasCardiovascularSystem()
				? organs.getCardiovascularSystem()
				: CardiovascularSystem.getDefaultInstance();
		float maximumBlood = organs.hasHeart() ? positive(organs.getHeart().getMaxBlood()) : 0;
		float currentBlood = organs.hasHeart()
				? clamp(organs.getHeart().getBlood(), 0, maximumBlood)
				: 0;
		float capacity = cardiovascular.hasFluidCapacity()
				? Math.max(maximumBlood, positive(cardiovascular.getFluidCapacity()))
				: maximumBlood + ChemicalUnits.DEFAULT_INJECTION_RESERVE;
		return Math.max(0, capacity - currentBlood - total(chemicalContents(cardiovascular)));
	}

	private static Map<Integer, Float> stomachContents(Stomach stomach) {
		Map<Integer, Float> contents = new TreeMap<>();
		for (int chemicalId : stomach.getChemicalsList()) contents.merge(chemicalId, 1.0f, Float::sum);
		for (Chemical chemical : stomach.getContentsList()) {
			contents.merge(chemical.getName(), positive(chemical.getAmount()), Float::sum);
		}
		return contents;
	}

	private static Map<Integer, Float> chemicalContents(CardiovascularSystem cardiovascular) {
		Map<Integer, Float> contents = new TreeMap<>();
		for (Chemical chemical : cardiovascular.getChemicalsList()) {
			contents.merge(chemical.getName(), positive(chemical.getAmount()), Float::sum);
		}
		return contents;
	}

	private static void addChemicals(Stomach.Builder stomach, Map<Integer, Float> contents) {
		for (Map.Entry<Integer, Float> entry : contents.entrySet()) {
			if (entry.getValue() > 0) stomach.addContents(chemical(entry.getKey(), entry.getValue()));
		}
	}

	private static Chemical chemical(int id, float amount) {
		return Chemical.newBuilder().setName(id).setAmount(amount).build();
	}

	private static float remaining(float capacity, Map<Integer, Float> contents) {
		return Math.max(0, capacity - total(contents));
	}

	private static float total(Map<Integer, Float> contents) {
		float total = 0;
		for (float amount : contents.values()) total += positive(amount);
		return total;
	}

	private static float positive(float value) {
		return Float.isFinite(value) ? Math.max(0, value) : 0;
	}

	private static float clamp(float value, float minimum, float maximum) {
		if (!Float.isFinite(value)) return minimum;
		return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
	}
}
