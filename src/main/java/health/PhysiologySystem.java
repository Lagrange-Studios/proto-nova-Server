package health;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import entity.EntityManager;
import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.Brain;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.Heart;
import protonova.protobuf.OrgansProto.Liver;
import protonova.protobuf.OrgansProto.Lungs;
import protonova.protobuf.OrgansProto.OrganSlots;
import protonova.protobuf.OrgansProto.OrganStatus;
import protonova.protobuf.OrgansProto.OrganType;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

public final class PhysiologySystem {

	// default useage so we can do 200 percent effeciency insted of having to say the number without any context.
	private static final float DEFAULT_HEART_OXYGEN_USE = 1.0f;
	private static final float DEFAULT_LUNG_OXYGEN_USE = 0.5f;
	private static final float DEFAULT_LIVER_OXYGEN_USE = 1.0f;
	private static final float DEFAULT_BRAIN_OXYGEN_USE = 3.0f;
	private static final float DEFAULT_STOMACH_OXYGEN_USE = 0.5f;
	private static final float DEFAULT_STOMACH_ABSORPTION = 1.0f;
	private static final float ASPHYXIATION_DAMAGE_PER_SECOND = 2.0f;
	private static final float ASPHYXIATION_RECOVERY_PER_SECOND = 0.5f;

	public Entity update(Entity entity) {
		return update(entity, 1.0f);
	}

	public Entity update(Entity body, EntityManager entityManager) {
		return update(body, entityManager, 1.0f);
	}

	public Entity update(Entity body, EntityManager entityManager, float breathableOxygen) {
		Objects.requireNonNull(body, "body");
		Objects.requireNonNull(entityManager, "entityManager");

		if (!body.hasOrganSlots()) {
			return update(body, breathableOxygen);
		}

		Organs.Builder installedOrgans = body.getOrgans().toBuilder();
		OrganSlots organSlots = body.getOrganSlots();

		loadHeartFromEntity(installedOrgans, entityManager, getHeartEntityId(organSlots));
		loadLungsFromEntity(installedOrgans, entityManager, getLungsEntityId(organSlots));
		loadLiverFromEntity(installedOrgans, entityManager, getLiverEntityId(organSlots));
		loadBrainFromEntity(installedOrgans, entityManager, getBrainEntityId(organSlots));
		loadStomachFromEntity(installedOrgans, entityManager, getStomachEntityId(organSlots));

		Entity bodyWithInstalledOrgans = body.toBuilder()
				.setOrgans(installedOrgans)
				.build();
		Entity updatedBody = update(bodyWithInstalledOrgans, breathableOxygen);

		updateStomachEntity(updatedBody, organSlots, entityManager);

		Organs.Builder bodyPhysiologyState = body.getOrgans().toBuilder();
		bodyPhysiologyState.clearHeart();
		bodyPhysiologyState.clearLungs();
		bodyPhysiologyState.clearLiver();
		bodyPhysiologyState.clearBrain();
		bodyPhysiologyState.clearStomach();

		if (updatedBody.getOrgans().hasCardiovascularSystem()) {
			bodyPhysiologyState.setCardiovascularSystem(
					updatedBody.getOrgans().getCardiovascularSystem());
		}

		return updatedBody.toBuilder()
				.setOrgans(bodyPhysiologyState)
				.build();
	}

	public Entity update(Entity entity, float breathableOxygen) {
		Objects.requireNonNull(entity, "entity");

		if (!entity.hasOrgans()) {
			return entity;
		}

		float safeBreathableOxygen = keepValueInRange(breathableOxygen, 0, 1);
		PhysiologyUpdate physiologyUpdate = new PhysiologyUpdate(entity, safeBreathableOxygen);
		physiologyUpdate.resolveOrganEnergyUse();

		collectLungChanges(physiologyUpdate);
		collectHeartChanges(physiologyUpdate);
		collectLiverChanges(physiologyUpdate);
		collectStomachChanges(physiologyUpdate);
		collectBrainChanges(physiologyUpdate);

		physiologyUpdate.resolveOxygenChanges();
		physiologyUpdate.resolveChemicalChanges();

		return applyPhysiologyChanges(entity, physiologyUpdate);
	}

