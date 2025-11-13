package socket;

import java.util.ArrayList;

import entity.EntityManager;
import enums.Player.State;
import file.ServerLoader;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.ServerToClientPacketProto;
import protonova.protobuf.ServerToClientPacketProto.ServerToClientPacket;

public class PacketMaker {

	private ServerSocketHandler serverSocket;
	private ServerLoader serverLoader;
	private EntityManager entityManager;
	
	public PacketMaker(ServerSocketHandler serverSocket, ServerLoader serverLoader, EntityManager entityManager) {
		this.serverSocket = serverSocket;
		this.serverLoader = serverLoader;
		this.entityManager = entityManager;
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
		
		ServerToClientPacket packet = ServerToClientPacket.newBuilder()
				.setPlayerEntity(entityManager.getEntity(player.data.getEntityId()))
				.build();
		
		player.send(packet.toByteArray());
	}
}
