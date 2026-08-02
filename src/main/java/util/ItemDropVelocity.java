package util;

import java.util.concurrent.ThreadLocalRandom;

import protonova.protobuf.VectorProto.Vector;

public final class ItemDropVelocity {

	private static final float MINIMUM_SPEED = 1.5f;
	private static final float MAXIMUM_SPEED = 3.0f;

	private ItemDropVelocity() {
	}

	public static Vector createRandomScatterVelocity() {
		double direction = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
		float speed = ThreadLocalRandom.current().nextFloat(MINIMUM_SPEED, MAXIMUM_SPEED);
		float horizontalVelocity = (float) Math.cos(direction) * speed;
		float verticalVelocity = (float) Math.sin(direction) * speed;

		return Vector.newBuilder()
				.setX(horizontalVelocity)
				.setY(verticalVelocity)
				.build();
	}
}
