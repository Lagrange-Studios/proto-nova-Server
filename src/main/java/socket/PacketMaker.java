package socket;

import java.util.ArrayList;

import enums.Player.State;

public class PacketMaker {

	ServerSocketHandler serverSocket;
	
	public PacketMaker(ServerSocketHandler serverSocket) {
		this.serverSocket = serverSocket;
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
					
				}
				
				break;
		default:
			break;
		
		}
	}
}