	private void collectLungChanges(PhysiologyUpdate physiologyUpdate) {
		if (!physiologyUpdate.organs.hasLungs()) {
			return;
		}

		Lungs lungs = physiologyUpdate.organs.getLungs();
		float organFunction = physiologyUpdate.getOrganFunction(lungs.getStatus());
		float oxygenTransfer = getNonNegativeValue(lungs.getOxygen());
		oxygenTransfer = oxygenTransfer * organFunction * physiologyUpdate.breathableOxygen;

		float oxygenUse = getSetValueOrDefault(
				lungs.hasOxygenUsePerSecond(),
				lungs.getOxygenUsePerSecond(),
				DEFAULT_LUNG_OXYGEN_USE);

		physiologyUpdate.addOxygenSupply(oxygenTransfer);
		physiologyUpdate.addOxygenUse(oxygenUse * organFunction);
	}

	private void collectHeartChanges(PhysiologyUpdate physiologyUpdate) {
		if (!physiologyUpdate.organs.hasHeart()) {
			return;
		}

		Heart heart = physiologyUpdate.organs.getHeart();
		float organFunction = physiologyUpdate.getOrganFunction(heart.getStatus());
		float maximumBlood = getNonNegativeValue(heart.getMaxBlood());
		float currentBloodRatio = 0;

		if (maximumBlood > 0) {
			currentBloodRatio = keepValueInRange(heart.getBlood() / maximumBlood, 0, 1);
		}

		float circulationPerSecond = getSetValueOrDefault(
				heart.hasCirculationPerSecond(),
				heart.getCirculationPerSecond(),
				maximumBlood);
		float oxygenUse = getSetValueOrDefault(
				heart.hasOxygenUsePerSecond(),
				heart.getOxygenUsePerSecond(),
				DEFAULT_HEART_OXYGEN_USE);

		float availableCirculation = getNonNegativeValue(circulationPerSecond);
		availableCirculation = availableCirculation * currentBloodRatio * organFunction;

		physiologyUpdate.addAvailableCirculation(availableCirculation);
		physiologyUpdate.addOxygenUse(oxygenUse * organFunction);
	}

	private void collectLiverChanges(PhysiologyUpdate physiologyUpdate) {
		if (!physiologyUpdate.organs.hasLiver()) {
			return;
		}

		Liver liver = physiologyUpdate.organs.getLiver();
		float organFunction = physiologyUpdate.getOrganFunction(liver.getStatus());
		float oxygenUse = getSetValueOrDefault(
				liver.hasOxygenUsePerSecond(),
				liver.getOxygenUsePerSecond(),
				DEFAULT_LIVER_OXYGEN_USE);
		float detoxification = getNonNegativeValue(liver.getDetoxification()) * organFunction;

		physiologyUpdate.addDetoxification(detoxification);
		physiologyUpdate.addOxygenUse(oxygenUse * organFunction);
	}

	private void collectStomachChanges(PhysiologyUpdate physiologyUpdate) {
		if (!physiologyUpdate.organs.hasStomach()) {
			return;
		}

		Stomach stomach = physiologyUpdate.organs.getStomach();
		float organFunction = physiologyUpdate.getOrganFunction(stomach.getStatus());
		float absorptionPerSecond = getSetValueOrDefault(
				stomach.hasAbsorptionPerSecond(),
				stomach.getAbsorptionPerSecond(),
				DEFAULT_STOMACH_ABSORPTION);
		float oxygenUse = getSetValueOrDefault(
				stomach.hasOxygenUsePerSecond(),
				stomach.getOxygenUsePerSecond(),
				DEFAULT_STOMACH_OXYGEN_USE);

		physiologyUpdate.absorbStomachChemicals(
				getNonNegativeValue(absorptionPerSecond * organFunction));
		physiologyUpdate.addOxygenUse(oxygenUse * organFunction);
	}

	private void collectBrainChanges(PhysiologyUpdate physiologyUpdate) {
		if (!physiologyUpdate.organs.hasBrain()) {
			return;
		}

		Brain brain = physiologyUpdate.organs.getBrain();
		float organFunction = physiologyUpdate.getOrganFunction(brain.getStatus());
		float oxygenUse = getSetValueOrDefault(
				brain.hasOxygenUsePerSecond(),
				brain.getOxygenUsePerSecond(),
				DEFAULT_BRAIN_OXYGEN_USE);

		physiologyUpdate.addBrainOxygenUse(oxygenUse * organFunction);
	}

