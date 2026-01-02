package generation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import entity.EntityManager;
import file.AssetManager;
import main.Console;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.TileProto.Tile;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;
import util.FileReader;

public class EnviromentGenerator {

	private Console console;
	private EntityManager entityManager;
	private AssetManager assetManager;

	public EnviromentGenerator(AssetManager assetManager, EntityManager entityManager, Console console) {
		this.assetManager = assetManager;
		this.entityManager = entityManager;
		this.console = console;
	}
	
	private HashMap<String, ArrayList<Tile>> countTiles(Plane plane) {
		
		HashMap<String,ArrayList<Tile>> tiles = new HashMap<>();
		
		for (Tile tile : plane.getTilesMap().values()) {
			if (tiles.containsKey(tile.getName())) {
				tiles.get(tile.getName()).add(tile);
			}
			else {
				ArrayList<Tile> list = new ArrayList<>();
				list.add(tile);
				tiles.put(tile.getName(), list);
			}
		}
		
		return tiles;
	}

	public void generateEnviroment(Plane plane, String type) {
		
		HashMap<String, ArrayList<Tile>> planeTiles = countTiles(plane);
		
		File enviromentFolder = new File("assets/generation/enviroment/"+type);
		
		if (!enviromentFolder.exists()) return;
		
		File[] enviromentDataFiles = enviromentFolder.listFiles();
		
		for (int i=0;i<enviromentDataFiles.length;i++) {
			
			JSONObject generationData = new JSONObject(FileReader.readJSONFile(enviromentDataFiles[i].getPath()));
			
			if (!assetManager.containsEntity(generationData.getString("name"))) continue; // Check if we even have the entity
			
			
			JSONArray tilesArray = generationData.getJSONArray("tiles");
			ArrayList<Tile> validTiles = new ArrayList<>();
			
			// Gather all the valid tiles
			for (Object tileName : tilesArray) {
				
				if (planeTiles.containsKey(tileName)) {
					validTiles.addAll(planeTiles.get(tileName));
				}
			}
			
			// divide the size by density and then add entities randomly
			for (int u=0;u<Math.ceil(validTiles.size()/generationData.getDouble("tileDensity"));u++) {
				int randomIndex = (int) Math.round(Math.random()*validTiles.size());
				
				Entity clone = assetManager.getEntity(generationData.getString("name"), plane.getId());
				
				Vector newPosition = CoordinateConverter.toVector(validTiles.get(randomIndex).getCoordinate());
				
				if (!generationData.getBoolean("grid")) {
					newPosition = newPosition.toBuilder()
							.setX((float) (newPosition.getX()+Math.random()/2))
							.setY((float) (newPosition.getY()+Math.random()/2))
							.build();
				}
				
				clone = clone.toBuilder()
					.setPosition(newPosition)
					.build();
				
				entityManager.updateEntity(clone);
				validTiles.remove(randomIndex);
			}
		}
	}
}
