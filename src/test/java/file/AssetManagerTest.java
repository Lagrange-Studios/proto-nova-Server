package file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Entity;

public class AssetManagerTest {

	@Test
	public void spawningAnEntityAlwaysResetsItsAttackCooldown() {
		Entity asset = Entity.newBuilder()
				.setHitDamage(HitDamage.newBuilder().setCanAttack(false))
				.build();

		Entity spawnedEntity = AssetManager.prepareEntityForSpawn(asset, 42, 3);

		assertEquals(42, spawnedEntity.getId());
		assertEquals(3, spawnedEntity.getMap());
		assertTrue(spawnedEntity.getHitDamage().getCanAttack());
	}
}
