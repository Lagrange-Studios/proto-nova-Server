package socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import enums.Player.State;
import file.ServerLoader;
import plane.PlaneManager;
import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.ServerToClientPacketProto.ServerToClientPacket;
import protonova.protobuf.ServerToClientPacketProto.ServerToClientPacket.Builder;
import protonova.protobuf.TileProto.Tile;
import sound.SoundFinder;

public class PacketMaker {

	private ServerSocketHandler serverSocket;
	private ServerLoader serverLoader;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private SoundFinder soundFinder;
	private PlaneManager planeManager;
	
	private static final double renderDistance = 40;
	
	public PacketMaker(ServerSocketHandler serverSocket, ServerLoader serverLoader,
			EntityManager entityManager, EntityFinder entityFinder, SoundFinder soundFinder, PlaneManager planeManager) {
		this.serverSocket = serverSocket;
		this.serverLoader = serverLoader;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.soundFinder = soundFinder;
		this.planeManager = planeManager;
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
					player.data = serverLoader.getPlayerData(player.getUsername());
					
					if (player.data.getEntityId() == 0 || entityManager.getEntity(player.data.getEntityId()) == null) {
						Entity newEntity = entityManager.makeNewEntity("human");
						
						player.data = player.data.toBuilder()
							.setEntityId(newEntity.getId())
							.build();
						
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
		
		for (int x=-30;x<=30;x++) {
			for (int y=-20;y<=20;y++) {
				int planeX = startX + x;
				int planeY = startY + y;
				String key = planeX+","+planeY;
				
				if (currentPlane.getTilesMap().containsKey(key)) {
					packet.putTiles(key, currentPlane.getTilesMap().get(key));
				}
			}
		}
		
		// add nearby entities
		ArrayList<Entity> foundEntities = entityFinder.getAllEntitiesInRadis(playerEntity, renderDistance);
		
		for (Entity entity : foundEntities) {
			packet.addEntities(entity);
		}
		
		// Add inventory
		for (int id : playerEntity.getInventorySlotsMap().values()) {
			packet.addEntities(entityManager.getEntity(id));
		}

		ArrayList<Audio> foundSounds = soundFinder.getAllSoundsInRadis(playerEntity, renderDistance);
		
		for (int i=0;i<foundSounds.size();i++) {
			packet.addSounds(foundSounds.get(i));
		}
		
		packet.setReconcile(player.shouldReconcile);
		
		player.send(packet.build().toByteArray());
	}
}
