package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import file.AssetManager;
import main.Console;
import protonova.protobuf.CraftingRecipeProto.CraftingRecipe;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.LootTableItemProto.lootTableItem;
import protonova.protobuf.OrgansProto.Organs;
import protonova.protobuf.VectorProto.Vector;

import java.util.concurrent.ThreadLocalRandom;


public class LootTableManager {
	private static final float MIN_DROP_SPEED = 2.0f;
	private static final float MAX_DROP_SPEED = 7.0f;
	
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
				Vector velocity = randomDropVelocity();
				result = result.toBuilder().setVelocity(velocity).setPosition(entity.getPosition()).build();
				entityManager.updateEntity(result);
				loot.add(result);
			}
		}
		return loot;
	}
	
	public void dropEverythingIncludingInsides(Entity entity) {
		
		for (lootTableItem item : getLootTable(entity)) {			
			Entity result = assetManager.getEntity(item.getItemName(), entity.getMap());
			Vector velocity = randomDropVelocity();
			result = result.toBuilder().setVelocity(velocity).setPosition(entity.getPosition()).build();
			entityManager.updateEntity(result);			
		}
	}

	private Vector randomDropVelocity() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double angle = random.nextDouble(0, Math.PI * 2);
		float speed = random.nextFloat(MIN_DROP_SPEED, MAX_DROP_SPEED);
		return Vector.newBuilder()
				.setX((float) (Math.cos(angle) * speed))
				.setY((float) (Math.sin(angle) * speed))
				.build();
	}
	
	
}
