package health;

import java.util.Map;
import java.util.TreeMap;

import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.OrganStatus;
import protonova.protobuf.OrgansProto.OrganType;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

final class PhysiologyState {

	final Organs organs;
	final float breathableOxygen;
	final Map<String, Float> bloodChemicals = new TreeMap<>();
	final Map<String, Float> stomachChemicals = new TreeMap<>();
	float maxOxygen;
	float oxygenCapacity;
	float oxygen;
	float maxPower;
	float power;
	float maxNutrition;
	float nutrition;
	float fluidCapacity;
	float chemicalSpace;
	float stomachCapacity;
	float cyberneticPower = 1;
	float biologicalNutrition = 1;
	float oxygenFromLungs;
	float totalOxygenUse;
	float brainOxygenUse;
	float circulation;
	float toxinRecovery;
	float brainOxygenShortage;

	PhysiologyState(Entity entity, float breathableOxygen) {
		this.organs = entity.getOrgans();
		this.breathableOxygen = breathableOxygen;

		CardiovascularSystem cardiovascularSystem;
		if (organs.hasCardiovascularSystem()) {
			cardiovascularSystem = organs.getCardiovascularSystem();
		} else {
			cardiovascularSystem = CardiovascularSystem.getDefaultInstance();
		}

		float bloodOxygenCapacity = 0;
		if (organs.hasHeart()) {
			bloodOxygenCapacity = positive(organs.getHeart().getMaxBlood());
		}

		float fullBloodOxygenCapacity = bloodOxygenCapacity;
		if (cardiovascularSystem.hasMaxOxygen()) {
			fullBloodOxygenCapacity = positive(cardiovascularSystem.getMaxOxygen());
		}

		maxOxygen = fullBloodOxygenCapacity;

		float currentBloodRatio = 0;
		if (bloodOxygenCapacity > 0) {
			currentBloodRatio = organs.getHeart().getBlood() / bloodOxygenCapacity;
			currentBloodRatio = clamp(currentBloodRatio, 0, 1);
		}

		oxygenCapacity = fullBloodOxygenCapacity * currentBloodRatio;
		oxygen = clamp(
				cardiovascularSystem.getOxygen(),
				0,
				oxygenCapacity);

		if (cardiovascularSystem.hasMaxElectricalPower()) {
			maxPower = positive(
					cardiovascularSystem.getMaxElectricalPower());
		} else {
			maxPower = positive(
					cardiovascularSystem.getElectricalPower());
		}

		power = clamp(
				cardiovascularSystem.getElectricalPower(),
				0,
				maxPower);

		maxNutrition = OrganEnergy.maximumNutrition(cardiovascularSystem);

		nutrition = clamp(
				cardiovascularSystem.getNutrition(),
				0,
				maxNutrition);

		float maximumBlood = 0;
		float currentBlood = 0;
		if (organs.hasHeart()) {
			maximumBlood = positive(organs.getHeart().getMaxBlood());
			currentBlood = clamp(organs.getHeart().getBlood(), 0, maximumBlood);
		}

		float requestedFluidCapacity;
		if (cardiovascularSystem.hasFluidCapacity()) {
			requestedFluidCapacity = positive(
					cardiovascularSystem.getFluidCapacity());
		} else {
			requestedFluidCapacity = maximumBlood + ChemicalUnits.DEFAULT_INJECTION_RESERVE;
		}

		fluidCapacity = Math.max(maximumBlood, requestedFluidCapacity);
		chemicalSpace = Math.max(0, fluidCapacity - currentBlood);

		for (Chemical chemical : cardiovascularSystem.getChemicalsList()) {
			addChemical(
					bloodChemicals,
					chemical.getName(),
					chemical.getAmount());
		}

		limitChemicals(bloodChemicals, chemicalSpace);

		if (organs.hasStomach()) {
			Stomach stomach = organs.getStomach();

			if (stomach.hasChemicalCapacity()) {
				stomachCapacity = positive(stomach.getChemicalCapacity());
			} else {
				stomachCapacity = ChemicalUnits.DEFAULT_STOMACH_CAPACITY;
			}

			for (String chemicalName : stomach.getChemicalsList()) {
				addChemical(stomachChemicals, chemicalName, 1);
			}

			for (Chemical chemical : stomach.getContentsList()) {
				addChemical(
						stomachChemicals,
						chemical.getName(),
						chemical.getAmount());
			}

			limitChemicals(stomachChemicals, stomachCapacity);
		}
	}

