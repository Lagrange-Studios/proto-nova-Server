package health;

import java.util.Map;
import java.util.Objects;

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
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

public final class PhysiologySystem {

	private static final float DEFAULT_HEART_OXYGEN_USE = 1.0f;
	private static final float DEFAULT_LUNG_OXYGEN_USE = 0.5f;
	private static final float DEFAULT_LIVER_OXYGEN_USE = 1.0f;
	private static final float DEFAULT_BRAIN_OXYGEN_USE = 3.0f;
	private static final float DEFAULT_STOMACH_OXYGEN_USE = 0.5f;
	private static final float DEFAULT_STOMACH_ABSORPTION = 1.0f;
	private static final float ASPHYXIATION_DAMAGE_PER_SECOND = 2.0f;
	private static final float ASPHYXIATION_RECOVERY_PER_SECOND = 0.5f;
	private static final float BLEEDING_RECOVERY_PER_SECOND = 1.0f;
	private static final float LIVER_TOXIN_RECOVERY_PER_DETOX_UNIT = 0.05f;

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

		updateHeartEntity(updatedBody, organSlots, entityManager);
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

		Entity updated = updateBleeding(entity);
		PhysiologyState state = new PhysiologyState(updated, clamp(breathableOxygen, 0, 1));
		state.useEnergy();

		updateLungs(state);
		updateHeart(state);
		updateLiver(state);
		updateStomach(state);
		updateBrain(state);

