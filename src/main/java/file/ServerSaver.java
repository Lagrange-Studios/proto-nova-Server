package file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import entity.EntityManager;
import main.Server;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import socket.Player;

public class ServerSaver {

	private Server server;
	private EntityManager entityManager;
	private HashMap<Integer, Plane> planes;
	
	public ServerSaver(Server server, EntityManager entityManager, HashMap<Integer, Plane> planes) {
		this.server = server;
		this.entityManager = entityManager;
		this.planes = planes;
	}
	
	public String save() {

		// Saving Players
		ArrayList<Player> players = server.getPlayers();
		
		for (int i=0;i<players.size();i++) {
			savePlayer(players.get(i));
		}
		
		// Saving entities
		HashMap<Integer, Entity> entities = entityManager.getAllEntities();
		
		try {
			Set<Integer> keys = entities.keySet();
			Iterator<Integer> iterator = keys.iterator();
			
			while (iterator.hasNext()) {
				int key = iterator.next();
				
				Entity entity = entities.get(key);
				
				byte[] array = entity.toByteArray();
				Path newPath = Paths.get("worldRoot/entities/"+entity.getId()+".data");
				
				Files.write(newPath, array);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		//Saving planes
		try {
			Set<Integer> keys = planes.keySet();
			Iterator<Integer> iterator = keys.iterator();
			
			while (iterator.hasNext()) {
				int key = iterator.next();
				
				Plane plane = planes.get(key);
				
				byte[] array = plane.toByteArray();
				Path newPath = Paths.get("worldRoot/planes/"+plane.getId()+".data");
				
				Files.write(newPath, array);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		return "Saved all data without errors";
	}
	
	public void savePlayer(Player player) {
		if (player.data != null) {
			Path path = Paths.get("worldRoot/playerData/"+player.getUsername()+".data");
			byte[] data = player.data.toByteArray();
			
			try {
				Files.write(path, data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
