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
import space.CelestialObjectManager;
import util.Id;

public class Generator {
	
	private Console console;
	private PlaneManager planeManager;
	private EntityManager entityManager;
	private PlaneGenerator planeGenerator;
	private AssetManager assetManager;
	private EnviromentGenerator enviromentGenerator;
	private EntityFinder entityFinder;
	private CelestialObjectManager celestialObjectManager;
	
	public Generator(Console console, PlaneManager planeManager, EntityManager entityManager, AssetManager assetManager, EntityFinder entityFinder, CelestialObjectManager celestialObjectManager) {
		this.console = console;
		this.planeManager = planeManager;
		this.entityManager = entityManager;
		this.assetManager = assetManager;
		this.entityFinder = entityFinder;
		this.celestialObjectManager = celestialObjectManager;
		
		enviromentGenerator = new EnviromentGenerator(assetManager,entityManager,console, entityFinder);
		planeGenerator = new PlaneGenerator(planeManager.getPlanes(),console);
	}
	
	private String getRandomWorldType() {
		File[] files = new File("assets/generation/terrain").listFiles();
		File randomFile = files[(int) Math.round(Math.random()*(files.length-1))];
		String fileName = randomFile.getName();
		
		return fileName.substring(0,fileName.lastIndexOf('.'));
	}
	
	private Plane generatePlane(String worldType, int size) {
		
		Plane plane = planeGenerator.generatePlane(size, size, worldType);
		
		if (plane != null) {
			planeManager.updatePlane(plane.getId(), plane);
			
			enviromentGenerator.generateEnviroment(plane, worldType);
			
			console.print("Generated new plane with id: "+plane.getId()+ " Type: "+worldType);
			
			return plane;
		}
		return null;
	}
	
	public void generatePlanet(int width, String type, double rotationPeroid) {
		
		Plane plane = generatePlane(type,width);
		
		if (plane != null) {
			
			CelestialObject planet = CelestialObject.newBuilder()
					.setId(Id.getNewId(celestialObjectManager.getCelestialObjects().keySet()))
					.setTileWidth(width)
					.setRotationPeroidMinutes(rotationPeroid)
					.setSurfacePlaneId(plane.getId())
					.setCurrentRotation(0.5)
					.build();
			
			celestialObjectManager.updateCelestialObject(planet);

			console.print("Generated new planet with id: "+planet.getId());
		}
	}
	
	public void generatePlanet(String type) {
		generatePlanet(500, type, 5);
	}
	
	public void generatePlanet() {
		generatePlanet(getRandomWorldType());
	}
	
	
}
