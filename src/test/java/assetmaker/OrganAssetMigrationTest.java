package assetmaker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import protonova.protobuf.EntityProto.Entity;

public class OrganAssetMigrationTest {

	private final AssetMaker assets = new AssetMaker();

	@Test
	public void humanUsesOrganAssetTemplatesInsteadOfEmbeddedOrgans() {
		Entity human = assets.loadAsset("human");

		assertEquals("human heart", human.getOrganAssetSlots().getHeartAsset());
		assertEquals("human lungs", human.getOrganAssetSlots().getLungsAsset());
		assertEquals("human liver", human.getOrganAssetSlots().getLiverAsset());
		assertEquals("human brain", human.getOrganAssetSlots().getBrainAsset());
		assertEquals("human stomach", human.getOrganAssetSlots().getStomachAsset());
		assertTrue(human.getTagsList().contains("physiology"));
		assertTrue(human.getHitDamage().getCanAttack());
		assertFalse(human.getOrgans().hasHeart());
		assertFalse(human.getOrgans().hasStomach());
	}

	@Test
	public void newOrganAssetsContainOneMatchingComponent() {
		Entity brain = assets.loadAsset("human brain");
		Entity stomach = assets.loadAsset("human stomach");

		assertTrue(brain.getIsItem());
		assertTrue(brain.getCanDestroy());
		assertTrue(brain.getOrganComponent().hasBrain());
		assertTrue(stomach.getIsItem());
		assertTrue(stomach.getCanDestroy());
		assertTrue(stomach.getOrganComponent().hasStomach());
		assertEquals(50.0f, stomach.getOrganComponent().getStomach().getChemicalCapacity(), 0.001f);
		for (String organName : new String[] {
				"human brain", "human heart", "human liver", "human lungs", "human stomach"}) {
			Entity organ = assets.loadAsset(organName);
			assertEquals(1.0f / 3.0f, organ.getSize().getX(), 0.001f);
			assertEquals(1.0f / 3.0f, organ.getSize().getY(), 0.001f);
			assertEquals(organ.getName(), organ.getDisplayTexture());
		}
	}
}
