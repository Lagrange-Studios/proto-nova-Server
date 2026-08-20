package health;

import entity.EntityManager;
import protonova.protobuf.ChemicalProto.Chemical;
import protonova.protobuf.CraftingRecipeProto.CraftingComponent;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.EntityProto.Entity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChemicalDigestionManager {
	
	private EntityManager entityManager;
	private HashMap<String, ChemicalDefinition> storedChemicals = new HashMap<>();
	
	public ChemicalDigestionManager(EntityManager entityManager) {
		this.entityManager = entityManager;
		loadChemicalsIntoMemory();
	}
	
	private void loadChemicalsIntoMemory() {
		File folder = new File("assets/chemicals");
	    File[] chemicals = folder.listFiles();
		for (File file : chemicals) {
			try (FileReader reader = new FileReader(file)) {
	            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
	            
	            String name = file.getName();
	            String nameWithoutExtension = name.substring(0, name.lastIndexOf('.'));
	            ChemicalDefinition chemical = new ChemicalDefinition(jsonObject);
	            storedChemicals.put(name, chemical);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
	}
	
	public void processEntityChemicals(Entity entity) {
		
	}
	
	private void applyChemicalToEntity(Entity entity, Chemical chemical) {
		
	}
	
	private Damage calculateChemicalHealing(Entity entity, Chemical chemical) {
		return null;
	}
	
	private Damage calculateChemicalDamage(Entity entity, Chemical chemical) {
		return null;
	}
	
}