		state.useOxygen();
		return applyChanges(updated, state);
	}

	private Entity updateBleeding(Entity entity) {
		Damage.Builder damage = entity.getDamage().toBuilder();
		float bleedingPerSecond = positive(damage.getBleedingPerSecond());
		damage.setBleedingPerSecond(Math.max(
				0,
				bleedingPerSecond - BLEEDING_RECOVERY_PER_SECOND));

		Organs.Builder organs = entity.getOrgans().toBuilder();
		if (bleedingPerSecond > 0 && organs.hasHeart()) {
			Heart heart = organs.getHeart();
			float maximumBlood = positive(heart.getMaxBlood());
			float currentBlood = clamp(heart.getBlood(), 0, maximumBlood);
			organs.setHeart(heart.toBuilder()
					.setBlood(Math.max(0, currentBlood - bleedingPerSecond)));
		}

		return entity.toBuilder()
				.setDamage(damage)
				.setOrgans(organs)
				.build();
	}

	private void updateLungs(PhysiologyState state) {
		if (!state.organs.hasLungs()) return;

		Lungs lungs = state.organs.getLungs();
		float organFunction = state.getOrganFunction(lungs.getStatus());
		float oxygenTransfer = positive(lungs.getOxygen());
		oxygenTransfer = oxygenTransfer * organFunction * state.breathableOxygen;

		float oxygenUse = valueOrDefault(
				lungs.hasOxygenUsePerSecond(),
				lungs.getOxygenUsePerSecond(),
				DEFAULT_LUNG_OXYGEN_USE);

		state.addOxygen(oxygenTransfer);
		state.addOxygenUse(oxygenUse * organFunction);
	}

	private void updateHeart(PhysiologyState state) {
		if (!state.organs.hasHeart()) return;

		Heart heart = state.organs.getHeart();
		float organFunction = state.getOrganFunction(heart.getStatus());
		float maximumBlood = positive(heart.getMaxBlood());
		float currentBloodRatio = 0;

		if (maximumBlood > 0) {
			currentBloodRatio = clamp(heart.getBlood() / maximumBlood, 0, 1);
		}

		float circulationPerSecond = valueOrDefault(
				heart.hasCirculationPerSecond(),
				heart.getCirculationPerSecond(),
				maximumBlood);
		float oxygenUse = valueOrDefault(
				heart.hasOxygenUsePerSecond(),
				heart.getOxygenUsePerSecond(),
				DEFAULT_HEART_OXYGEN_USE);

		float availableCirculation = positive(circulationPerSecond);
		availableCirculation = availableCirculation * currentBloodRatio * organFunction;

		state.addCirculation(availableCirculation);
		state.addOxygenUse(oxygenUse * organFunction);
	}

	private void updateLiver(PhysiologyState state) {
		if (!state.organs.hasLiver()) return;

		Liver liver = state.organs.getLiver();
		float organFunction = state.getOrganFunction(liver.getStatus());
		float oxygenUse = valueOrDefault(
				liver.hasOxygenUsePerSecond(),
				liver.getOxygenUsePerSecond(),
				DEFAULT_LIVER_OXYGEN_USE);
		float toxinRecovery = positive(liver.getDetoxification())
				* organFunction
				* LIVER_TOXIN_RECOVERY_PER_DETOX_UNIT;

		state.addToxinRecovery(toxinRecovery);
		state.addOxygenUse(oxygenUse * organFunction);
	}

	private void updateStomach(PhysiologyState state) {
		if (!state.organs.hasStomach()) return;

		Stomach stomach = state.organs.getStomach();
		float organFunction = state.getOrganFunction(stomach.getStatus());
		float absorptionPerSecond = valueOrDefault(
				stomach.hasAbsorptionPerSecond(),
				stomach.getAbsorptionPerSecond(),
				DEFAULT_STOMACH_ABSORPTION);
		float oxygenUse = valueOrDefault(
				stomach.hasOxygenUsePerSecond(),
				stomach.getOxygenUsePerSecond(),
				DEFAULT_STOMACH_OXYGEN_USE);

		state.digest(positive(absorptionPerSecond * organFunction));
		state.addOxygenUse(oxygenUse * organFunction);
	}

	private void updateBrain(PhysiologyState state) {
		if (!state.organs.hasBrain()) return;

		Brain brain = state.organs.getBrain();
		float organFunction = state.getOrganFunction(brain.getStatus());
		float oxygenUse = valueOrDefault(
				brain.hasOxygenUsePerSecond(),
				brain.getOxygenUsePerSecond(),
				DEFAULT_BRAIN_OXYGEN_USE);

		state.addBrainOxygen(oxygenUse * organFunction);
	}

	private Entity applyChanges(Entity entity, PhysiologyState state) {
		Organs.Builder updatedOrgans = entity.getOrgans().toBuilder();
		CardiovascularSystem.Builder updatedCardiovascularSystem;

		if (updatedOrgans.hasCardiovascularSystem()) {
			updatedCardiovascularSystem = updatedOrgans.getCardiovascularSystem().toBuilder();
		} else {
			updatedCardiovascularSystem = CardiovascularSystem.newBuilder();
		}

		updatedCardiovascularSystem.setOxygen(state.oxygen);
		updatedCardiovascularSystem.setMaxOxygen(state.maxOxygen);
		updatedCardiovascularSystem.setElectricalPower(state.power);
		updatedCardiovascularSystem.setMaxElectricalPower(state.maxPower);
		updatedCardiovascularSystem.setNutrition(state.nutrition);
		updatedCardiovascularSystem.setMaxNutrition(state.maxNutrition);
		updatedCardiovascularSystem.setFluidCapacity(state.fluidCapacity);
		updatedCardiovascularSystem.clearChemicals();

		for (Map.Entry<String, Float> chemical : state.bloodChemicals.entrySet()) {
			if (chemical.getValue() > 0) {
				updatedCardiovascularSystem.addChemicals(Chemical.newBuilder()
						.setName(chemical.getKey())
						.setAmount(chemical.getValue()));
			}
		}

		updatedOrgans.setCardiovascularSystem(updatedCardiovascularSystem);

		if (updatedOrgans.hasHeart()) {
			Heart currentHeart = updatedOrgans.getHeart();
			float maximumBlood = positive(currentHeart.getMaxBlood());
			float currentBlood = clamp(currentHeart.getBlood(), 0, maximumBlood);
			updatedOrgans.setHeart(currentHeart.toBuilder().setBlood(currentBlood));
		}

		if (updatedOrgans.hasStomach()) {
			Stomach.Builder updatedStomach = updatedOrgans.getStomach().toBuilder();
			updatedStomach.clearChemicals();
			updatedStomach.clearContents();
			updatedStomach.setChemicalCapacity(state.stomachCapacity);

			for (Map.Entry<String, Float> chemical : state.stomachChemicals.entrySet()) {
				if (chemical.getValue() > 0) {
					updatedStomach.addContents(Chemical.newBuilder()
							.setName(chemical.getKey())
							.setAmount(chemical.getValue()));
				}
			}

			updatedOrgans.setStomach(updatedStomach);
		}

		Damage.Builder updatedDamage = entity.getDamage().toBuilder();
		float asphyxiationDamage = positive(updatedDamage.getAsphyxiationDamage());

		if (state.brainOxygenUse > 0 && state.brainOxygenShortage > 0) {
			float oxygenShortageRatio = state.brainOxygenShortage / state.brainOxygenUse;
			oxygenShortageRatio = clamp(oxygenShortageRatio, 0, 1);
			asphyxiationDamage += ASPHYXIATION_DAMAGE_PER_SECOND * oxygenShortageRatio;
		} else {
			asphyxiationDamage -= ASPHYXIATION_RECOVERY_PER_SECOND;
			asphyxiationDamage = Math.max(0, asphyxiationDamage);
		}

		updatedDamage.setAsphyxiationDamage(asphyxiationDamage);
		updatedDamage.setToxinDamage(Math.max(
				0,
				positive(updatedDamage.getToxinDamage()) - state.toxinRecovery));

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

	private static void updateHeartEntity(
			Entity updatedBody,
			OrganSlots organSlots,
			EntityManager entityManager) {

		if (!organSlots.hasHeartEntityId() || !updatedBody.getOrgans().hasHeart()) {
			return;
		}

		Entity heartEntity = entityManager.getEntity(organSlots.getHeartEntityId());
		if (heartEntity == null
				|| !heartEntity.hasOrganComponent()
				|| !heartEntity.getOrganComponent().hasHeart()) {
			return;
		}

		Entity updatedHeartEntity = heartEntity.toBuilder()
				.setOrganComponent(heartEntity.getOrganComponent().toBuilder()
						.setHeart(updatedBody.getOrgans().getHeart()))
				.build();
		entityManager.updateEntity(updatedHeartEntity);
	}

	private static int getHeartEntityId(OrganSlots organSlots) {
		return organSlots.hasHeartEntityId() ? organSlots.getHeartEntityId() : -1;
	}

	private static int getLungsEntityId(OrganSlots organSlots) {
		return organSlots.hasLungsEntityId() ? organSlots.getLungsEntityId() : -1;
	}

	private static int getLiverEntityId(OrganSlots organSlots) {
		return organSlots.hasLiverEntityId() ? organSlots.getLiverEntityId() : -1;
	}

	private static int getBrainEntityId(OrganSlots organSlots) {
		return organSlots.hasBrainEntityId() ? organSlots.getBrainEntityId() : -1;
	}

	private static int getStomachEntityId(OrganSlots organSlots) {
		return organSlots.hasStomachEntityId() ? organSlots.getStomachEntityId() : -1;
	}

	private static float valueOrDefault(boolean valueIsSet, float value, float defaultValue) {
		return valueIsSet ? positive(value) : defaultValue;
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

	private static float positive(float value) {
		return Float.isFinite(value) ? Math.max(0, value) : 0;
	}

	private static float clamp(float value, float minimum, float maximum) {
		if (!Float.isFinite(value)) return minimum;
		return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
	}
}
