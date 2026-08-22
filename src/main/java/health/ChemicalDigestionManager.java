package health;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.Organs;

public class ChemicalDigestionManager {

	private final Map<String, ChemicalDefinition> storedChemicals = new HashMap<>();

	public ChemicalDigestionManager() {
		loadChemicalsIntoMemory();
	}

	private void loadChemicalsIntoMemory() {
		File folder = new File("assets/chemicals");
		File[] chemicalFiles = folder.listFiles((directory, name) -> name.endsWith(".json"));
		if (chemicalFiles == null) {
			return;
		}

		for (File file : chemicalFiles) {
			try (FileReader reader = new FileReader(file)) {
				JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
				ChemicalDefinition chemical = new ChemicalDefinition(jsonObject);
				if (chemical.getName() != null && !chemical.getName().trim().isEmpty()) {
					storedChemicals.put(normalizeName(chemical.getName()), chemical);
				}
			} catch (IOException | RuntimeException exception) {
				exception.printStackTrace();
			}
		}
	}

	public Entity processEntityChemicals(Entity entity) {
		if (entity == null
				|| !entity.hasOrgans()
				|| !entity.getOrgans().hasCardiovascularSystem()) {
			return entity;
		}

		Entity.Builder updatedEntity = entity.toBuilder();
		Organs.Builder updatedOrgans = entity.getOrgans().toBuilder();
		CardiovascularSystem updatedCirculation = entity.getOrgans().getCardiovascularSystem();
		CardiovascularSystem.Builder chemicalChanges = updatedCirculation.toBuilder().clearChemicals();

		for (Chemical chemical : updatedCirculation.getChemicalsList()) {
			float amount = positive(chemical.getAmount());
			ChemicalDefinition definition = storedChemicals.get(normalizeName(chemical.getName()));

			if (definition == null || amount <= 0) {
				if (amount > 0) {
					chemicalChanges.addChemicals(chemical.toBuilder().setAmount(amount));
				}
				continue;
			}

			if (isInTemperatureRange(updatedEntity, definition)) {
				boolean overdose = isOverdose(chemical, definition);
				applyDamageEffects(updatedEntity, definition, overdose);
				updatedCirculation = applyNutritionEffect(
						updatedCirculation,
						getSaturation(definition, overdose));
			}

			float remainingAmount = Math.max(
					0,
					amount - positive(definition.getUnitsPerSecond()));
			if (remainingAmount > 0) {
				chemicalChanges.addChemicals(
						chemical.toBuilder().setAmount(remainingAmount));
			}
		}

		updatedCirculation = updatedCirculation.toBuilder()
				.clearChemicals()
				.addAllChemicals(chemicalChanges.getChemicalsList())
				.build();
		updatedOrgans.setCardiovascularSystem(updatedCirculation);
		updatedEntity.setOrgans(updatedOrgans);
		return updatedEntity.build();
	}

	private void applyDamageEffects(
			Entity.Builder entity,
			ChemicalDefinition definition,
			boolean overdose) {
		Damage damage = overdose ? definition.getOverdoseDamage() : definition.getDamage();
		Damage healing = overdose ? definition.getOverdoseHealing() : definition.getHealing();
		if (damage == null) {
			damage = Damage.getDefaultInstance();
		}
		if (healing == null || !canHeal(entity, definition)) {
			healing = Damage.getDefaultInstance();
		}

		Damage current = entity.getDamage();
		Damage.Builder updated = current.toBuilder()
				.setBruteDamage(resolveDamage(
						current.getBruteDamage(), damage.getBruteDamage(), healing.getBruteDamage()))
				.setAsphyxiationDamage(resolveDamage(
						current.getAsphyxiationDamage(), damage.getAsphyxiationDamage(), healing.getAsphyxiationDamage()))
				.setBurnDamage(resolveDamage(
						current.getBurnDamage(), damage.getBurnDamage(), healing.getBurnDamage()))
				.setToxinDamage(resolveDamage(
						current.getToxinDamage(), damage.getToxinDamage(), healing.getToxinDamage()))
				.setGeneticDamage(resolveDamage(
						current.getGeneticDamage(), damage.getGeneticDamage(), healing.getGeneticDamage()))
				.setStructuralDamage(resolveDamage(
						current.getStructuralDamage(), damage.getStructuralDamage(), healing.getStructuralDamage()))
				.setBleedingPerSecond(resolveDamage(
						current.getBleedingPerSecond(), damage.getBleedingPerSecond(), healing.getBleedingPerSecond()));
		entity.setDamage(updated);

		if (!entity.getAlive()
				&& Boolean.TRUE.equals(definition.getCanHealDead())
				&& totalDamage(updated) < entity.getMaxHealth()) {
			entity.setAlive(true);
		}
	}

	private static boolean canHeal(Entity.Builder entity, ChemicalDefinition definition) {
		if (!entity.getAlive() && !Boolean.TRUE.equals(definition.getCanHealDead())) {
			return false;
		}

		float damage = totalDamage(entity.getDamage());
		Double minimum = definition.getMinDamage();
		Double maximum = definition.getMaxDamage();
		return (minimum == null || minimum <= 0 || damage >= minimum)
				&& (maximum == null || maximum <= 0 || damage <= maximum);
	}

	private static CardiovascularSystem applyNutritionEffect(
			CardiovascularSystem cardiovascularSystem,
			Double saturation) {
		if (saturation == null || !Double.isFinite(saturation) || saturation == 0) {
			return cardiovascularSystem;
		}
		if (saturation > 0) {
			return OrganEnergy.addNutrition(cardiovascularSystem, saturation.floatValue());
		}
		return OrganEnergy.consumeNutrition(cardiovascularSystem, -saturation.floatValue());
	}

	private static Double getSaturation(ChemicalDefinition definition, boolean overdose) {
		return overdose ? definition.getOverdoseSaturation() : definition.getSaturation();
	}

	private static boolean isOverdose(Chemical chemical, ChemicalDefinition definition) {
		Double overdose = definition.getOverdose();
		return overdose != null && overdose > 0 && chemical.getAmount() >= overdose;
	}

	private static boolean isInTemperatureRange(
			Entity.Builder entity,
			ChemicalDefinition definition) {
		Double minimum = definition.getMinTempature();
		Double maximum = definition.getMaxTempature();
		return (minimum == null || entity.getTemperature() >= minimum)
				&& (maximum == null || entity.getTemperature() <= maximum);
	}

	private static float resolveDamage(float current, float damage, float healing) {
		return Math.max(0, positive(current) + positive(damage) - positive(healing));
	}

	private static float totalDamage(Damage damage) {
		return positive(damage.getBruteDamage())
				+ positive(damage.getAsphyxiationDamage())
				+ positive(damage.getBurnDamage())
				+ positive(damage.getToxinDamage())
				+ positive(damage.getGeneticDamage())
				+ positive(damage.getStructuralDamage());
	}

	private static float totalDamage(Damage.Builder damage) {
		return totalDamage(damage.build());
	}

	private static float positive(Double value) {
		return value != null && Double.isFinite(value) ? Math.max(0, value.floatValue()) : 0;
	}

	private static float positive(float value) {
		return Float.isFinite(value) ? Math.max(0, value) : 0;
	}

	private static String normalizeName(String name) {
		return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
	}
}
