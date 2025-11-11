package socket;

import java.util.ArrayList;

import enums.Player.State;
import file.ServerLoader;

public class PacketMaker {

	private ServerSocketHandler serverSocket;
	private ServerLoader serverLoader;
	
	public PacketMaker(ServerSocketHandler serverSocket, ServerLoader serverLoader) {
		this.serverSocket = serverSocket;
		this.serverLoader = serverLoader;
	}
	
	public void SendPacket(Player player) {
		
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
					//TODO: send the player their entity
				}
				
				break;
		default:
			break;
		
		}
	}
}
