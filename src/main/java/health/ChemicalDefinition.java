package health;

import com.google.gson.JsonObject;

import protonova.protobuf.DamageProto.Damage;

public class ChemicalDefinition {

    private String name;

    private Double unitsPerSecond;

    private Double overdose;

    private Double minTempature;

    private Double maxTempature;

    private Double minDamage;

    private Double maxDamage;

    private Boolean canHealDead;

    // Normal Amount

    private Damage healing;

    private Damage damage;

    private Double movementMultiplyer;

    private Double saturation;

    // Overdose Amount

    private Damage overdoseHealing;

    private Damage overdoseDamage;

    private Double overdoseMovementMultiplyer;

    private Double overdoseSaturation;

    public ChemicalDefinition(JsonObject chemicalJson) {

        name = chemicalJson.has("name")
                ? chemicalJson.get("name").getAsString()
                : null;

        unitsPerSecond = chemicalJson.has("unitsPerSecond")
                ? chemicalJson.get("unitsPerSecond").getAsDouble()
                : null;

        overdose = chemicalJson.has("overdose")
                ? chemicalJson.get("overdose").getAsDouble()
                : null;

        minTempature = chemicalJson.has("minTempature")
                ? chemicalJson.get("minTempature").getAsDouble()
                : null;

        maxTempature = chemicalJson.has("maxTempature")
                ? chemicalJson.get("maxTempature").getAsDouble()
                : null;

        minDamage = chemicalJson.has("minDamage")
                ? chemicalJson.get("minDamage").getAsDouble()
                : null;

        maxDamage = chemicalJson.has("maxDamage")
                ? chemicalJson.get("maxDamage").getAsDouble()
                : null;

        canHealDead = chemicalJson.has("canHealDead")
                ? chemicalJson.get("canHealDead").getAsBoolean()
                : null;

        // Normal Effects

        JsonObject effects = chemicalJson.has("effects")
                ? chemicalJson.getAsJsonObject("effects")
                : null;

        if (effects != null) {

            healing = effects.has("healing")
                    ? getDamageFromJson(effects.getAsJsonObject("healing"))
                    : null;

            damage = effects.has("damage")
                    ? getDamageFromJson(effects.getAsJsonObject("damage"))
                    : null;

            movementMultiplyer = effects.has("movementMultiplyer")
                    ? effects.get("movementMultiplyer").getAsDouble()
                    : null;

            saturation = effects.has("saturation")
                    ? effects.get("saturation").getAsDouble()
                    : null;
        }

        // Overdose Effects

        JsonObject overdoseEffects = chemicalJson.has("overdoseEffects")
                ? chemicalJson.getAsJsonObject("overdoseEffects")
                : null;

        if (overdoseEffects != null) {

            overdoseHealing = overdoseEffects.has("healing")
                    ? getDamageFromJson(overdoseEffects.getAsJsonObject("healing"))
                    : null;

            overdoseDamage = overdoseEffects.has("damage")
                    ? getDamageFromJson(overdoseEffects.getAsJsonObject("damage"))
                    : null;

            overdoseMovementMultiplyer = overdoseEffects.has("movementMultiplyer")
                    ? overdoseEffects.get("movementMultiplyer").getAsDouble()
                    : null;

            overdoseSaturation = overdoseEffects.has("saturation")
                    ? overdoseEffects.get("saturation").getAsDouble()
                    : null;
        }
    }

    public String getName() {
        return name;
    }

    public Double getUnitsPerSecond() {
        return unitsPerSecond;
    }

    public Double getOverdose() {
        return overdose;
    }

    public Double getMinTempature() {
        return minTempature;
    }

    public Double getMaxTempature() {
        return maxTempature;
    }

    public Double getMinDamage() {
        return minDamage;
    }

    public Double getMaxDamage() {
        return maxDamage;
    }

    public Boolean getCanHealDead() {
        return canHealDead;
    }

    public Damage getHealing() {
        return healing;
    }

    public Damage getDamage() {
        return damage;
    }

    public Double getMovementMultiplyer() {
        return movementMultiplyer;
    }

    public Double getSaturation() {
        return saturation;
    }

    public Damage getOverdoseHealing() {
        return overdoseHealing;
    }

    public Damage getOverdoseDamage() {
        return overdoseDamage;
    }

    public Double getOverdoseMovementMultiplyer() {
        return overdoseMovementMultiplyer;
    }

    public Double getOverdoseSaturation() {
        return overdoseSaturation;
    }

    private Damage getDamageFromJson(JsonObject jsonObject) {

        Damage.Builder damage = Damage.newBuilder();

        damage.setBruteDamage(
                jsonObject.has("brute")
                        ? jsonObject.get("brute").getAsFloat()
                        : 0.0f
        );

        damage.setAsphyxiationDamage(
                jsonObject.has("asphyxiation")
                        ? jsonObject.get("asphyxiation").getAsFloat()
                        : 0.0f
        );

        damage.setBurnDamage(
                jsonObject.has("burn")
                        ? jsonObject.get("burn").getAsFloat()
                        : 0.0f
        );

        damage.setToxinDamage(
                jsonObject.has("toxin")
                        ? jsonObject.get("toxin").getAsFloat()
                        : 0.0f
        );

        damage.setGeneticDamage(
                jsonObject.has("genetic")
                        ? jsonObject.get("genetic").getAsFloat()
                        : 0.0f
        );

        damage.setStructuralDamage(
                jsonObject.has("structural")
                        ? jsonObject.get("structural").getAsFloat()
                        : 0.0f
        );

        float bleeding = jsonObject.has("bleedingPerSecond")
                ? jsonObject.get("bleedingPerSecond").getAsFloat()
                : jsonObject.has("bleeding")
                        ? jsonObject.get("bleeding").getAsFloat()
                        : 0.0f;
        damage.setBleedingPerSecond(bleeding);

        return damage.build();
    }
}
