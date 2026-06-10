package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import file.AssetManager;
import main.Console;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.LootTableItemProto.lootTableItem;
import protonova.protobuf.VectorProto.Vector;

import java.util.concurrent.ThreadLocalRandom;


public class LootTableManager {
	
	private EntityManager entityManager;
	private Console console;
	private AssetManager assetManager;
	
	
	public LootTableManager(EntityManager entityManager, Console console, AssetManager assetManager) {
		this.entityManager = entityManager;
		this.console = console;
		this.assetManager = assetManager;
	}
	
	public void dropLoot(Entity entity) {
		rollLootTable(entity);
		
	}
	
	public List<lootTableItem> getLootTable(Entity entity) {
		return entity.getLootTableList();
	}
	
	private ArrayList<Entity> rollLootTable(Entity entity) {
		ArrayList<Entity> loot = new ArrayList<>();
		for (lootTableItem item : getLootTable(entity)) {
			double prob = item.getProbability();
			int randomNumber = ThreadLocalRandom.current().nextInt(1, 101);
			if (randomNumber <= prob) {
				Entity result = assetManager.getEntity(item.getItemName(), entity.getMap());
				float offset = ThreadLocalRandom.current().nextFloat(-7, 7);
				Vector velocity = Vector.newBuilder().setX(offset).setY(offset).build();
				result = result.toBuilder().setVelocity(velocity).setPosition(entity.getPosition()).build();
				entityManager.updateEntity(result);
				loot.add(result);
			}
		}
		return loot;
	}
	
}
