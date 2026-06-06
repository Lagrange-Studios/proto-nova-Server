package socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import chat.ChatFinder;
import chat.ChatManager;
import entity.ChunkManager;
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
import protonova.protobuf.TileProto.Tile;
import sound.SoundFinder;
import space.CelestialObjectManager;
import util.VectorMath;

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
	
	private static final double renderDistance = 40;
	private static final double renderDistanceSquared = Math.pow(renderDistance, 2);
	private static final int TILE_RENDER_X = 30;
	private static final int TILE_RENDER_Y = 20;
	
	public PacketMaker(ServerSocketHandler serverSocket, ServerLoader serverLoader, ServerSaver serverSaver,
			EntityManager entityManager, EntityFinder entityFinder, SoundFinder soundFinder, ChatFinder chatFinder, PlaneManager planeManager, CelestialObjectManager celestialObjectManager) {
		this.serverSocket = serverSocket;
		this.serverLoader = serverLoader;
		this.serverSaver = serverSaver;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.soundFinder = soundFinder;
		this.chatFinder = chatFinder;
		this.planeManager = planeManager;
		this.celestialObjectManager = celestialObjectManager;
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
						
						System.out.println("[PacketMaker] Player: " + player.getUsername() + 
							" | isNewPlayer: " + isNewPlayer + 
							" | savedEntityId: " + player.data.getEntityId());
						
						// If player data was loaded and has a valid entity ID, check if entity still exists
						if (!isNewPlayer && entityManager.getEntity(player.data.getEntityId()) == null) {
							// Entity doesn't exist but player data exists - recreate the entity
							// This handles the case where entities were cleared but player data persists
							System.out.println("[PacketMaker] Recreating lost entity for " + player.getUsername());
							Entity newEntity = entityManager.makeNewEntity("human");
							player.data = player.data.toBuilder()
								.setEntityId(newEntity.getId())
								.build();
						} else if (isNewPlayer) {
							// This is a new player, create their first character
							System.out.println("[PacketMaker] Creating new character for " + player.getUsername());
							Entity newEntity = entityManager.makeNewEntity("human");
							player.data = player.data.toBuilder()
								.setEntityId(newEntity.getId())
								.build();
						} else {
							// Player is reconnecting with existing entity
							System.out.println("[PacketMaker] Restoring existing entity #" + player.data.getEntityId() + 
								" for returning player " + player.getUsername());
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
	
	private void sendNormalPacket(Player player) {
		
		Entity playerEntity = entityManager.getEntity(player.data.getEntityId());
		
		Plane currentPlane = planeManager.getPlane(playerEntity);
		
		Builder packet = ServerToClientPacket.newBuilder()
				.setMindId(player.data.getEntityId());
		
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
				entitiesSentThisPacket.add(entity.getId());
			}
		}
		
		// Add inventory
		for (int id : playerEntity.getInventorySlotsMap().values()) {
			Entity inventoryItem = entityManager.getEntity(id);
			if (inventoryItem != null && !entitiesSent.contains(inventoryItem.getId())) {
				packet.addEntities(inventoryItem);
				entitiesSentThisPacket.add(inventoryItem.getId());
			}
		}
		
		//check for updates
		for (int id : entitiesSent.toArray(new Integer[0])) {
			if (player.deleteList.contains(id) ||
				entityManager.getEntity(id) == null ||
				VectorMath.distanceSquared(playerEntity.getPosition(), entityManager.getEntity(id).getPosition()) > renderDistanceSquared) {
				
				entitiesSent.remove(id);
				packet.addRemovedEntities(id);
				if (entitiesSentThisPacket.contains(id)) entitiesSentThisPacket.remove(id);
			}
			else if (player.updateList.contains(id)) {
				packet.addEntities(entityManager.getEntity(id));
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
		
		packet.setReconcile(player.shouldReconcile);
		
		player.send(packet.build().toByteArray());
		
		// Add all entities sent this packet to entitiesSent so updates are tracked next tick
		entitiesSent.addAll(entitiesSentThisPacket);
		
		player.deleteList.clear();
		player.updateList.clear();
	}
}
