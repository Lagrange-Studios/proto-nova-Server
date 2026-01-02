package socket;

import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.EntityProto.Entity;
import simulation.EntitySimulation;
import util.VectorMath;

public class PacketReciver {

	private EntityFinder entityFinder;
	private ChunkManager chunkManager;
	private EntityManager entityManager;
	private final double reconcileDistance = 1; // nessecary distance to reconcile
	
	public PacketReciver(EntityFinder entityFinder, ChunkManager chunkManager, EntityManager entityManager) {
		this.entityFinder = entityFinder;
		this.chunkManager = chunkManager;
		this.entityManager = entityManager;
	}
	
	public void recivePacket(Player player, ClientToServerPacket packet) {
		
		// simulate the entity serverside
		
		// simulate keyPresses
		for (int i=0;i<packet.getActionsCount();i++) {
			entityManager.updateEntity(EntitySimulation.simulateMovement(entityManager.getEntity(player), packet.getActions(i)));
		}
		
		// Simulate final velocity
		entityManager.updateEntity(EntitySimulation.simulateVelocity(entityManager.getEntity(player)));
		
		if (VectorMath.distance(packet.getUpdatedEntity().getPosition(), entityManager.getEntity(player).getPosition()) >= reconcileDistance) {
			player.shouldReconcile = true;
		}
	}
}
