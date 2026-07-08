package action;

import java.util.ArrayList;
import java.util.HashMap;

import entity.EntityManager;
import file.AssetManager;
import main.Console;
import plane.PlaneManager;
import protonova.protobuf.CraftingRecipeProto.CraftingComponent;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.TileProto.Tile;
import protonova.protobuf.VectorProto.Vector;

public class CraftingManager {
	private HashMap<String, CraftingRecipe> itemsToRecipes;
	private HashMap<String, CraftingRecipe> recipeNames; // TODO: add implementation later when needed
	private EntityManager entityManager;
	private Console console;
	private AssetManager assetManager;
	private PlaneManager planeManager;
	
	public CraftingManager(EntityManager entityManager,ArrayList<CraftingRecipe> loadedRecipes, Console console, AssetManager assetManager, PlaneManager planeManager) {
		this.entityManager = entityManager;
		this.console = console;
		this.assetManager = assetManager;
		this.planeManager = planeManager;
		
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
			
			if (recipe != null && (!recipe.hasTileResult() || planeManager.getTileAt(component).getSurfaceTexture().equals("")) &&
					checkComponent(heldComponent,recipe.getItem1()) && checkComponent(component,recipe.getItem2())) {
				
				if (recipe.getTileResult()) {
					// tile result
					
					Tile tile = planeManager.getTileAt(component);
					tile = tile.toBuilder().setSurfaceTexture(recipe.getResult()).build();
					planeManager.updateTile(tile, component.getMap());
				}
				else {
					// entity result
					
					// WARNING: this could be a possible dupe glitch in the future but also maybe not since we track entity ids
					Entity result = assetManager.getEntity(recipe.getResult(), component.getMap());
					
					if (result.getAnchored()) {
						Vector position = component.getPosition();
						position = position.toBuilder()
								.setX(Math.round(position.getX()))
								.setY(Math.round(position.getY()))
								.build();
						
						result = result.toBuilder().setPosition(position).build();
					}
					else result = result.toBuilder().setPosition(component.getPosition()).build();
					entityManager.updateEntity(result);
				}
				
				
				if (recipe.getItem1().getConsumed()) {
					craftingEntity = entityManager.decrementSlot(craftingEntity, selectedSlot);
				}
				
				if (recipe.getItem2().getConsumed()) {
					component = entityManager.decrementAmount(component);
				}
				
				
				
			}
		}
		
		return craftingEntity;
	}
	
	private boolean checkComponent(Entity component, CraftingComponent craftingComponent) {
		
		for (String key : craftingComponent.getMinimumSlotValueMap().keySet()) {			
			if (!component.containsInventorySlots(key) || 
					component.getInventorySlotsMap().get(key) < craftingComponent.getMinimumSlotValueMap().get(key)) return false;
		}
		
		for (String tag : craftingComponent.getTagsRequiredList()) {
			if (!component.getTagsList().contains(tag)) return false;
		}
		
		return true;
	}
	
	private void loadRecipes(ArrayList<CraftingRecipe> loadedRecipes) {
		itemsToRecipes = new HashMap<>();
		
		for (CraftingRecipe recipe : loadedRecipes) {
			attemptToAddRecipe(recipe.getItem1().getName()+recipe.getItem2().getName(),recipe);
			
			// only add the other variation of recipe if its not forced to be held and theres different ingredients
			if (!recipe.getItem1MustBeHeld() && !recipe.getItem1().getName().equals(recipe.getItem2().getName())) {
				attemptToAddRecipe(recipe.getItem2().getName()+recipe.getItem1().getName(),recipe);
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
		else if (!assetManager.containsEntity(value.getItem1().getName())) console.print("Warning: Could not add recipe for "+value.getResult()+" becuase the asset manager is missing: "+value.getItem1().getName());
		else if (!assetManager.containsEntity(value.getItem2().getName())) console.print("Warning: Could not add recipe for "+value.getResult()+" becuase the asset manager is missing: "+value.getItem2().getName());
		else if (!value.getTileResult() && !assetManager.containsEntity(value.getResult())) console.print("Warning: Could not add recipe for "+value.getResult()+" becuase the asset manager is missing: "+value.getResult());
		else itemsToRecipes.put(key, value);
	}
	
}
