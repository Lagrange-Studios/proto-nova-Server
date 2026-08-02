package util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import protonova.protobuf.VectorProto.Vector;
import util.VectorMath;

public class ItemDropVelocityTest {

	@Test
	public void automaticDropsUseASmallScatterSpeed() {
		for (int attempt = 0; attempt < 100; attempt++) {
			Vector scatterVelocity = ItemDropVelocity.createRandomScatterVelocity();
			double scatterSpeed = VectorMath.magnitude(scatterVelocity);

			assertTrue(scatterSpeed >= 1.499);
			assertTrue(scatterSpeed < 3.001);
		}
	}
}
