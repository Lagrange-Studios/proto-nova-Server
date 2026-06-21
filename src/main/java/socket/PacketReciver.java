package socket;

import java.util.ArrayList;

import action.ActionHandler;
import chat.ChatManager;
import collision.EntityCollision;
import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import health.HealthManager;
import main.Console;
import main.Server;
import protonova.protobuf.ActionProto.Action;
import protonova.protobuf.ActionProto.ActionType;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import simulation.EntitySimulation;
import sound.SoundManager;
import util.VectorMath;

public class PacketReciver {

	private EntityManager entityManager;
	private SoundManager soundManager;
	private ChatManager chatManager;
	private Console console;
	private ActionHandler actionHandler;
	private EntityFinder entityFinder;
	private HealthManager healthManager;
	private final double reconcileCoefficient = 10;
	private long lastDebugPrintTime = 0;
	private Server server;
	
	public PacketReciver(EntityManager entityManager, SoundManager soundManager, ChatManager chatManager, Console console, ActionHandler actionHandler, EntityFinder entityFinder, HealthManager healthManager, Server server) {
		this.entityManager = entityManager;
		this.soundManager = soundManager;
		this.chatManager = chatManager;
		this.console = console;
		this.actionHandler = actionHandler;
		this.entityFinder = entityFinder;
		this.healthManager = healthManager;
		this.server = server;
	}
	
	public void recivePacket(Player player, ClientToServerPacket packet) {
		
		Entity clientEntity = packet.getUpdatedEntity();
		Entity serverEntity = entityManager.getEntity(player);
		
		serverEntity = serverEntity.toBuilder()
				.setSelectedSlot(clientEntity.getSelectedSlot())
				.build();
		
		if (!healthManager.checkCrit(serverEntity)) {
			for (Action action : packet.getActionsList()) {
				
				if (action.getActionType() != ActionType.Interact) {
					serverEntity = EntitySimulation.simulateMovement(serverEntity, action);
				}
				else {
					serverEntity = actionHandler.executeAction(player, action);
				}
			}
		} else {
			Vector clearedVelocity = serverEntity.getVelocity().toBuilder()
					.setX(0)
					.setY(0)
					.build();
			serverEntity = serverEntity.toBuilder()
					.setVelocity(clearedVelocity)
					.build();
		}
		
		for (int i=0;i<packet.getSoundsCount();i++) {
			soundManager.addSoundToQueue(packet.getSounds(i));
		}
		
		for (int i=0;i<packet.getChatMessageCount();i++) {
			chatManager.addChatToQueue(packet.getChatMessage(i));
		}
		
		entityManager.updateEntity(serverEntity);
		
		healthManager.entityCheck(serverEntity);
		
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastDebugPrintTime >= 1000) {
			Entity updated = entityManager.getEntity(player);
			double totalDamage = healthManager.combatManager.getDamage(updated);
			lastDebugPrintTime = currentTime;
		}
		
		if (VectorMath.distance(clientEntity.getPosition(), serverEntity.getPosition()) >= (serverEntity.getSpeed()/server.TPS)*reconcileCoefficient) {
			player.shouldReconcile = true;
			console.print("WARNING: Player "+player.getUsername()+" is moving too fast!");
			
		}
	}
}
