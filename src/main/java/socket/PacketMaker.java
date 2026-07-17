package socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import chat.ChatFinder;
import entity.EntityFinder;
import entity.EntityManager;
import enums.Player.State;
import file.ServerLoader;
import file.ServerSaver;
import plane.PlaneManager;
import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.ChatProto.ChatMessage;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.ServerToClientPacketProto.ServerToClientPacket;
import protonova.protobuf.ServerToClientPacketProto.ServerToClientPacket.Builder;
import sound.SoundFinder;
import space.CelestialObjectManager;
import util.VectorMath;
import diagnostics.ResourceDiagnostics;

public class PacketMaker {

	private ServerSocketHandler serverSocket;
	private ServerLoader serverLoader;
	private ServerSaver serverSaver;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private SoundFinder soundFinder;
	private ChatFinder chatFinder;
	private PlaneManager planeManager;
	private CelestialObjectManager celestialObjectManager;
	private ResourceDiagnostics diagnostics;
	
	private static final double renderDistance = 40;
	private static final double renderDistanceSquared = Math.pow(renderDistance, 2);
	private static final int TILE_RENDER_X = 30;
	private static final int TILE_RENDER_Y = 20;
	
	public PacketMaker(ServerSocketHandler serverSocket, ServerLoader serverLoader, ServerSaver serverSaver,
			EntityManager entityManager, EntityFinder entityFinder, SoundFinder soundFinder, ChatFinder chatFinder, PlaneManager planeManager, CelestialObjectManager celestialObjectManager,
			ResourceDiagnostics diagnostics) {
		this.serverSocket = serverSocket;
		this.serverLoader = serverLoader;
		this.serverSaver = serverSaver;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.soundFinder = soundFinder;
		this.chatFinder = chatFinder;
		this.planeManager = planeManager;
		this.celestialObjectManager = celestialObjectManager;
		this.diagnostics = diagnostics;
	}
	
	public void sendPacket(Player player) {
		switch(player.getState()) {
			case AWAITING_SERVER_PACKET:
				
				ArrayList<Player> players = serverSocket.getPlayerList();
				
				// Checking for duplicate players
				for (int i=0;i<players.size();i++) {
					if (players.get(i) != player && players.get(i).getUsername().equals(player.getUsername())) {
						player.disconnect();
						break;
						// TODO: add a disconnection reason
					}
				}
				
				if (player.getState() != State.DISCONNECTED) {
					// Only load and initialize player data once on first connection
					if (player.data == null) {
						player.data = serverLoader.getPlayerData(player.getUsername());
						boolean isNewPlayer = player.data.getEntityId() == 0;
						
						// If player data was loaded and has a valid entity ID, check if entity still exists
						if (!isNewPlayer && entityManager.getEntity(player.data.getEntityId()) == null) {
							// Entity doesn't exist but player data exists - recreate the entity
							// This handles the case where entities were cleared but player data persists
							Entity newEntity = entityManager.makeNewEntity("human");
							player.data = player.data.toBuilder()
								.setEntityId(newEntity.getId())
								.build();
						} else if (isNewPlayer) {
							// This is a new player, create their first character
							Entity newEntity = entityManager.makeNewEntity("human");
							player.data = player.data.toBuilder()
								.setEntityId(newEntity.getId())
								.build();
						}
						// If player data exists and entity exists, just reuse it (player is reconnecting)
						
						// Save player data after initialization
						if (serverSaver != null) {
							serverSaver.savePlayer(player);
						}
					}
					
					sendNormalPacket(player);
					player.setState(State.PLAYING);
					
				}
				
				break;
			case AWAITING_CLIENT_PACKET:
				break;
		default:
			sendNormalPacket(player);
			break;
		
		}
	}
	
