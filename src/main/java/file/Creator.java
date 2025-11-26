package file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlayerDataProto.PlayerData;
import util.Id;

public class Creator {

	public static void createEmptyPlane(HashMap<Integer,Plane> planes) {
		Plane plane = PlaneGenerator.generatePlane(planes);
		File planeFile = new File("worldRoot/planes/plane" + plane.getId() + ".data");
		
		try {
			Files.write(Paths.get(planeFile.getPath()), plane.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void createCelestialObject(HashMap<Integer,CelestialObject> celestialObjects, int peroid, int speed) {
		
		CelestialObject newObject = CelestialObject.newBuilder()
				.setId(Id.getNewId(celestialObjects.keySet()))
				.setCurrentRotation(0)
				.setRotationPeroid(peroid)
				.setRotationSpeed(speed)
				.build();
		
		File objectFile = new File("worldRoot/planes/plane" + newObject.getId() + ".data");
		
		try {
			Files.write(Paths.get(objectFile.getPath()), newObject.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static PlayerData createNewPlayer(String username, int id) {
		PlayerData data = PlayerData.newBuilder()
				.setUsername(username)
				.setEntityId(id)
				.build();
		return data;
	}
}
