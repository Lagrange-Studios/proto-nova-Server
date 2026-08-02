package health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.EntityProto.Entity;

public class HealthManagerTest {

	private static final double DELTA = 0.0001;
	private final HealthManager health = new HealthManager(null, null, null);

	@Test
	public void fatalSpawnDamageCreatesANonMovingCorpse() {
		Entity entity = entityWithDamage(100, 50, 120, true, 8);

		Entity checked = health.prepareEntityState(entity);

		assertFalse(checked.getAlive());
		assertEquals(0, checked.getSpeed(), DELTA);
	}

	@Test
	public void healthySpawnKeepsItsFullSpeed() {
		Entity entity = entityWithDamage(100, 50, 0, true, 8);

		Entity checked = health.prepareEntityState(entity);

		assertTrue(checked.getAlive());
		assertEquals(8, checked.getSpeed(), DELTA);
	}

	@Test
	public void injuredSpawnStartsAtReducedSpeed() {
		Entity entity = entityWithDamage(200, 100, 45, true, 10);

		Entity checked = health.prepareEntityState(entity);

		assertTrue(checked.getAlive());
		assertEquals(7.5, checked.getSpeed(), DELTA);
	}

	@Test
	public void validationDoesNotReviveAnExistingCorpse() {
		Entity entity = entityWithDamage(100, 50, 0, false, 8);

		Entity checked = health.prepareEntityState(entity);

		assertFalse(checked.getAlive());
		assertEquals(0, checked.getSpeed(), DELTA);
	}

	@Test
	public void itemsThatCannotBeDestroyedAreNotTreatedAsDead() {
		Entity item = Entity.newBuilder()
				.setIsItem(true)
				.setCanDestroy(false)
				.setMaxHealth(0)
				.build();

		assertFalse(health.checkDeath(item));
		assertFalse(health.checkCrit(item));
	}

	@Test
	public void destroyableItemsStillUseTheirHealth() {
		Entity item = Entity.newBuilder()
				.setIsItem(true)
				.setCanDestroy(true)
				.setMaxHealth(5)
				.setDamage(Damage.newBuilder().setBruteDamage(5))
				.build();

		assertTrue(health.checkDeath(item));
	}

	@Test
	public void destroyableItemsWithoutHealthAreNotRemovedOnSpawn() {
		Entity item = Entity.newBuilder()
				.setIsItem(true)
				.setCanDestroy(true)
				.setMaxHealth(0)
				.build();

		assertFalse(health.checkDeath(item));
		assertFalse(health.checkCrit(item));
	}

	@Test
	public void assetsWithoutHealthAreNotRemovedOnSpawn() {
		Entity asset = Entity.newBuilder()
				.setAlive(false)
				.setMaxHealth(0)
				.setMaxSpeed(4)
				.setSpeed(2)
				.build();

		Entity checked = health.prepareEntityState(asset);

		assertFalse(health.checkDeath(checked));
		assertFalse(health.checkCrit(checked));
		assertEquals(2, checked.getSpeed(), DELTA);
	}

	private static Entity entityWithDamage(int maxHealth, int criticalDamage,
			float bruteDamage, boolean alive, double maximumSpeed) {
		return Entity.newBuilder()
				.setMaxHealth(maxHealth)
				.setCritHealth(criticalDamage)
				.setAlive(alive)
				.setMaxSpeed(maximumSpeed)
				.setSpeed(maximumSpeed)
				.setDamage(Damage.newBuilder().setBruteDamage(bruteDamage))
				.build();
	}
}
