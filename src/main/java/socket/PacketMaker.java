package socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import entity.ChunkManager;
import entity.EntityManager;
import enums.Player.State;
import file.ServerLoader;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.ServerToClientPacketProto.ServerToClientPacket;
import protonova.protobuf.ServerToClientPacketProto.ServerToClientPacket.Builder;
import protonova.protobuf.TileProto.Tile;

public class PacketMaker {

	private ServerSocketHandler serverSocket;
	private ServerLoader serverLoader;
	private EntityManager entityManager;
	private HashMap<Integer,Plane> planes;
	private ChunkManager chunkManager;
	private static final double renderDistance = 40;
	
	public PacketMaker(ServerSocketHandler serverSocket, ServerLoader serverLoader,
			EntityManager entityManager, HashMap<Integer,Plane> planes, ChunkManager chunkManager) {
		this.serverSocket = serverSocket;
		this.serverLoader = serverLoader;
		this.entityManager = entityManager;
		this.planes = planes;
		this.chunkManager = chunkManager;
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
					
					if (player.data.getEntityId() == 0) {
						Entity newEntity = entityManager.makeNewEntity("human");

						player.data = player.data.toBuilder()
							.setEntityId(newEntity.getId())
							.build();
						
						sendNormalPacket(player);
						player.setState(State.PLAYING);
					}
					
					//TODO: send the player their entity
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
		
		Plane currentPlane = planes.get(playerEntity.getMap());
		
		Builder packet = ServerToClientPacket.newBuilder()
				.setPlayerEntity(playerEntity);
		
		int startX = Math.round(playerEntity.getPosition().getX());
		int startY = Math.round(playerEntity.getPosition().getY());
		
		for (int x=-20;x<=20;x++) {
			for (int y=-10;y<=10;y++) {
				int planeX = startX + x;
				int planeY = startY + y;
				String key = planeX+","+planeY;
				
				if (currentPlane.getTilesMap().containsKey(key)) {
					packet.putTiles(key, currentPlane.getTilesMap().get(key));
				}
			}
		}
		
		ArrayList<Entity> foundEntities;
		
		player.send(packet.build().toByteArray());
	}
}
