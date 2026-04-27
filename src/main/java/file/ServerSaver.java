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
import plane.PlaneManager;
import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.EntityDataProto.EntityData;
import protonova.protobuf.EntityDataProto.EntityData.Builder;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import socket.Player;
import space.CelestialObjectManager;

public class ServerSaver {

	private Server server;
	private EntityManager entityManager;
	private PlaneManager planeManager;
	private CelestialObjectManager celestialObjectManager;
	
	public ServerSaver(Server server, EntityManager entityManager, PlaneManager planeManager, CelestialObjectManager celestialObjectManager) {
		this.server = server;
		this.entityManager = entityManager;
		this.planeManager = planeManager;
		this.celestialObjectManager = celestialObjectManager;
	}
	
	public String save() {

		// Saving Players
		ArrayList<Player> players = server.getPlayers();
		
		for (int i=0;i<players.size();i++) {
			savePlayer(players.get(i));
		}
		
		// Saving entities
		try {
			Builder entityData = EntityData.newBuilder();
			for (Entity entity : entityManager.getAllEntities().values()) entityData.putData(entity.getId(), entity);

			Path newPath = Paths.get("worldRoot/entities.data");
			Files.write(newPath, entityData.build().toByteArray());
		}
		catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		//Saving planes
		try {			
			for (Plane plane : planeManager.getPlanes().values()) {
				
				byte[] array = plane.toByteArray();
				Path newPath = Paths.get("worldRoot/planes/"+plane.getId()+".data");
				
				Files.write(newPath, array);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		//Saving celestialObjects
		try {
			for (CelestialObject object : celestialObjectManager.getCelestialObjects().values()) {
				
				byte[] array = object.toByteArray();
				Path newPath = Paths.get("worldRoot/celestialObjects/"+object.getId()+".data");
				
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
		if (player.data != null && player.getUsername() != null) {
			try {
				// Ensure playerData directory exists
				Path playerDataDir = Paths.get("worldRoot/playerData");
				if (!Files.exists(playerDataDir)) {
					Files.createDirectories(playerDataDir);
					System.out.println("[ServerSaver] Created playerData directory");
				}
				
				Path path = playerDataDir.resolve(player.getUsername() + ".data");
				byte[] data = player.data.toByteArray();
				
				Files.write(path, data);
				System.out.println("[ServerSaver] ✓ Saved player: " + player.getUsername() + 
					" (entityId: " + player.data.getEntityId() + ") to " + path);
			} catch (IOException e) {
				System.err.println("[ServerSaver] ✗ ERROR saving player " + player.getUsername());
				e.printStackTrace();
			}
		}
	}
}
