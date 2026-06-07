package action;

import java.util.ArrayList;
import java.util.HashMap;

import entity.EntityManager;
import file.AssetManager;
import main.Console;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;
import protonova.protobuf.EntityProto.Entity;

public class CraftingManager {
	private HashMap<String, CraftingRecipe> itemsToRecipes;
	private HashMap<String, CraftingRecipe> RecipeNames; // TODO: add implementation later when needed
	private EntityManager entityManager;
	private Console console;
	private AssetManager assetManager;
	
	public CraftingManager(EntityManager entityManager,ArrayList<CraftingRecipe> loadedRecipes, Console console, AssetManager assetManager) {
		this.entityManager = entityManager;
		this.console = console;
		this.assetManager = assetManager;
		
		// make the recipes accesible based on crafting components
		loadRecipes(loadedRecipes);
	}
	
	public CraftingRecipe getRecipe(String item1Name, String item2Name) {
		if (itemsToRecipes.containsKey(item1Name+item2Name)) return itemsToRecipes.get(item1Name+item2Name);
		else if (itemsToRecipes.containsKey(item2Name+item1Name)) return itemsToRecipes.get(item2Name+item1Name);
		return null;
	}
	
	public CraftingRecipe getrecipe(Entity entity1, Entity entity2) {
		return getRecipe(entity1.getName(),entity2.getName());
	}
	
	/**
	 * Attempts to craft based on a entitys selected item and another entity
	 * @return returns the updated form of the crafting entity
	 */
	public Entity attemptCraftingRecipe(Entity craftingEntity, Entity component) {

		String selectedSlot = craftingEntity.getSelectedSlot();
		if (!craftingEntity.getInventorySlotsMap().containsKey(selectedSlot)) return craftingEntity;
		
		Entity heldComponent = entityManager.getEntity(craftingEntity.getInventorySlotsMap().get(selectedSlot));
		
		// null check
		if (heldComponent != null && component != null) {
			CraftingRecipe recipe = getrecipe(heldComponent,component);
			
			if (recipe != null) {
				
				// WARNING: this could be a possible dupe glitch in the future but also maybe not since we track entity ids
				Entity result = assetManager.getEntity(recipe.getResult(), component.getMap());
				result = result.toBuilder().setPosition(component.getPosition()).build();
				entityManager.updateEntity(result);
				
				if (recipe.getItem1Consumed()) {
					craftingEntity = entityManager.decrementSlot(craftingEntity, selectedSlot);
				}
				
				if (recipe.getItem2Consumed()) {
					component = entityManager.decrementAmount(component);
				}
				
				
				
			}
		}
		
		return craftingEntity;
	}
	
	private void loadRecipes(ArrayList<CraftingRecipe> loadedRecipes) {
		itemsToRecipes = new HashMap<>();
		
		for (CraftingRecipe recipe : loadedRecipes) {
			attemptToAddRecipe(recipe.getItem1Name()+recipe.getItem2Name(),recipe);
			
			// only add the other variation of recipe if its not forced to be held and theres different ingredients
			if (!recipe.getItem1MustBeHeld() && !recipe.getItem1Name().equals(recipe.getItem2Name())) {
				attemptToAddRecipe(recipe.getItem2Name()+recipe.getItem1Name(),recipe);
			}
		}
	}
	
	/**
	 * Checks the recipe list to see if there is another recipe with this value
	 */
	private void attemptToAddRecipe(String key, CraftingRecipe value) {
		
		if (itemsToRecipes.containsKey(key)) {
			String warningMessage = "Warning: Could not add recipe for "+value.getResult()+" becuase it conflicts with the recipe for "+ itemsToRecipes.get(key).getResult();
			console.print(warningMessage);
			System.err.print(warningMessage);
		}
		else if (!assetManager.containsEntity(value.getItem1Name())) console.print("Warning: Could not add recipe for "+value.getResult()+" becuase the asset manager is missing: "+value.getItem1Name());
		else if (!assetManager.containsEntity(value.getItem2Name())) console.print("Warning: Could not add recipe for "+value.getResult()+" becuase the asset manager is missing: "+value.getItem2Name());
		else if (!assetManager.containsEntity(value.getResult())) console.print("Warning: Could not add recipe for "+value.getResult()+" becuase the asset manager is missing: "+value.getResult());
		else itemsToRecipes.put(key, value);
	}
	
}
