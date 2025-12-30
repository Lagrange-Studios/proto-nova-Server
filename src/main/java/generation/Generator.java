package generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import entity.EntityManager;
import main.Console;
import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.VectorProto.Vector;
import util.Id;

public class Generator {
	
	private Console console;
	private HashMap<Integer, Plane> planes;
	private EntityManager entityManager;
	private PlaneGenerator planeGenerator;
	
	public Generator(Console console, HashMap<Integer, Plane> planes, EntityManager entityManager) {
		this.console = console;
		this.planes = planes;
		this.entityManager = entityManager;
		planeGenerator = new PlaneGenerator(planes);
	}
	
	public void generateWorld() {
		
		Plane plane = planeGenerator.generatePlane(100, 100);
		planes.put(plane.getId(), plane);
		
		generateEcosystem(plane.getId());
	}
	
	private void generateEcosystem(int planeId) {
		
		for (int i=0;i<10;i++) {
			Vector position = Vector.newBuilder()
					.setX((float) (Math.random()*100-50))
					.setY((float) (Math.random()*100-50))
					.build();
			
			Entity frog = entityManager.makeNewEntity("frog", planeId);
			entityManager.updateEntity(frog.toBuilder().setPosition(position).build());
		}
	}
	
	
	// Decreptated
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
}
