package action;

import java.util.ArrayList;
import java.util.HashMap;

import entity.EntityManager;
import main.Console;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;
import protonova.protobuf.EntityProto.Entity;

public class CraftingManager {
	private HashMap<String, CraftingRecipe> itemsToRecipes;
	private HashMap<String, CraftingRecipe> RecipeNames; // TODO: add implementation later when needed
	private EntityManager entityManager;
	private Console console;
	
	public CraftingManager(EntityManager entityManager,ArrayList<CraftingRecipe> loadedRecipes, Console console) {
		this.entityManager = entityManager;
		this.console = console;
		
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
	
	/*
	 * Checks the recipe list to see if there is another recipe with this value
	 */
	private void attemptToAddRecipe(String key, CraftingRecipe value) {
		if (itemsToRecipes.containsKey(key)) {
			String warningMessage = "Warning: Could not add recipe for "+value.getResult()+" becuase it conflicts with the recipe for "+ itemsToRecipes.get(key).getResult();
			console.print(warningMessage);
			System.err.print(warningMessage);
		}
		else itemsToRecipes.put(key, value);
	}
	
}