	void useEnergy() {
		float totalPowerUse = 0;
		float totalNutritionUse = 0;

		if (organs.hasHeart()) {
			totalPowerUse += OrganEnergy.powerUsePerSecond(organs.getHeart().getStatus());
			totalNutritionUse += OrganEnergy.nutritionUsePerSecond(
					organs.getHeart().getStatus(), OrganEnergy.DEFAULT_HEART_NUTRITION_USE);
		}
		if (organs.hasLungs()) {
			totalPowerUse += OrganEnergy.powerUsePerSecond(organs.getLungs().getStatus());
			totalNutritionUse += OrganEnergy.nutritionUsePerSecond(
					organs.getLungs().getStatus(), OrganEnergy.DEFAULT_LUNG_NUTRITION_USE);
		}
		if (organs.hasLiver()) {
			totalPowerUse += OrganEnergy.powerUsePerSecond(organs.getLiver().getStatus());
			totalNutritionUse += OrganEnergy.nutritionUsePerSecond(
					organs.getLiver().getStatus(), OrganEnergy.DEFAULT_LIVER_NUTRITION_USE);
		}
		if (organs.hasBrain()) {
			totalPowerUse += OrganEnergy.powerUsePerSecond(organs.getBrain().getStatus());
			totalNutritionUse += OrganEnergy.nutritionUsePerSecond(
					organs.getBrain().getStatus(), OrganEnergy.DEFAULT_BRAIN_NUTRITION_USE);
		}
		if (organs.hasStomach()) {
			totalPowerUse += OrganEnergy.powerUsePerSecond(organs.getStomach().getStatus());
			totalNutritionUse += OrganEnergy.nutritionUsePerSecond(
					organs.getStomach().getStatus(), OrganEnergy.DEFAULT_STOMACH_NUTRITION_USE);
		}

		if (totalPowerUse > 0) {
			float deliveredPower = Math.min(power, totalPowerUse);
			cyberneticPower = deliveredPower / totalPowerUse;
			power -= deliveredPower;
		}

		if (totalNutritionUse > 0) {
			float deliveredNutrition = Math.min(nutrition, totalNutritionUse);
			biologicalNutrition = deliveredNutrition / totalNutritionUse;
			nutrition -= deliveredNutrition;
		}
	}

	float getOrganFunction(OrganStatus organStatus) {
		float organHealth = organCondition(
				organStatus.hasHealth(),
				organStatus.getHealth());
		float organEfficiency = organCondition(
				organStatus.hasEfficiency(),
				organStatus.getEfficiency());
		float availablePower = 1;

		if (organStatus.getType() == OrganType.ORGAN_TYPE_CYBERNETIC) {
			availablePower = cyberneticPower;
		} else {
			availablePower = biologicalNutrition;
		}

		return organHealth * organEfficiency * availablePower;
	}

	void addOxygen(float oxygenAmount) {
		oxygenFromLungs += positive(oxygenAmount);
	}

	void addOxygenUse(float oxygenAmount) {
		totalOxygenUse += positive(oxygenAmount);
	}

	void addBrainOxygen(float oxygenAmount) {
		float safeOxygenAmount = positive(oxygenAmount);
		brainOxygenUse += safeOxygenAmount;
		totalOxygenUse += safeOxygenAmount;
	}

	void addCirculation(float circulationAmount) {
		circulation += positive(circulationAmount);
	}

	void addToxinRecovery(float recoveryAmount) {
		toxinRecovery += positive(recoveryAmount);
	}