	private Entity applyPhysiologyChanges(Entity entity, PhysiologyUpdate physiologyUpdate) {
		Organs.Builder updatedOrgans = entity.getOrgans().toBuilder();
		CardiovascularSystem.Builder updatedCardiovascularSystem;

		if (updatedOrgans.hasCardiovascularSystem()) {
			updatedCardiovascularSystem = updatedOrgans.getCardiovascularSystem().toBuilder();
		} else {
			updatedCardiovascularSystem = CardiovascularSystem.newBuilder();
		}

		updatedCardiovascularSystem.setOxygen(physiologyUpdate.storedOxygen);
		updatedCardiovascularSystem.setMaxOxygen(physiologyUpdate.configuredMaximumOxygen);
		updatedCardiovascularSystem.setElectricalPower(physiologyUpdate.storedElectricalPower);
		updatedCardiovascularSystem.setMaxElectricalPower(physiologyUpdate.maximumElectricalPower);
		updatedCardiovascularSystem.setNutrition(physiologyUpdate.storedNutrition);
		updatedCardiovascularSystem.setMaxNutrition(physiologyUpdate.maximumNutrition);
		updatedCardiovascularSystem.setFluidCapacity(physiologyUpdate.configuredFluidCapacity);
		updatedCardiovascularSystem.clearChemicals();

		for (Map.Entry<String, Float> chemical : physiologyUpdate.bloodstreamChemicals.entrySet()) {
			if (chemical.getValue() > 0) {
				updatedCardiovascularSystem.addChemicals(Chemical.newBuilder()
						.setName(chemical.getKey())
						.setAmount(chemical.getValue()));
			}
		}

		updatedOrgans.setCardiovascularSystem(updatedCardiovascularSystem);

		if (updatedOrgans.hasHeart()) {
			Heart currentHeart = updatedOrgans.getHeart();
			float maximumBlood = getNonNegativeValue(currentHeart.getMaxBlood());
			float currentBlood = keepValueInRange(currentHeart.getBlood(), 0, maximumBlood);
			updatedOrgans.setHeart(currentHeart.toBuilder().setBlood(currentBlood));
		}

		if (updatedOrgans.hasStomach()) {
			Stomach.Builder updatedStomach = updatedOrgans.getStomach().toBuilder();
			updatedStomach.clearChemicals();
			updatedStomach.clearContents();
			updatedStomach.setChemicalCapacity(physiologyUpdate.stomachChemicalCapacity);

			for (Map.Entry<String, Float> chemical : physiologyUpdate.stomachChemicals.entrySet()) {
				if (chemical.getValue() > 0) {
					updatedStomach.addContents(Chemical.newBuilder()
							.setName(chemical.getKey())
							.setAmount(chemical.getValue()));
				}
			}

			updatedOrgans.setStomach(updatedStomach);
		}

		Damage.Builder updatedDamage = entity.getDamage().toBuilder();
		float asphyxiationDamage = getNonNegativeValue(updatedDamage.getAsphyxiationDamage());

		if (physiologyUpdate.brainOxygenUse > 0 && physiologyUpdate.missingBrainOxygen > 0) {
			float oxygenShortageRatio = physiologyUpdate.missingBrainOxygen
					/ physiologyUpdate.brainOxygenUse;
			oxygenShortageRatio = keepValueInRange(oxygenShortageRatio, 0, 1);
			asphyxiationDamage += ASPHYXIATION_DAMAGE_PER_SECOND * oxygenShortageRatio;
		} else {
			asphyxiationDamage -= ASPHYXIATION_RECOVERY_PER_SECOND;
			asphyxiationDamage = Math.max(0, asphyxiationDamage);
		}

		updatedDamage.setAsphyxiationDamage(asphyxiationDamage);

		return entity.toBuilder()
				.setOrgans(updatedOrgans)
				.setDamage(updatedDamage)
				.build();
	}

	private static void updateStomachEntity(
			Entity updatedBody,
			OrganSlots organSlots,
			EntityManager entityManager) {

		if (!organSlots.hasStomachEntityId()) {
			return;
		}

		if (!updatedBody.getOrgans().hasStomach()) {
			return;
		}

		Entity stomachEntity = entityManager.getEntity(organSlots.getStomachEntityId());

		if (stomachEntity == null) {
			return;
		}

		if (!stomachEntity.hasOrganComponent()) {
			return;
		}

		Entity updatedStomachEntity = stomachEntity.toBuilder()
				.setOrganComponent(stomachEntity.getOrganComponent().toBuilder()
						.setStomach(updatedBody.getOrgans().getStomach()))
				.build();
		entityManager.updateEntity(updatedStomachEntity);
	}

