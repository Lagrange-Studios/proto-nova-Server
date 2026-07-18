package file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import main.Console;
import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.CraftingRecipeProto.CraftingComponent;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlayerDataProto.PlayerData;
import protonova.protobuf.EntityDataProto.EntityData;

public class ServerLoader {

	private Console console;
	
	public ServerLoader(Console console) {
		this.console = console;
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
		
		HashMap<Integer, Plane> planes = new HashMap<Integer,Plane>();
		
		File[] planesFiles = new File("worldRoot/planes").listFiles();
		
		for (int i=0;i<planesFiles.length;i++) {
			Plane newPlane = loadPlane(planesFiles[i]);
			planes.put(newPlane.getId(), newPlane);
		}
		
		return planes;
	}
	
	public PlayerData getPlayerData(String username) {
		
		// Try to load directly from username file first
		Path playerFilePath = Paths.get("worldRoot/playerData/" + username + ".data");
		
		if (Files.exists(playerFilePath)) {
			try {
				PlayerData playerData = PlayerData.parseFrom(Files.readAllBytes(playerFilePath));
				
				// Verify the username matches (sanity check)
				if (playerData.getUsername().equals(username)) {
					console.print("✓ Successfully loaded player: " + username + " (entityId: " + playerData.getEntityId() + ")");
					return playerData;
				}
			} catch (IOException e) {
				console.print("ERROR: Failed to load player data for " + username);
				e.printStackTrace();
			}
		}
		
		// File doesn't exist - new player
		console.print("✓ New player detected: " + username);
		return PlayerData.newBuilder()
				.setEntityId(0)
				.setUsername(username)
				.build();
	}

	public HashMap<Integer, Entity> loadEntities() {
		
		HashMap<Integer, Entity> allEntities = new HashMap<Integer, Entity>();
		
		File entities = new File("worldRoot/entities.data");
		
		if (entities.exists()) {
			try {
				EntityData entityData = EntityData.parseFrom(Files.readAllBytes(Path.of(entities.getPath())));
				
				for (Entity entity : entityData.getDataMap().values()) allEntities.put(entity.getId(), entity);
			} catch (IOException e) {
				e.printStackTrace();
				console.print("Something went wrong loading entities: "+e.getMessage());
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

	public HashMap<String, Entity> loadEntityAssets() {
		HashMap<String, Entity> entityAssets = new HashMap<String,Entity>();
		
		File[] entityAssetFiles = new File("assets/entities").listFiles();
		
		for (int i=0;i<entityAssetFiles.length;i++) {
			try {
				String fileName = entityAssetFiles[i].getName();
				Entity newObject = Entity.parseFrom(Files.readAllBytes(Path.of(entityAssetFiles[i].getPath())));
				entityAssets.put(fileName.substring(0,fileName.lastIndexOf('.')), newObject);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return entityAssets;
	}
	
	public ArrayList<CraftingRecipe> loadCraftingRecipes() {
		ArrayList<CraftingRecipe> recipeList = new ArrayList<>();
		
		File[] craftingRecipes = new File("assets/crafting").listFiles();
		
		for (File file : craftingRecipes) {
			try {
				JSONObject jsonRecipe = new JSONObject(Files.readString(Path.of(file.getPath())));
				
				JSONObject item1 = jsonRecipe.getJSONObject("item1");
				CraftingComponent itemComponent1 = loadItemComponent(item1);
				
				JSONObject item2 = jsonRecipe.getJSONObject("item2");
				CraftingComponent itemComponent2 = loadItemComponent(item2);
				
				CraftingRecipe.Builder newRecipe = CraftingRecipe.newBuilder();
				
				newRecipe.setItem1(itemComponent1);
				newRecipe.setItem1MustBeHeld(item1.getBoolean("mustBeHeld"));
				newRecipe.setItem2(itemComponent2);
				newRecipe.setResult(jsonRecipe.getString("result"));
				if (jsonRecipe.has("tileResult")) newRecipe.setTileResult(jsonRecipe.getBoolean("tileResult"));
				
						
				recipeList.add(newRecipe.build());
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return recipeList;
	}
	
	private CraftingComponent loadItemComponent(JSONObject itemJson) {
		CraftingComponent.Builder component = CraftingComponent.newBuilder();
		
		component.setName(itemJson.getString("name"));
		component.setConsumed(itemJson.getBoolean("consumed"));
		
		if (itemJson.has("minimumSlotValue")) {
			JSONObject slotValues = itemJson.getJSONObject("minimumSlotValue");
			
			for (String key : slotValues.keySet()) {
				component.putMinimumSlotValue(key, slotValues.getInt(key));
			}
		}
		
		if (itemJson.has("tagsRequired")) {
			 JSONArray tagsRequired = itemJson.getJSONArray("tagsRequired");
			 
			 for (int i=0;i<tagsRequired.length();i++) {
				 component.addTagsRequired(tagsRequired.getString(i));
			 }
		}
		
		return component.build();
		
	}
	
	public JSONObject getGamemode() {
		File gamemodeFile = new File("worldRoot/gamemode.json");
		
		try {
			return new JSONObject(Files.readString(Path.of(gamemodeFile.getPath())));
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public HashMap<String, HashSet<String>> loadTypes() {
		HashMap<String, HashSet<String>> types = new HashMap<>();
		
		File typeFolder = new File("assets/types");
		
		if (typeFolder.exists()) {
			for (File typeFile : typeFolder.listFiles()) {
				try {
					String name = typeFile.getName();
					name = (String) name.subSequence(0, name.indexOf('.')-1);
					
					JSONArray array = new JSONArray(Files.readString(Path.of(typeFile.getPath())));
					
					HashSet<String> typesSet = new HashSet<>();
					
					for (int i=0;i<array.length();i++) {
						typesSet.add(array.getString(i));
					}
					
					types.put(name, typesSet);
					
				} catch (JSONException | IOException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			console.print("Warning: No type folder was found");
		}
		
		return types;
	}
}
