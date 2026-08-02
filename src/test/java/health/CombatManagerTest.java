package health;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import protonova.protobuf.EntityProto.Entity;

public class CombatManagerTest {

	@Test
	public void indestructibleItemCannotReceiveAttack() {
		Entity item = Entity.newBuilder()
				.setIsItem(true)
				.setCanDestroy(false)
				.build();

		assertFalse(CombatManager.canReceiveAttack(item));
	}

	@Test
	public void destructibleItemCanReceiveAttack() {
		Entity item = Entity.newBuilder()
				.setIsItem(true)
				.setCanDestroy(true)
				.build();

		assertTrue(CombatManager.canReceiveAttack(item));
	}

	@Test
	public void nonItemIgnoresItemDestructionFlag() {
		Entity creature = Entity.newBuilder()
				.setIsItem(false)
				.setCanDestroy(false)
				.build();

		assertTrue(CombatManager.canReceiveAttack(creature));
	}
}
