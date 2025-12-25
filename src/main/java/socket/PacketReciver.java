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
	private final int  TPS = 60;
	private final double reconcileDistance = 0.5; // nessecary distance to reconcile
	
	public PacketReciver(EntityFinder entityFinder, ChunkManager chunkManager, EntityManager entityManager) {
		this.entityFinder = entityFinder;
		this.chunkManager = chunkManager;
		this.entityManager = entityManager;
	}
	
	public void recivePacket(Player player, ClientToServerPacket packet) {
		
		// simulate the entity serverside
		for (int i=0;i<packet.getActionsCount();i++) {
			entityManager.updateEntity(EntitySimulation.simulateMovement(entityManager.getEntity(player), TPS, packet.getActions(i)));
		}
		
		if (VectorMath.distance(packet.getUpdatedEntity().getPosition(), entityManager.getEntity(player).getPosition()) >= reconcileDistance) {
			player.shouldReconcile = true;
		}
	}
}
