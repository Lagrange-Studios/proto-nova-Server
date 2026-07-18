package file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;

import entity.EntityManager;
import gamemode.GamemodeManager;
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
	private GamemodeManager gamemodeManager;
	
	public ServerSaver(Server server, EntityManager entityManager, PlaneManager planeManager, CelestialObjectManager celestialObjectManager, GamemodeManager gamemodeManager) {
		this.server = server;
		this.entityManager = entityManager;
		this.planeManager = planeManager;
		this.celestialObjectManager = celestialObjectManager;
		this.gamemodeManager = gamemodeManager;
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
			System.err.println("ERROR: Failed to save entity data.");
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
			System.err.println("ERROR: Failed to save plane data.");
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
			System.err.println("ERROR: Failed to save celestial object data.");
			return e.getMessage();
		}
		
		// saving gamemode
		try {
			JSONObject gamemode = gamemodeManager.getGamemode();
			Files.write(Path.of("worldRoot/gamemode.json"), gamemode.toString().getBytes());
		} catch (IOException e) {
			System.err.println("ERROR: Failed to save gamemode data.");
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
				}
				
				Path path = playerDataDir.resolve(player.getUsername() + ".data");
				byte[] data = player.data.toByteArray();
				
				Files.write(path, data);
			} catch (IOException e) {
				System.err.println("ERROR: Failed to save player " + player.getUsername() + ".");
			}
		}
	}
}
