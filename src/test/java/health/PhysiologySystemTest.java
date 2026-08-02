package health;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.Brain;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.Heart;
import protonova.protobuf.OrgansProto.Liver;
import protonova.protobuf.OrgansProto.Lungs;
import protonova.protobuf.OrgansProto.OrganStatus;
import protonova.protobuf.OrgansProto.OrganType;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

public class PhysiologySystemTest {

	private static final float DELTA = 0.0001f;

	private final PhysiologySystem physiology = new PhysiologySystem();

	@Test
	public void healthyOrgansOxygenateAndConsumeBloodOxygen() {
		Entity updated = physiology.update(entityWith(
				Heart.newBuilder().setBlood(100).setMaxBlood(100).setCirculationPerSecond(10).build(),
				Lungs.newBuilder().setOxygen(10).build(),
				Liver.newBuilder().setDetoxification(0).build(),
				Brain.newBuilder().build(),
				Stomach.newBuilder().setAbsorptionPerSecond(0).build(),
				CardiovascularSystem.newBuilder().setMaxOxygen(100).build()));

		assertEquals(4.0f, updated.getOrgans().getCardiovascularSystem().getOxygen(), DELTA);
		assertEquals(0.0f, updated.getDamage().getAsphyxiationDamage(), DELTA);
	}

	@Test
	public void brainTakesAsphyxiationDamageWithoutBreathableOxygen() {
		Entity updated = physiology.update(entityWith(
				Heart.newBuilder().setBlood(100).setMaxBlood(100).setCirculationPerSecond(10).build(),
				Lungs.newBuilder().setOxygen(10).build(),
				Liver.getDefaultInstance(),
				Brain.newBuilder().build(),
				Stomach.newBuilder().setAbsorptionPerSecond(0).build(),
				CardiovascularSystem.newBuilder().setMaxOxygen(100).build()), 0);

		assertEquals(2.0f, updated.getDamage().getAsphyxiationDamage(), DELTA);
	}

	@Test
	public void stomachAbsorbsChemicalsAndLiverDetoxifiesThem() {
		Stomach stomach = Stomach.newBuilder()
				.setAbsorptionPerSecond(4)
				.addContents(Chemical.newBuilder().setId(7).setAmount(10))
				.build();
		Entity updated = physiology.update(entityWith(
				Heart.newBuilder().setBlood(100).setMaxBlood(100).build(),
				Lungs.newBuilder().setOxygen(10).build(),
				Liver.newBuilder().setDetoxification(2).build(),
				Brain.newBuilder().build(),
				stomach,
				CardiovascularSystem.newBuilder().setMaxOxygen(100).build()));

		assertEquals(6.0f, updated.getOrgans().getStomach().getContents(0).getAmount(), DELTA);
		assertEquals(2.0f,
				updated.getOrgans().getCardiovascularSystem().getChemicals(0).getAmount(), DELTA);
	}

	@Test
	public void cyberneticOrgansScaleDownWhenPowerIsLimited() {
		OrganStatus cybernetic = OrganStatus.newBuilder()
				.setType(OrganType.ORGAN_TYPE_CYBERNETIC)
				.setPowerUsePerSecond(5)
				.build();
		Entity updated = physiology.update(entityWith(
				Heart.newBuilder().setBlood(100).setMaxBlood(100).setCirculationPerSecond(10).build(),
				Lungs.newBuilder().setOxygen(10).setStatus(cybernetic).build(),
				Liver.newBuilder().setDetoxification(0).setOxygenUsePerSecond(0).build(),
				Brain.newBuilder().build(),
				Stomach.newBuilder().setAbsorptionPerSecond(0).setOxygenUsePerSecond(0).build(),
				CardiovascularSystem.newBuilder()
						.setMaxOxygen(100)
						.setElectricalPower(2.5f)
						.setMaxElectricalPower(5)
						.build()));

		assertEquals(0.75f, updated.getOrgans().getCardiovascularSystem().getOxygen(), DELTA);
		assertEquals(0.0f, updated.getOrgans().getCardiovascularSystem().getElectricalPower(), DELTA);
		assertEquals(0.0f, updated.getDamage().getAsphyxiationDamage(), DELTA);
	}

