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
	private final double reconcileDistance = 1;
	private Console console;
	private ActionHandler actionHandler;
	private EntityFinder entityFinder;
	private HealthManager healthManager;
	private long lastDebugPrintTime = 0;
	
	public PacketReciver(EntityManager entityManager, SoundManager soundManager, ChatManager chatManager, Console console, ActionHandler actionHandler, EntityFinder entityFinder, HealthManager healthManager) {
		this.entityManager = entityManager;
		this.soundManager = soundManager;
		this.chatManager = chatManager;
		this.console = console;
		this.actionHandler = actionHandler;
		this.entityFinder = entityFinder;
		this.healthManager = healthManager;
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
		
		Entity finalEntity = simulateVelocity(serverEntity);
		entityManager.updateEntity(finalEntity);
		
		healthManager.entityCheck(finalEntity);
		
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastDebugPrintTime >= 1000) {
			Entity updated = entityManager.getEntity(player);
			double totalDamage = healthManager.combatManager.getDamage(updated);
			lastDebugPrintTime = currentTime;
		}
		
		if (VectorMath.distance(clientEntity.getPosition(), serverEntity.getPosition()) >= reconcileDistance) {
			player.shouldReconcile = true;
			console.print("WARNING: Player "+player.getUsername()+" is moving too fast!");
			
		}
	}
	
	private Entity simulateVelocity(Entity entity) {
		
		ArrayList<Entity> closeEntities = entityFinder.getAllEntitiesInRadis(entity, 10);
		
		Entity entityXAxis = checkCollision(EntitySimulation.simulateVelocityXAxis(entity),entity,closeEntities);
		
		// check if we actualy did anything
		if (entityXAxis.getPosition().equals(entity.getPosition())) {
			// if not then just remove the veloicty
			
			Vector newVeloicty = entity.getVelocity().toBuilder()
					.setX(0)
					.build();
			
			entity = entity.toBuilder()
					.setVelocity(newVeloicty)
					.build();
		}
		else entity = entityXAxis;
		
		Entity entityYAxis = checkCollision(EntitySimulation.simulateVelocityYAxis(entity),entity,closeEntities);
		
		// check if we actualy did anything
		if (entityYAxis.getPosition().equals(entity.getPosition())) {
			// if not then just remove the veloicty
			
			Vector newVeloicty = entity.getVelocity().toBuilder()
					.setY(0)
					.build();
			
			entity = entity.toBuilder()
					.setVelocity(newVeloicty)
					.build();
		}
		else entity = entityYAxis;

		return entity;
	}
	
	private Entity checkCollision(Entity updatedEntity, Entity originalEntity, ArrayList<Entity> closeEntities) {
		if (originalEntity.getPosition().equals(updatedEntity.getPosition())) return originalEntity;
		
		for (Entity entity : closeEntities) {
			if (entity.getId() != originalEntity.getId()) {
				//if (EntityCollision.checkCollision(updatedEntity, entity)) System.out.println(true);
				if (entity.getCanCollide() && EntityCollision.checkCollision(updatedEntity, entity)) return originalEntity;
			}
		}
		
		return updatedEntity;
	}
}
