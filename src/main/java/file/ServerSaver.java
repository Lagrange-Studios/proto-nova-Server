package file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import main.Server;
import socket.Player;

public class ServerSaver {

	private Server server;
	
	public ServerSaver(Server server) {
		this.server = server;
	}
	
	public String save() {

		ArrayList<Player> players = server.getPlayers();
		
		
		try {
			
			for (int i=0;i<players.size();i++) {
				savePlayer(players.get(i));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		return "Saved all data without errors";
	}
	
	public void savePlayer(Player player) throws IOException {
		if (player.data != null) {
			Path path = Paths.get("worldRoot/playerData/"+player.getUsername()+".data");
			byte[] data = player.data.toByteArray();
			
			Files.write(path, data);
		}
	}
}
