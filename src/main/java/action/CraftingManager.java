package action;

import java.util.ArrayList;
import java.util.HashMap;

import entity.EntityManager;
import main.Console;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;

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
	
	private void loadRecipes(ArrayList<CraftingRecipe> loadedRecipes) {
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
