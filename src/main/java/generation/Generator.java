package generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import main.Console;
import plane.PlaneManager;
import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.VectorProto.Vector;
import util.Id;

public class Generator {
	
	private Console console;
	private PlaneManager planeManager;
	private EntityManager entityManager;
	private PlaneGenerator planeGenerator;
	private AssetManager assetManager;
	private EnviromentGenerator enviromentGenerator;
	private EntityFinder entityFinder;
	
	public Generator(Console console, PlaneManager planeManager, EntityManager entityManager, AssetManager assetManager, EntityFinder entityFinder) {
		this.console = console;
		this.planeManager = planeManager;
		this.entityManager = entityManager;
		this.assetManager = assetManager;
		this.entityFinder = entityFinder;
		
		enviromentGenerator = new EnviromentGenerator(assetManager,entityManager,console, entityFinder);
		planeGenerator = new PlaneGenerator(planeManager.getPlanes(),console);
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
			planeManager.updatePlane(plane.getId(), plane);
			
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