	@SuppressWarnings("unchecked")
	private void sendNormalPacket(Player player) {
		
		Entity playerEntity = entityManager.getEntity(player.data.getEntityId());
		
		Plane currentPlane = planeManager.getPlane(playerEntity);
		
		Builder packet = ServerToClientPacket.newBuilder()
				.setMindId(player.data.getEntityId());
		
		// clone the delete and update list so we only remove the updates we actualy sent
		// this is a possible fix for desyncs but could also just use more cpu and ram for no reason
		// further testing required
		HashSet<Integer> updateList = new HashSet<>(player.updateList);
		HashSet<Integer> deleteList = new HashSet<>(player.deleteList);
		
		int startX = Math.round(playerEntity.getPosition().getX());
		int startY = Math.round(playerEntity.getPosition().getY());
		
		// Use StringBuilder to reduce string allocation overhead
		StringBuilder keyBuilder = new StringBuilder();
		
		for (int x=-TILE_RENDER_X;x<=TILE_RENDER_X;x++) {
			for (int y=-TILE_RENDER_Y;y<=TILE_RENDER_Y;y++) {
				int planeX = startX + x;
				int planeY = startY + y;
				
				// Use StringBuilder to create key efficiently
				keyBuilder.setLength(0);
				keyBuilder.append(planeX).append(',').append(planeY);
				String key = keyBuilder.toString();
				
				if (currentPlane.getTilesMap().containsKey(key)) {
					packet.putTiles(key, currentPlane.getTilesMap().get(key));
				}
			}
		}
		
		HashSet<Integer> entitiesSent = player.entitiesSent;
		
		HashSet<Integer> entitiesSentThisPacket = new HashSet<>();
		
		// add nearby entities
		ArrayList<Entity> foundEntities = entityFinder.getAllEntitiesInRadis(playerEntity, renderDistance);
		
		for (Entity entity : foundEntities) {
			if (entity != null && !entitiesSent.contains(entity.getId())) {
				packet.addEntities(entity);
				diagnostics.recordEntityNetwork(entity.getId(), entity.getSerializedSize());
				entitiesSentThisPacket.add(entity.getId());
			}
		}
		
		// Add inventory
		for (int id : playerEntity.getInventorySlotsMap().values()) {
			Entity inventoryItem = entityManager.getEntity(id);
			if (inventoryItem != null && (!entitiesSent.contains(inventoryItem.getId())
				|| updateList.contains(id))) {
				packet.addEntities(inventoryItem);
				diagnostics.recordEntityNetwork(inventoryItem.getId(), inventoryItem.getSerializedSize());
				entitiesSentThisPacket.add(inventoryItem.getId());
			}
			else if (deleteList.contains(id)) packet.addRemovedEntities(id);
		}
		
		//check for updates
		for (int id : entitiesSent.toArray(new Integer[0])) {
			if (deleteList.contains(id) ||
				entityManager.getEntity(id) == null ||
				VectorMath.distanceSquared(playerEntity.getPosition(), entityManager.getEntity(id).getPosition()) > renderDistanceSquared &&
				!playerEntity.getInventorySlotsMap().containsValue(id)) {
				
				entitiesSent.remove(id);
				packet.addRemovedEntities(id);
				if (entitiesSentThisPacket.contains(id)) entitiesSentThisPacket.remove(id);
			}
			else if (updateList.contains(id)) {
				Entity updatedEntity = entityManager.getEntity(id);
				packet.addEntities(updatedEntity);
				diagnostics.recordEntityNetwork(updatedEntity.getId(), updatedEntity.getSerializedSize());
				entitiesSentThisPacket.add(id);
			}
		}

		ArrayList<Audio> foundSounds = soundFinder.getAllSoundsInRadis(playerEntity, renderDistance);
		
		for (int i=0;i<foundSounds.size();i++) {
			packet.addSounds(foundSounds.get(i));
		}
		
		ArrayList<ChatMessage> foundChats = chatFinder.getAllChatsInRadis(playerEntity, renderDistance);
		
		for (int i=0;i<foundChats.size();i++) {
			packet.addChatMessage(foundChats.get(i));
		}
		
		packet.setCurrentCelestialObject(celestialObjectManager.getCelestialObjectFromPlane(currentPlane));
		
		ArrayList<String> messages = new ArrayList<>(player.messageList);
		
		// add any server messages for this player
		packet.addAllServerMessages(messages);
		
		packet.setReconcile(player.shouldReconcile);
		
		player.send(packet.build().toByteArray());
		
		// Add all entities sent this packet to entitiesSent so updates are tracked next tick
		entitiesSent.addAll(entitiesSentThisPacket);
		
		
		// clear only the entities updates and deletes we cloned since its linked values
		deleteLikeValues(deleteList,player.deleteList);
		deleteLikeValues(updateList,player.updateList);
		deleteLikeValues(messages,player.messageList);
	}
	
	private void deleteLikeValues(HashSet<?> set1, Set<?> set2) {
		for (Object value : set1) {
			set2.remove(value);
		}
	}
	
	private void deleteLikeValues(ArrayList<?> list1, ArrayList<?> list2) {
		for (Object value : list1) {
			list2.remove(value);
		}
	}
}
