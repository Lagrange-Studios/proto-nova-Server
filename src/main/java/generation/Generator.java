package generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import entity.EntityManager;
import file.AssetManager;
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
	private AssetManager assetManager;
	private EnviromentGenerator enviromentGenerator;
	
	public Generator(Console console, HashMap<Integer, Plane> planes, EntityManager entityManager, AssetManager assetManager) {
		this.console = console;
		this.planes = planes;
		this.entityManager = entityManager;
		this.assetManager = assetManager;
		
		enviromentGenerator = new EnviromentGenerator(assetManager,entityManager,console);
		planeGenerator = new PlaneGenerator(planes,console);
	}
	
	private String getRandomWorldType() {
		File[] files = new File("assets/generation/terrain").listFiles();
		File randomFile = files[(int) Math.round(Math.random()*(files.length-1))];
		String fileName = randomFile.getName();
		
		return fileName.substring(0,fileName.lastIndexOf('.'));
	}
	
	public void generateWorld(String worldType) {
		
		Plane plane = planeGenerator.generatePlane(100, 100, worldType);
		
		if (plane != null) {
			planes.put(plane.getId(), plane);
			
			enviromentGenerator.generateEnviroment(plane, worldType);
			
			console.print("Generated new plane with id: "+plane.getId()+ " Type: "+worldType);
		}
	}
	
	public void generateWorld() {
		generateWorld(getRandomWorldType());
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