	void digest(float absorptionAmount) {
		float totalStomachChemicalAmount = totalChemicals(stomachChemicals);
		float currentBloodstreamChemicalAmount = totalChemicals(bloodChemicals);
		float availableChemicalSpace = chemicalSpace
				- currentBloodstreamChemicalAmount;
		availableChemicalSpace = Math.max(0, availableChemicalSpace);

		if (totalStomachChemicalAmount <= 0
				|| absorptionAmount <= 0
				|| availableChemicalSpace <= 0) {
			return;
		}

		float absorbedAmount = Math.min(absorptionAmount, availableChemicalSpace);
		float absorbedRatio = absorbedAmount / totalStomachChemicalAmount;
		absorbedRatio = Math.min(1, absorbedRatio);

		for (Map.Entry<String, Float> stomachChemical : stomachChemicals.entrySet()) {
			float amountMovedToBloodstream = stomachChemical.getValue() * absorbedRatio;
			float amountLeftInStomach = stomachChemical.getValue() - amountMovedToBloodstream;
			stomachChemical.setValue(amountLeftInStomach);
			addChemical(
					bloodChemicals,
					stomachChemical.getKey(),
					amountMovedToBloodstream);
		}
	}

	void useOxygen() {
		oxygen += oxygenFromLungs;
		oxygen = Math.min(oxygenCapacity, oxygen);

		float oxygenAllowedByCirculation = Math.min(circulation, totalOxygenUse);
		float deliveredOxygen = Math.min(oxygen, oxygenAllowedByCirculation);
		oxygen -= deliveredOxygen;

		float brainShareOfOxygenUse = 0;
		if (totalOxygenUse > 0) {
			brainShareOfOxygenUse = brainOxygenUse / totalOxygenUse;
		}

		float oxygenDeliveredToBrain = deliveredOxygen * brainShareOfOxygenUse;
		brainOxygenShortage = Math.max(0, brainOxygenUse - oxygenDeliveredToBrain);
	}

	private static void addChemical(
			Map<String, Float> chemicals,
			String chemicalName,
			float chemicalAmount) {

		float safeChemicalAmount = positive(chemicalAmount);
		if (safeChemicalAmount <= 0) {
			return;
		}

		if (chemicalName == null || chemicalName.trim().isEmpty()) {
			return;
		}
		chemicalName = chemicalName.trim().toLowerCase(java.util.Locale.ROOT);

		if (chemicals.containsKey(chemicalName)) {
			float combinedAmount = chemicals.get(chemicalName) + safeChemicalAmount;
			chemicals.put(chemicalName, combinedAmount);
		} else {
			chemicals.put(chemicalName, safeChemicalAmount);
		}
	}

	private static float totalChemicals(Map<String, Float> chemicals) {
		float totalChemicalAmount = 0;
		for (float chemicalAmount : chemicals.values()) {
			totalChemicalAmount += positive(chemicalAmount);
		}
		return totalChemicalAmount;
	}

	private static void limitChemicals(
			Map<String, Float> chemicals,
			float chemicalCapacity) {

		float totalChemicalAmount = totalChemicals(chemicals);
		if (totalChemicalAmount <= chemicalCapacity || totalChemicalAmount <= 0) {
			return;
		}

		float safeChemicalCapacity = Math.max(0, chemicalCapacity);
		float retainedChemicalRatio = safeChemicalCapacity / totalChemicalAmount;

		for (Map.Entry<String, Float> chemical : chemicals.entrySet()) {
			float retainedChemicalAmount = chemical.getValue() * retainedChemicalRatio;
			chemical.setValue(retainedChemicalAmount);
		}
	}

	private static float organCondition(boolean conditionIsSet, float condition) {
		return conditionIsSet ? clamp(condition, 0, 1) : 1;
	}

	private static float positive(float value) {
		return Float.isFinite(value) ? Math.max(0, value) : 0;
	}

	private static float clamp(float value, float minimum, float maximum) {
		if (!Float.isFinite(value)) return minimum;
		return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
	}
}