	@Test
	public void normalStomachCapacityIsFiftyUnits() {
		Stomach stomach = Stomach.newBuilder()
				.setAbsorptionPerSecond(0)
				.addContents(Chemical.newBuilder().setId(7).setAmount(60))
				.build();
		Entity updated = physiology.update(entityWith(
				Heart.newBuilder().setBlood(100).setMaxBlood(100).build(),
				Lungs.newBuilder().setOxygen(10).build(),
				Liver.newBuilder().setDetoxification(0).build(),
				Brain.newBuilder().build(),
				stomach,
				CardiovascularSystem.newBuilder().setMaxOxygen(100).build()));

		assertEquals(50.0f, updated.getOrgans().getStomach().getChemicalCapacity(), DELTA);
		assertEquals(50.0f, updated.getOrgans().getStomach().getContents(0).getAmount(), DELTA);
	}

	@Test
	public void normalCirculationFitsBloodAndFiftyInjectedUnits() {
		CardiovascularSystem cardiovascular = CardiovascularSystem.newBuilder()
				.setMaxOxygen(100)
				.addChemicals(Chemical.newBuilder().setId(9).setAmount(80))
				.build();
		Entity updated = physiology.update(entityWith(
				Heart.newBuilder().setBlood(100).setMaxBlood(100).build(),
				Lungs.newBuilder().setOxygen(10).build(),
				Liver.newBuilder().setDetoxification(0).build(),
				Brain.newBuilder().build(),
				Stomach.newBuilder().setAbsorptionPerSecond(0).build(),
				cardiovascular));

		assertEquals(150.0f,
				updated.getOrgans().getCardiovascularSystem().getFluidCapacity(), DELTA);
		assertEquals(50.0f,
				updated.getOrgans().getCardiovascularSystem().getChemicals(0).getAmount(), DELTA);
	}

	@Test
	public void digestionStopsWhenCirculationHasNoChemicalSpace() {
		Stomach stomach = Stomach.newBuilder()
				.setAbsorptionPerSecond(5)
				.addContents(Chemical.newBuilder().setId(7).setAmount(10))
				.build();
		CardiovascularSystem cardiovascular = CardiovascularSystem.newBuilder()
				.setMaxOxygen(100)
				.addChemicals(Chemical.newBuilder().setId(9).setAmount(48))
				.build();
		Entity updated = physiology.update(entityWith(
				Heart.newBuilder().setBlood(100).setMaxBlood(100).build(),
				Lungs.newBuilder().setOxygen(10).build(),
				Liver.newBuilder().setDetoxification(0).build(),
				Brain.newBuilder().build(),
				stomach,
				cardiovascular));

		assertEquals(8.0f, updated.getOrgans().getStomach().getContents(0).getAmount(), DELTA);
		assertEquals(50.0f, totalChemicals(
				updated.getOrgans().getCardiovascularSystem()), DELTA);
	}

	@Test
	public void tenUnitsIsOneStandardDose() {
		assertEquals(1.0f, ChemicalUnits.doses(10), DELTA);
		assertEquals(15.0f, ChemicalUnits.units(1.5f), DELTA);
	}

	private static Entity entityWith(Heart heart, Lungs lungs, Liver liver, Brain brain,
			Stomach stomach, CardiovascularSystem cardiovascular) {
		return Entity.newBuilder()
				.setOrgans(Organs.newBuilder()
						.setHeart(heart)
						.setLungs(lungs)
						.setLiver(liver)
						.setBrain(brain)
						.setStomach(stomach)
						.setCardiovascularSystem(cardiovascular))
				.build();
	}

	private static float totalChemicals(CardiovascularSystem cardiovascular) {
		float total = 0;
		for (Chemical chemical : cardiovascular.getChemicalsList()) {
			total += chemical.getAmount();
		}
		return total;
	}
}
