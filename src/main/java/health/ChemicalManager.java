package health;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Locale;

import entity.EntityManager;
import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.Heart;
import protonova.protobuf.OrgansProto.OrganComponent;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

public final class ChemicalManager {
	private final EntityManager entityManager;

	public ChemicalManager() {
		this(null);
	}

	public ChemicalManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Entity addToStomach(Entity entity, String chemicalName, float requestedUnits) {
		Objects.requireNonNull(entity, "entity");
		chemicalName = normalizeName(chemicalName);
		if (chemicalName.isEmpty()) return entity;
		Stomach stomach = getStomach(entity);
		if (stomach == null) return entity;

		float capacity = stomach.hasChemicalCapacity()
				? positive(stomach.getChemicalCapacity())
				: ChemicalUnits.DEFAULT_STOMACH_CAPACITY;
		Map<String, Float> contents = stomachContents(stomach);
		float accepted = Math.min(positive(requestedUnits), remaining(capacity, contents));
		if (accepted <= 0) return entity;

		contents.merge(chemicalName, accepted, Float::sum);
		Stomach.Builder updatedStomach = stomach.toBuilder()
				.clearChemicals()
				.clearContents()
				.setChemicalCapacity(capacity);
		addChemicals(updatedStomach, contents);

		return storeStomach(entity, updatedStomach.build());
	}

	public Entity injectIntoCirculation(Entity entity, String chemicalName, float requestedUnits) {
		Objects.requireNonNull(entity, "entity");
		chemicalName = normalizeName(chemicalName);
		if (chemicalName.isEmpty()) return entity;
		if (!entity.hasOrgans()) return entity;

		Organs organs = entity.getOrgans();
		CardiovascularSystem cardiovascular = organs.hasCardiovascularSystem()
				? organs.getCardiovascularSystem()
				: CardiovascularSystem.getDefaultInstance();
		Heart heart = getHeart(entity);
		float maximumBlood = heart == null ? 0 : positive(heart.getMaxBlood());
		float currentBlood = heart == null
				? 0
				: clamp(heart.getBlood(), 0, maximumBlood);
		float capacity = cardiovascular.hasFluidCapacity()
				? Math.max(maximumBlood, positive(cardiovascular.getFluidCapacity()))
				: maximumBlood + ChemicalUnits.DEFAULT_INJECTION_RESERVE;
		Map<String, Float> chemicals = chemicalContents(cardiovascular);
		float availableChemicalSpace = Math.max(0, capacity - currentBlood - total(chemicals));
		float accepted = Math.min(positive(requestedUnits), availableChemicalSpace);
		if (accepted <= 0) return entity;

		chemicals.merge(chemicalName, accepted, Float::sum);
		CardiovascularSystem.Builder updatedCardiovascular = cardiovascular.toBuilder()
				.clearChemicals()
				.setFluidCapacity(capacity);
		for (Map.Entry<String, Float> entry : chemicals.entrySet()) {
			updatedCardiovascular.addChemicals(chemical(entry.getKey(), entry.getValue()));
		}

		return entity.toBuilder()
				.setOrgans(organs.toBuilder().setCardiovascularSystem(updatedCardiovascular))
				.build();
	}

	public float getRemainingStomachCapacity(Entity entity) {
		if (entity == null) return 0;
		Stomach stomach = getStomach(entity);
		if (stomach == null) return 0;
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
		Heart heart = getHeart(entity);
		float maximumBlood = heart == null ? 0 : positive(heart.getMaxBlood());
		float currentBlood = heart == null
				? 0
				: clamp(heart.getBlood(), 0, maximumBlood);
		float capacity = cardiovascular.hasFluidCapacity()
				? Math.max(maximumBlood, positive(cardiovascular.getFluidCapacity()))
				: maximumBlood + ChemicalUnits.DEFAULT_INJECTION_RESERVE;
		return Math.max(0, capacity - currentBlood - total(chemicalContents(cardiovascular)));
	}

	private Stomach getStomach(Entity body) {
		Entity organEntity = getInstalledOrgan(body, getStomachEntityId(body));
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasStomach()) {
			return organEntity.getOrganComponent().getStomach();
		}
		if (body.hasOrgans() && body.getOrgans().hasStomach()) {
			return body.getOrgans().getStomach();
		}
		return null;
	}

	private Heart getHeart(Entity body) {
		Entity organEntity = getInstalledOrgan(body, getHeartEntityId(body));
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasHeart()) {
			return organEntity.getOrganComponent().getHeart();
		}
		if (body.hasOrgans() && body.getOrgans().hasHeart()) {
			return body.getOrgans().getHeart();
		}
		return null;
	}

	private Entity storeStomach(Entity body, Stomach stomach) {
		Entity organEntity = getInstalledOrgan(body, getStomachEntityId(body));
		if (organEntity != null
				&& organEntity.hasOrganComponent()
				&& organEntity.getOrganComponent().hasStomach()) {
			OrganComponent updatedComponent = organEntity.getOrganComponent().toBuilder()
					.setStomach(stomach)
					.build();
			entityManager.updateEntity(organEntity.toBuilder()
					.setOrganComponent(updatedComponent)
					.build());
			return body;
		}

		Organs updatedOrgans = body.getOrgans().toBuilder()
				.setStomach(stomach)
				.build();
		return body.toBuilder().setOrgans(updatedOrgans).build();
	}

	private Entity getInstalledOrgan(Entity body, int organEntityId) {
		if (entityManager == null || organEntityId < 0) return null;
		return entityManager.getEntity(organEntityId);
	}

	private static int getHeartEntityId(Entity body) {
		return body.hasOrganSlots() && body.getOrganSlots().hasHeartEntityId()
				? body.getOrganSlots().getHeartEntityId()
				: -1;
	}

	private static int getStomachEntityId(Entity body) {
		return body.hasOrganSlots() && body.getOrganSlots().hasStomachEntityId()
				? body.getOrganSlots().getStomachEntityId()
				: -1;
	}

	private static Map<String, Float> stomachContents(Stomach stomach) {
		Map<String, Float> contents = new TreeMap<>();
		for (String chemicalName : stomach.getChemicalsList()) {
			chemicalName = normalizeName(chemicalName);
			if (!chemicalName.isEmpty()) contents.merge(chemicalName, 1.0f, Float::sum);
		}
		for (Chemical chemical : stomach.getContentsList()) {
			String chemicalName = normalizeName(chemical.getName());
			if (!chemicalName.isEmpty()) {
				contents.merge(chemicalName, positive(chemical.getAmount()), Float::sum);
			}
		}
		return contents;
	}

	private static Map<String, Float> chemicalContents(CardiovascularSystem cardiovascular) {
		Map<String, Float> contents = new TreeMap<>();
		for (Chemical chemical : cardiovascular.getChemicalsList()) {
			String chemicalName = normalizeName(chemical.getName());
			if (!chemicalName.isEmpty()) {
				contents.merge(chemicalName, positive(chemical.getAmount()), Float::sum);
			}
		}
		return contents;
	}

	private static void addChemicals(Stomach.Builder stomach, Map<String, Float> contents) {
		for (Map.Entry<String, Float> entry : contents.entrySet()) {
			if (entry.getValue() > 0) stomach.addContents(chemical(entry.getKey(), entry.getValue()));
		}
	}

	private static Chemical chemical(String name, float amount) {
		return Chemical.newBuilder().setName(name).setAmount(amount).build();
	}

	private static float remaining(float capacity, Map<String, Float> contents) {
		return Math.max(0, capacity - total(contents));
	}

	private static float total(Map<String, Float> contents) {
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

	private static String normalizeName(String name) {
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}
}
