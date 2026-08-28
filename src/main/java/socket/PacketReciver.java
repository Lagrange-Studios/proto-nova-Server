package socket;

import java.util.ArrayList;

import action.ActionHandler;
import character.CharacterAppearanceCodec;
import chat.ChatManager;
import entity.EntityFinder;
import entity.EntityManager;
import health.HealthManager;
import main.Console;
import main.Server;
import plane.PlaneManager;
import protonova.protobuf.ActionProto.Action;
import protonova.protobuf.ActionProto.ActionType;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.ChatProto.ChatMessage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import simulation.EntitySimulation;
import simulation.TileMovement;
import util.VectorMath;

public class PacketReciver {

	private EntityManager entityManager;
	private ChatManager chatManager;
	private Console console;
	private ActionHandler actionHandler;
	private EntityFinder entityFinder;
	private HealthManager healthManager;
	private PlaneManager planeManager;
	private final double reconcileCoefficient = 5; // this is very tight could cuase rubber banding in the future
	private static final int MAX_ACTIONS_PER_PACKET = 8;
	private static final int MAX_INTERACTIONS_PER_PACKET = 1;
	private static final int MAX_CHAT_MESSAGES_PER_PACKET = 1;
	private static final int MAX_CHAT_LENGTH = 512;
	private Server server;
	
	public PacketReciver(EntityManager entityManager, ChatManager chatManager, Console console, ActionHandler actionHandler, EntityFinder entityFinder, HealthManager healthManager, PlaneManager planeManager, Server server) {
		this.entityManager = entityManager;
		this.chatManager = chatManager;
		this.console = console;
		this.actionHandler = actionHandler;
		this.entityFinder = entityFinder;
		this.healthManager = healthManager;
		this.planeManager = planeManager;
		this.server = server;
	}
	
	public void recivePacket(Player player, ClientToServerPacket packet) {
		synchronized (entityManager) {
			recivePacketLocked(player, packet);
		}
	}

	private void recivePacketLocked(Player player, ClientToServerPacket packet) {
		int interactionCount = 0;
		for (Action action : packet.getActionsList()) {
			if (action.getActionType() == ActionType.Interact) interactionCount++;
		}
		if (packet.getActionsCount() > MAX_ACTIONS_PER_PACKET
				|| interactionCount > MAX_INTERACTIONS_PER_PACKET
				|| packet.getChatMessageCount() > MAX_CHAT_MESSAGES_PER_PACKET) {
			console.print("WARNING: Rejected an oversized gameplay packet from " + player.getUsername());
			player.disconnect();
			return;
		}

		Entity clientEntity = packet.getUpdatedEntity();
		Entity serverEntity = entityManager.getEntity(player);
		if (serverEntity == null) return;
		float speedMultiplier = TileMovement.getSpeedMultiplier(planeManager.getTileAt(serverEntity));

		String selectedSlot = clientEntity.getSelectedSlot();
		if (selectedSlot.equals("leftHand") || selectedSlot.equals("rightHand")) {
			serverEntity = serverEntity.toBuilder().setSelectedSlot(selectedSlot).build();
		}
		serverEntity = applyCharacterAppearance(player, serverEntity, clientEntity);
		
		if (healthManager.canPerformActions(serverEntity)) {
			for (Action action : packet.getActionsList()) {
				
				if (action.getActionType() != ActionType.Interact) {
					serverEntity = EntitySimulation.simulateMovement(serverEntity, action, speedMultiplier);
				}
				else {
					if (!player.allowInteraction()) continue;
					// so we need the velocity changes persisted first to prevent the re-fetch from getting stale data.
					entityManager.updateEntity(serverEntity);
					serverEntity = actionHandler.executeAction(player, action, serverEntity);
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
		
		for (int i=0;i<packet.getChatMessageCount();i++) {
			if (!player.allowChatMessage()) continue;
			ChatMessage message = packet.getChatMessage(i);
			String text = message.getMessage().trim();
			if (text.isEmpty() || text.length() > MAX_CHAT_LENGTH) continue;
			chatManager.addChatToQueue(message.toBuilder()
					.setMessage(text)
					.setEntityID(serverEntity.getId())
					.setPosition(serverEntity.getPosition())
					.setMap(serverEntity.getMap())
					.setMaxTime(0)
					.setChatID(0)
					.build());
		}

		entityManager.updateEntity(serverEntity);
		
		healthManager.entityCheck(serverEntity);
		
		
		double reconcileDistance = Math.max(0.05,
				(serverEntity.getSpeed() * speedMultiplier / server.CLIENT_TPS) * reconcileCoefficient);
		if (isFinite(clientEntity.getPosition())
				&& VectorMath.distance(clientEntity.getPosition(), serverEntity.getPosition())
						>= reconcileDistance) {
			player.shouldReconcile = true;
			console.print("WARNING: Player "+player.getUsername()+" rubberbanded");
			
		}
	}

	private static boolean isFinite(Vector vector) {
		return vector != null && Float.isFinite(vector.getX()) && Float.isFinite(vector.getY());
	}

	private Entity applyCharacterAppearance(Player player, Entity serverEntity, Entity clientEntity) {
		String update = null;
		for (String tag : clientEntity.getTagsList()) {
			if (!CharacterAppearanceCodec.isAppearanceTag(tag)) continue;
			if (update != null || !CharacterAppearanceCodec.isValidUpdate(tag)) {
				console.print("WARNING: Rejected invalid character appearance from " + player.getUsername());
				return serverEntity;
			}
			update = tag;
		}
		if (update == null) return serverEntity; // Older clients do not change the saved appearance.

		ArrayList<String> preservedTags = new ArrayList<>();
		for (String tag : serverEntity.getTagsList()) {
			if (!CharacterAppearanceCodec.isAppearanceTag(tag)) preservedTags.add(tag);
		}
		Entity.Builder builder = serverEntity.toBuilder().clearTags().addAllTags(preservedTags);
		if (!CharacterAppearanceCodec.DEFAULT.equals(update)) builder.addTags(update);
		return builder.build();
	}
}
