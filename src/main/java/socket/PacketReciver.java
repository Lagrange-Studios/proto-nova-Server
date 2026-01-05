package socket;

import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import main.Console;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.EntityProto.Entity;
import simulation.EntitySimulation;
import sound.SoundManager;
import util.VectorMath;

public class PacketReciver {

	private EntityFinder entityFinder;
	private ChunkManager chunkManager;
	private EntityManager entityManager;
	private SoundManager soundManager;
	private final double reconcileDistance = 1; // nessecary distance to reconcile
	private Console console;
	

public PacketReciver(EntityFinder entityFinder, ChunkManager chunkManager, EntityManager entityManager, SoundManager soundManager, Console console) {
		this.entityFinder = entityFinder;
		this.chunkManager = chunkManager;
		this.entityManager = entityManager;
		this.soundManager = soundManager;
		this.console = console;
	}
	
	public void recivePacket(Player player, ClientToServerPacket packet) {
		
		Entity clientEntity = packet.getUpdatedEntity();
		Entity serverEntity = entityManager.getEntity(player);
		// simulate the entity serverside
		
		// update all the client trusted values
		serverEntity = serverEntity.toBuilder()
				.setSelectedSlot(clientEntity.getSelectedSlot())
				.build();
		
		// simulate keyPresses
		for (int i=0;i<packet.getActionsCount();i++) {
			serverEntity = EntitySimulation.simulateMovement(serverEntity, packet.getActions(i));
		}
		
		for (int i=0;i<packet.getSoundsCount();i++) {
			soundManager.makeNewSound(packet.getSounds(i));
		}
		
		// Simulate final velocity
		entityManager.updateEntity(EntitySimulation.simulateVelocity(serverEntity));
		
		if (VectorMath.distance(clientEntity.getPosition(), serverEntity.getPosition()) >= reconcileDistance) {
			player.shouldReconcile = true;
			console.print("WARNING: Player "+player.getUsername()+" is moving too fast!");
			
		}
	}
}
