package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import file.AssetManager;
import main.Console;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.LootTableItemProto.lootTableItem;
import util.ItemDropVelocity;

public class LootTableManager {

	private EntityManager entityManager;
	private Console console;
	private AssetManager assetManager;

	public LootTableManager(
			EntityManager entityManager,
			Console console,
			AssetManager assetManager) {

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
		ArrayList<Entity> droppedLoot = new ArrayList<>();

		for (lootTableItem lootEntry : getLootTable(entity)) {
			double randomPercent = ThreadLocalRandom.current().nextDouble(0, 100);
			if (randomPercent < lootEntry.getProbability()) {
				droppedLoot.addAll(dropLootEntry(entity, lootEntry));
			}
		}

		return droppedLoot;
	}

	public void dropEverythingIncludingInsides(Entity entity) {
		for (lootTableItem lootEntry : getLootTable(entity)) {
			dropLootEntry(entity, lootEntry);
		}
	}

	private ArrayList<Entity> dropLootEntry(Entity sourceEntity, lootTableItem lootEntry) {
		ArrayList<Entity> droppedEntities = new ArrayList<>();
		int requestedAmount = 1;

		if (lootEntry.hasAmount()) {
			requestedAmount = Math.max(1, lootEntry.getAmount());
		}

		Entity firstDroppedEntity = assetManager.getEntity(
				lootEntry.getItemName(),
				sourceEntity.getMap());

		if (firstDroppedEntity == null) {
			return droppedEntities;
		}

		if (firstDroppedEntity.getStackable()) {
			firstDroppedEntity = firstDroppedEntity.toBuilder()
					.setAmount(requestedAmount)
					.build();
			droppedEntities.add(placeDroppedEntity(firstDroppedEntity, sourceEntity));
			return droppedEntities;
		}

		droppedEntities.add(placeDroppedEntity(firstDroppedEntity, sourceEntity));
		for (int droppedAmount = 1; droppedAmount < requestedAmount; droppedAmount++) {
			Entity additionalDroppedEntity = assetManager.getEntity(
					lootEntry.getItemName(),
					sourceEntity.getMap());
			if (additionalDroppedEntity != null) {
				droppedEntities.add(placeDroppedEntity(additionalDroppedEntity, sourceEntity));
			}
		}

		return droppedEntities;
	}

	private Entity placeDroppedEntity(Entity droppedEntity, Entity sourceEntity) {
		Entity placedEntity = droppedEntity.toBuilder()
				.setPosition(sourceEntity.getPosition())
				.setVelocity(ItemDropVelocity.createRandomScatterVelocity())
				.setAnchored(false)
				.build();
		entityManager.updateEntity(placedEntity);
		return placedEntity;
	}
}
