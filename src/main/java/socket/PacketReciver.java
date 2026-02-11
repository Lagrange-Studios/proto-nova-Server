package socket;

import action.ActionHandler;
import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import main.Console;
import protonova.protobuf.ActionProto.Action;
import protonova.protobuf.ActionProto.ActionType;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.EntityProto.Entity;
import simulation.EntitySimulation;
import sound.SoundManager;
import util.VectorMath;

public class PacketReciver {

	private EntityManager entityManager;
	private SoundManager soundManager;
	private final double reconcileDistance = 1; // nessecary distance to reconcile
	private Console console;
	private ActionHandler actionHandler;
	
public PacketReciver(EntityManager entityManager, SoundManager soundManager, Console console, ActionHandler actionHandler) {
		this.entityManager = entityManager;
		this.soundManager = soundManager;
		this.console = console;
		this.actionHandler = actionHandler;
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
		for (Action action : packet.getActionsList()) {
			
			if (action.getActionType() != ActionType.Interact) {
				serverEntity = EntitySimulation.simulateMovement(serverEntity, action);
			}
			else {
				serverEntity = actionHandler.executeAction(player, action);
			}
		}
		
		serverEntity = serverEntity.toBuilder().clearChatMessage().addAllChatMessage(clientEntity.getChatMessageList()).build();
		
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
