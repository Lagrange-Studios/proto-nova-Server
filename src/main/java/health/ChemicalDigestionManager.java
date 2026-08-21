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
	private ChemicalManager chemicalManager;
	private HashMap<String, ChemicalDefinition> storedChemicals = new HashMap<>();
	
	public ChemicalDigestionManager(EntityManager entityManager, ChemicalManager chemicalManager) {
		this.entityManager = entityManager;
		this.chemicalManager = chemicalManager;
		loadChemicalsIntoMemory();
	}
	
	private void loadChemicalsIntoMemory() {
		File folder = new File("assets/chemicals");
	    File[] chemicals = folder.listFiles();
		for (File file : chemicals) {
			try (FileReader reader = new FileReader(file)) {
	            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
	            
	            String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
	            ChemicalDefinition chemical = new ChemicalDefinition(jsonObject);
	            storedChemicals.put(name, chemical);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
	}
	
	@SuppressWarnings("unlikely-arg-type")
	public void processEntityChemicals(Entity entity) {
		
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private void applyChemicalToEntity(Entity entity, Chemical chemical) {
		
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private Damage calculateChemicalHealing(Entity entity, Chemical chemical) {
		return null;
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private int calculateSaturation(Entity entity, Chemical chemical) {
		return 0;
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private Damage calculateChemicalDamage(Entity entity, Chemical chemical) {
		return null;
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private boolean isOverdose(Entity entity, Chemical chemical) {
		if (storedChemicals.get(chemical.getName()).getOverdose() == null) {
			return false;
		}
		if (chemical.getAmount() >= storedChemicals.get(chemical.getName()).getOverdose()) {
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private boolean isInTemperatureRange(Entity entity, Chemical chemical) {
		Double maxTemp = storedChemicals.get(chemical.getName()).getOverdose();
		Double minTemp = storedChemicals.get(chemical.getName()).getOverdose();
		int entityTemperature = entity.getTemperature();
		if (maxTemp != null) {
			if (minTemp != null) {
				if (entityTemperature > maxTemp && entityTemperature < minTemp) {
					return false;
				}
			} else {
				if (entityTemperature > maxTemp) {
					return false;
				}
			}
		}else if (minTemp != null) {
			if (entityTemperature < minTemp) {
				return false;
			}
		}
		return true;
	}
	
}
