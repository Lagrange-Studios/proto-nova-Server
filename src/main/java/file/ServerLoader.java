package file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import main.Console;
import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlayerDataProto.PlayerData;

public class ServerLoader {

	private Console console;
	private Validater validater;
	
	public ServerLoader(Console console) {
		this.console = console;
		validater = new Validater(console);
	}
	
	private Plane loadPlane(File planeFile) {
		
		try {
			byte[] array = Files.readAllBytes(Paths.get(planeFile.getPath()));
			
			Plane planeObject = PlaneProto.Plane.parseFrom(array);
			
			return planeObject;
			
		} catch (IOException e) {
			e.printStackTrace();
			console.print("ERROR: " + planeFile.getPath() + " not found");
		}
		
		return null;
		
	}
	
	public HashMap<Integer, Plane> loadWorld() {
		
		validater.validateWorldFiles();
		
		HashMap<Integer, Plane> planes = new HashMap<Integer,Plane>();
		
		File[] planesFiles = new File("worldRoot/planes").listFiles();
		
		for (int i=0;i<planesFiles.length;i++) {
			Plane newPlane = loadPlane(planesFiles[i]);
			planes.put(newPlane.getId(), newPlane);
		}
		
		return planes;
	}
	
	public PlayerData getPlayerData(String username) {
		
		File[] data = new File("worldRoot/playerData").listFiles();
		
		for (int i=0;i<data.length;i++) {
			try {
				PlayerData playerData = PlayerData.parseFrom(Files.readAllBytes(Path.of(data[i].getPath())));
				
				if (playerData.getUsername().equals(username)) {
					return playerData;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// TODO: get the real entity id;
		return Creator.createNewPlayer(username, 0);
	}

	public HashMap<Integer, Entity> loadEntities() {
		
		HashMap<Integer, Entity> allEntities = new HashMap<Integer, Entity>();
		
		File[] entities = new File("worldRoot/entities").listFiles();
		
		for (int i=0;i<entities.length;i++) {
	
			try {
				byte[] entityBytes = Files.readAllBytes(Paths.get(entities[i].getPath()));
				
				Entity entity = Entity.parseFrom(entityBytes);
				allEntities.put(entity.getId(), entity);
				
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
		return allEntities;
	}

	public HashMap<Integer, CelestialObject> loadCelestialObjects() {
		HashMap<Integer, CelestialObject> celestialObjects = new HashMap<Integer,CelestialObject>();
		
		File[] celestialObjectFiles = new File("worldRoot/celestialObjects").listFiles();
		
		for (int i=0;i<celestialObjectFiles.length;i++) {
			try {
				CelestialObject newObject = CelestialObject.parseFrom(Files.readAllBytes(Path.of(celestialObjectFiles[i].getPath())));
				celestialObjects.put(newObject.getId(), newObject);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return celestialObjects;
	}
}
