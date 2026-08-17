package simulation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;

public class EntitySimulationTest {

    @Test
    public void movingItemSlowsByConfiguredAmountPerTick() {
        Entity item = itemWithVelocity(3.0f, 4.0f);

        Entity slowed = EntitySimulation.slowItemVelocity(item, 20);

        assertEquals(2.7f, slowed.getVelocity().getX(), 0.0001f);
        assertEquals(3.6f, slowed.getVelocity().getY(), 0.0001f);
    }

    @Test
    public void slowItemStopsInsteadOfReversingDirection() {
        Entity item = itemWithVelocity(0.1f, 0.0f);

        Entity slowed = EntitySimulation.slowItemVelocity(item, 20);

        assertEquals(0.0f, slowed.getVelocity().getX(), 0.0f);
        assertEquals(0.0f, slowed.getVelocity().getY(), 0.0f);
    }

    @Test
    public void anchoredItemDoesNotChangeVelocity() {
        Entity item = itemWithVelocity(3.0f, 4.0f).toBuilder()
                .setAnchored(true)
                .build();

        Entity slowed = EntitySimulation.slowItemVelocity(item, 20);

        assertEquals(item.getVelocity(), slowed.getVelocity());
    }

    private Entity itemWithVelocity(float x, float y) {
        return Entity.newBuilder()
                .setIsItem(true)
                .setVelocity(Vector.newBuilder().setX(x).setY(y).build())
                .build();
    }
}
