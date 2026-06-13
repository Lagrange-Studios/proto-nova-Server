package generation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import entity.EntityManager;
import file.AssetManager;
import main.Console;
import perlinNoise.OpenSimplex2S;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlaneProto.Plane.Builder;
import protonova.protobuf.TileProto.Tile;
import util.CoordinateConverter;
import util.FileReader;
import util.Id;

public class PlaneGenerator {

	// Perlin noise generator https://github.com/LefMarOli/PerlinNoiseJava.git
	
	private HashMap<Integer, Plane> planes;
	private Console console;
	private AssetManager assetManager;
	private EntityManager entityManager;
	
	public PlaneGenerator(HashMap<Integer, Plane> planes, Console console, AssetManager assetManager, EntityManager entityManager) {
		this.planes = planes;
		this.console = console;
		this.assetManager = assetManager;
		this.entityManager = entityManager;
	}
	
	public Plane generatePlane(int sizeX, int sizeY, String generationType) {
		
		File generationData = new File("assets/generation/terrain/"+generationType+".json");
		
		if (!generationData.exists()) {
			console.print("Error: "+generationType+ " does not exist");
			return null;
		}
		
		int planeId = Id.getNewId(planes.keySet());
		
		Coordinate size = Coordinate.newBuilder()
				.setX(sizeX)
				.setY(sizeY)
				.build();
		
		Builder plane = Plane.newBuilder()
				.setId(planeId)
				.setSize(size);
		
		long seed = System.currentTimeMillis();
		
		JSONObject generationValues = new JSONObject(FileReader.readJSONFile("assets/generation/terrain/"+generationType+".json"));
		JSONObject tileValues = generationValues.getJSONObject("tiles");
		
		String[] unsortedNames = JSONObject.getNames(tileValues);
		String[] sortedNames = new String[unsortedNames.length];
		int nextIndex = 0;
		
		while (sortedNames[sortedNames.length-1] == null) {
			String largestName = "null";
			double largestValue = -2;
			int largestIndex = 0;
			
			for (int i=0;i<unsortedNames.length;i++) {
				if (unsortedNames[i] != null && tileValues.getDouble(unsortedNames[i]) > largestValue) {
					largestName = unsortedNames[i];
					largestValue = tileValues.getDouble(unsortedNames[i]);
					largestIndex = i;
				}
			}
			
			unsortedNames[largestIndex] = null;
			sortedNames[nextIndex] = largestName;
			nextIndex++;
		}
		
		for (int x=-sizeX/2;x<=sizeX/2;x++) {
			for (int y=-sizeY/2;y<=sizeY/2;y++) {
				
				double value = OpenSimplex2S.noise2(seed,x*generationValues.getDouble("frequency"),y*generationValues.getDouble("frequency"));
				String name = "null";
				
				for (String tileName : sortedNames) {
					if (value >= tileValues.getDouble(tileName)) {
						name = tileName;
						break;
					}
				}
				
				Coordinate coordinate = Coordinate.newBuilder()
						.setX(x)
						.setY(y)
						.build();
				
				Tile tile = Tile.newBuilder()
						.setName(name)
						.setCoordinate(coordinate)
						.setVariant((int) (Math.round(Math.random()*3)+1))
						.build();
				
				plane.putTiles(CoordinateConverter.convert(coordinate), tile);
				
				// border entity
				if (Math.abs(x) == sizeX/2 || Math.abs(y) == sizeY/2) {
					Entity borderEntity = assetManager.getEntity("border", planeId);
					entityManager.updateEntity(borderEntity);
				}
			}
		}		
		
		return plane.build();
	}
	
}
