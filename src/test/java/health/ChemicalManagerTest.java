package health;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.OrgansProto.CardiovascularSystem;
import protonova.protobuf.OrgansProto.Heart;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.OrgansProto.Stomach;

public class ChemicalManagerTest {

	private static final float DELTA = 0.0001f;
	private final ChemicalManager chemicals = new ChemicalManager();

	@Test
	public void addingToStomachAcceptsOnlyRemainingSpace() {
		Entity entity = Entity.newBuilder()
				.setOrgans(Organs.newBuilder().setStomach(Stomach.newBuilder()
						.addContents(Chemical.newBuilder().setId(1).setAmount(45))))
				.build();

		Entity updated = chemicals.addToStomach(entity, 2, 10);

		assertEquals(50.0f, totalStomachChemicals(updated), DELTA);
		assertEquals(0.0f, chemicals.getRemainingStomachCapacity(updated), DELTA);
	}

	@Test
	public void injectionAcceptsOnlySpaceBeyondBloodVolume() {
		Entity entity = Entity.newBuilder()
				.setOrgans(Organs.newBuilder()
						.setHeart(Heart.newBuilder().setBlood(100).setMaxBlood(100))
						.setCardiovascularSystem(CardiovascularSystem.newBuilder()
								.addChemicals(Chemical.newBuilder().setId(1).setAmount(45))))
				.build();

		Entity updated = chemicals.injectIntoCirculation(entity, 2, 10);

		assertEquals(50.0f, totalCirculatingChemicals(updated), DELTA);
		assertEquals(150.0f,
				updated.getOrgans().getCardiovascularSystem().getFluidCapacity(), DELTA);
		assertEquals(0.0f, chemicals.getRemainingCirculationCapacity(updated), DELTA);
	}

	private static float totalStomachChemicals(Entity entity) {
		float total = 0;
		for (Chemical chemical : entity.getOrgans().getStomach().getContentsList()) {
			total += chemical.getAmount();
		}
		return total;
	}

	private static float totalCirculatingChemicals(Entity entity) {
		float total = 0;
		for (Chemical chemical : entity.getOrgans().getCardiovascularSystem().getChemicalsList()) {
			total += chemical.getAmount();
		}
		return total;
	}
}