	private static int getHeartEntityId(OrganSlots organSlots) {
		if (organSlots.hasHeartEntityId()) {
			return organSlots.getHeartEntityId();
		}
		return -1;
	}

	private static int getLungsEntityId(OrganSlots organSlots) {
		if (organSlots.hasLungsEntityId()) {
			return organSlots.getLungsEntityId();
		}
		return -1;
	}

	private static int getLiverEntityId(OrganSlots organSlots) {
		if (organSlots.hasLiverEntityId()) {
			return organSlots.getLiverEntityId();
		}
		return -1;
	}

	private static int getBrainEntityId(OrganSlots organSlots) {
		if (organSlots.hasBrainEntityId()) {
			return organSlots.getBrainEntityId();
		}
		return -1;
	}

	private static int getStomachEntityId(OrganSlots organSlots) {
		if (organSlots.hasStomachEntityId()) {
			return organSlots.getStomachEntityId();
		}
		return -1;
	}

	private static float getSetValueOrDefault(boolean valueIsSet, float value, float defaultValue) {
		if (valueIsSet) {
			return getNonNegativeValue(value);
		}
		return defaultValue;
	}

	private static void loadHeartFromEntity(
			Organs.Builder installedOrgans,
			EntityManager entityManager,
			int organEntityId) {

		Entity organEntity = entityManager.getEntity(organEntityId);
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasHeart()) {
			installedOrgans.setHeart(organEntity.getOrganComponent().getHeart());
		}
	}

	private static void loadLungsFromEntity(
			Organs.Builder installedOrgans,
			EntityManager entityManager,
			int organEntityId) {

		Entity organEntity = entityManager.getEntity(organEntityId);
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasLungs()) {
			installedOrgans.setLungs(organEntity.getOrganComponent().getLungs());
		}
	}

	private static void loadLiverFromEntity(
			Organs.Builder installedOrgans,
			EntityManager entityManager,
			int organEntityId) {

		Entity organEntity = entityManager.getEntity(organEntityId);
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasLiver()) {
			installedOrgans.setLiver(organEntity.getOrganComponent().getLiver());
		}
	}

	private static void loadBrainFromEntity(
			Organs.Builder installedOrgans,
			EntityManager entityManager,
			int organEntityId) {

		Entity organEntity = entityManager.getEntity(organEntityId);
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasBrain()) {
			installedOrgans.setBrain(organEntity.getOrganComponent().getBrain());
		}
	}

	private static void loadStomachFromEntity(
			Organs.Builder installedOrgans,
			EntityManager entityManager,
			int organEntityId) {

		Entity organEntity = entityManager.getEntity(organEntityId);
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasStomach()) {
			installedOrgans.setStomach(organEntity.getOrganComponent().getStomach());
		}
	}

	private static float getOrganCondition(boolean conditionIsSet, float condition) {
		if (conditionIsSet) {
			return keepValueInRange(condition, 0, 1);
		}
		return 1;
	}

	private static float getNonNegativeValue(float value) {
		if (!Float.isFinite(value)) {
			return 0;
		}
		return Math.max(0, value);
	}

	private static float keepValueInRange(float value, float minimum, float maximum) {
		if (!Float.isFinite(value)) {
			return minimum;
		}

		float safeMaximum = Math.max(minimum, maximum);
		float valueAboveMinimum = Math.max(minimum, value);
		return Math.min(valueAboveMinimum, safeMaximum);
	}

	private static final class PhysiologyUpdate {

		private final Organs organs;
		private final float breathableOxygen;
		private final Map<String, Float> bloodstreamChemicals = new TreeMap<>();
		private final Map<String, Float> stomachChemicals = new TreeMap<>();
		private float configuredMaximumOxygen;
		private float maximumOxygenForCurrentBlood;
		private float storedOxygen;
		private float maximumElectricalPower;
		private float storedElectricalPower;
		private float maximumNutrition;
		private float storedNutrition;
		private float configuredFluidCapacity;
		private float availableBloodstreamChemicalSpace;
		private float stomachChemicalCapacity;
		private float availableCyberneticPowerRatio = 1;
		private float availableBiologicalNutritionRatio = 1;
		private float oxygenAddedByLungs;
		private float totalOxygenUse;
		private float brainOxygenUse;
		private float availableCirculation;
		private float availableDetoxification;
		private float missingBrainOxygen;

		private PhysiologyUpdate(Entity entity, float breathableOxygen) {
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
				bloodOxygenCapacity = getNonNegativeValue(organs.getHeart().getMaxBlood());
			}

			float fullBloodOxygenCapacity = bloodOxygenCapacity;
			if (cardiovascularSystem.hasMaxOxygen()) {
				fullBloodOxygenCapacity = getNonNegativeValue(cardiovascularSystem.getMaxOxygen());
			}

			configuredMaximumOxygen = fullBloodOxygenCapacity;

			float currentBloodRatio = 0;
			if (bloodOxygenCapacity > 0) {
				currentBloodRatio = organs.getHeart().getBlood() / bloodOxygenCapacity;
				currentBloodRatio = keepValueInRange(currentBloodRatio, 0, 1);
			}

			maximumOxygenForCurrentBlood = fullBloodOxygenCapacity * currentBloodRatio;
			storedOxygen = keepValueInRange(
					cardiovascularSystem.getOxygen(),
					0,
					maximumOxygenForCurrentBlood);

			if (cardiovascularSystem.hasMaxElectricalPower()) {
				maximumElectricalPower = getNonNegativeValue(
						cardiovascularSystem.getMaxElectricalPower());
			} else {
				maximumElectricalPower = getNonNegativeValue(
						cardiovascularSystem.getElectricalPower());
			}

			storedElectricalPower = keepValueInRange(
					cardiovascularSystem.getElectricalPower(),
					0,
					maximumElectricalPower);

			maximumNutrition = OrganEnergy.maximumNutrition(cardiovascularSystem);

			storedNutrition = keepValueInRange(
					cardiovascularSystem.getNutrition(),
					0,
					maximumNutrition);

			float maximumBlood = 0;
			float currentBlood = 0;
			if (organs.hasHeart()) {
				maximumBlood = getNonNegativeValue(organs.getHeart().getMaxBlood());
				currentBlood = keepValueInRange(organs.getHeart().getBlood(), 0, maximumBlood);
			}

			float requestedFluidCapacity;
			if (cardiovascularSystem.hasFluidCapacity()) {
				requestedFluidCapacity = getNonNegativeValue(
						cardiovascularSystem.getFluidCapacity());
			} else {
				requestedFluidCapacity = maximumBlood + ChemicalUnits.DEFAULT_INJECTION_RESERVE;
			}

			configuredFluidCapacity = Math.max(maximumBlood, requestedFluidCapacity);
			availableBloodstreamChemicalSpace = Math.max(0, configuredFluidCapacity - currentBlood);

			for (Chemical chemical : cardiovascularSystem.getChemicalsList()) {
				addChemicalAmount(
						bloodstreamChemicals,
						chemical.getName(),
						chemical.getAmount());
			}

			limitChemicalAmount(bloodstreamChemicals, availableBloodstreamChemicalSpace);

			if (organs.hasStomach()) {
				Stomach stomach = organs.getStomach();

				if (stomach.hasChemicalCapacity()) {
					stomachChemicalCapacity = getNonNegativeValue(stomach.getChemicalCapacity());
				} else {
					stomachChemicalCapacity = ChemicalUnits.DEFAULT_STOMACH_CAPACITY;
				}

				for (String chemicalName : stomach.getChemicalsList()) {
					addChemicalAmount(stomachChemicals, chemicalName, 1);
				}

				for (Chemical chemical : stomach.getContentsList()) {
					addChemicalAmount(
							stomachChemicals,
							chemical.getName(),
							chemical.getAmount());
				}

				limitChemicalAmount(stomachChemicals, stomachChemicalCapacity);
			}
		}

		private void resolveOrganEnergyUse() {
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
				float deliveredPower = Math.min(storedElectricalPower, totalPowerUse);
				availableCyberneticPowerRatio = deliveredPower / totalPowerUse;
				storedElectricalPower -= deliveredPower;
			}

			if (totalNutritionUse > 0) {
				float deliveredNutrition = Math.min(storedNutrition, totalNutritionUse);
				availableBiologicalNutritionRatio = deliveredNutrition / totalNutritionUse;
				storedNutrition -= deliveredNutrition;
			}
		}

		private float getOrganFunction(OrganStatus organStatus) {
			float organHealth = getOrganCondition(
					organStatus.hasHealth(),
					organStatus.getHealth());
			float organEfficiency = getOrganCondition(
					organStatus.hasEfficiency(),
					organStatus.getEfficiency());
			float availablePower = 1;

			if (organStatus.getType() == OrganType.ORGAN_TYPE_CYBERNETIC) {
				availablePower = availableCyberneticPowerRatio;
			} else {
				availablePower = availableBiologicalNutritionRatio;
			}

			return organHealth * organEfficiency * availablePower;
		}

		private void addOxygenSupply(float oxygenAmount) {
			oxygenAddedByLungs += getNonNegativeValue(oxygenAmount);
		}

		private void addOxygenUse(float oxygenAmount) {
			totalOxygenUse += getNonNegativeValue(oxygenAmount);
		}

		private void addBrainOxygenUse(float oxygenAmount) {
			float safeOxygenAmount = getNonNegativeValue(oxygenAmount);
			brainOxygenUse += safeOxygenAmount;
			totalOxygenUse += safeOxygenAmount;
		}

		private void addAvailableCirculation(float circulationAmount) {
			availableCirculation += getNonNegativeValue(circulationAmount);
		}

		private void addDetoxification(float detoxificationAmount) {
			availableDetoxification += getNonNegativeValue(detoxificationAmount);
		}

		private void absorbStomachChemicals(float absorptionAmount) {
			float totalStomachChemicalAmount = getTotalChemicalAmount(stomachChemicals);
			float currentBloodstreamChemicalAmount = getTotalChemicalAmount(bloodstreamChemicals);
			float availableChemicalSpace = availableBloodstreamChemicalSpace
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
				addChemicalAmount(
						bloodstreamChemicals,
						stomachChemical.getKey(),
						amountMovedToBloodstream);
			}
		}

		private void resolveOxygenChanges() {
			storedOxygen += oxygenAddedByLungs;
			storedOxygen = Math.min(maximumOxygenForCurrentBlood, storedOxygen);

			float oxygenAllowedByCirculation = Math.min(availableCirculation, totalOxygenUse);
			float deliveredOxygen = Math.min(storedOxygen, oxygenAllowedByCirculation);
			storedOxygen -= deliveredOxygen;

			float brainShareOfOxygenUse = 0;
			if (totalOxygenUse > 0) {
				brainShareOfOxygenUse = brainOxygenUse / totalOxygenUse;
			}

			float oxygenDeliveredToBrain = deliveredOxygen * brainShareOfOxygenUse;
			missingBrainOxygen = Math.max(0, brainOxygenUse - oxygenDeliveredToBrain);
		}

		private void resolveChemicalChanges() {
			float totalBloodstreamChemicalAmount = getTotalChemicalAmount(bloodstreamChemicals);

			if (totalBloodstreamChemicalAmount <= 0 || availableDetoxification <= 0) {
				return;
			}

			float removedChemicalRatio = availableDetoxification / totalBloodstreamChemicalAmount;
			removedChemicalRatio = Math.min(1, removedChemicalRatio);

			for (Map.Entry<String, Float> chemical : bloodstreamChemicals.entrySet()) {
				float remainingAmount = chemical.getValue() * (1 - removedChemicalRatio);
				chemical.setValue(remainingAmount);
			}
		}

		private static void addChemicalAmount(
				Map<String, Float> chemicals,
				String chemicalName,
				float chemicalAmount) {

			float safeChemicalAmount = getNonNegativeValue(chemicalAmount);
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

		private static float getTotalChemicalAmount(Map<String, Float> chemicals) {
			float totalChemicalAmount = 0;
			for (float chemicalAmount : chemicals.values()) {
				totalChemicalAmount += getNonNegativeValue(chemicalAmount);
			}
			return totalChemicalAmount;
		}

		private static void limitChemicalAmount(
				Map<String, Float> chemicals,
				float chemicalCapacity) {

			float totalChemicalAmount = getTotalChemicalAmount(chemicals);
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
	}
}
