package action;

import entity.EntityFinder;
import entity.EntityManager;
import health.CombatManager;
import health.Health;
import health.HealthManager;
import main.Console;
import plane.PlaneManager;
import protonova.protobuf.ActionProto.Action;
import protonova.protobuf.ActionProto.ActionType;
import protonova.protobuf.ActionProto.InteractionType;
import protonova.protobuf.EntityProto.Entity;
import socket.Player;
import tag.TagHandler;

public class ActionHandler {

	private Console console;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private PlaneManager planeManager;
	private CraftingManager craftingManager;
	private TagHandler tagHandler;
	private CombatManager combatManager;
	private HealthManager healthManager;

	public ActionHandler(Console console, EntityManager entityManager, EntityFinder entityFinder, PlaneManager planeManager,
			CraftingManager craftingManager, TagHandler tagHandler, CombatManager combatManager, HealthManager healthManager) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.craftingManager = craftingManager;
		this.tagHandler = tagHandler;
		this.combatManager = combatManager;
		this.healthManager = healthManager;
	}

	public Entity executeAction(Player player, Action action, Entity playerEntity) {
		
		if (action.getActionType() != ActionType.Interact) return playerEntity;
		Entity interactingEntity = entityManager.getEntity(action.getInteractingEntityId());
		
		switch(action.getInteractionType().getNumber()) {
			case(InteractionType.PickUp_VALUE):
				
				if (interactingEntity == null) {
					console.print("Warning: Null interaction entity");
					break;
				}
				if (!interactingEntity.getIsItem()) break;
				
				if (playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) {
					Entity heldItem = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
					
					// check for same item and stacking
					if (interactingEntity.getName().equals(heldItem.getName()) && heldItem.getStackable()) {
						int newAmount = heldItem.getAmount() + interactingEntity.getAmount(); // for held item
						int leftOver = newAmount - 30; // for interacting entity
						newAmount = Math.min(newAmount, 30);
						
						if (leftOver < 1) {
							entityManager.removeEntity(interactingEntity);
						}
						else {
							interactingEntity = interactingEntity.toBuilder()
									.setAmount(leftOver)
									.build();
							entityManager.updateEntity(interactingEntity);
						}
						
						heldItem = heldItem.toBuilder()
								.setAmount(newAmount)
								.build();
						
						entityManager.updateEntity(heldItem);
					}
				}
				else { 
					// Normal pickup
					interactingEntity = interactingEntity.toBuilder()
							.setMap(0)
							.build();
					
					entityManager.updateEntity(interactingEntity);
			
					playerEntity = playerEntity.toBuilder()
						.putInventorySlots(playerEntity.getSelectedSlot(), interactingEntity.getId())
						.build();
				}
				
				break;
				
			case(InteractionType.Drop_VALUE):
				
				if (!playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) break;
				
				Entity item = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
				item = item.toBuilder()
					.setPosition(action.getInteractionPosition())
					.setMap(playerEntity.getMap())
					.build();
				entityManager.updateEntity(item);
			
				playerEntity = playerEntity.toBuilder()
					.removeInventorySlots(playerEntity.getSelectedSlot())
					.build();
				
				break;
			case(InteractionType.DropOne_VALUE):
				
				if (!playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) break;
				
				Entity heldItem = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
				
				// item is not stackable or there is just one
				if (!heldItem.getStackable() || heldItem.getAmount() == 1) {
					heldItem = heldItem.toBuilder()
							.setPosition(action.getInteractionPosition())
							.setMap(playerEntity.getMap())
							.build();
					entityManager.updateEntity(heldItem);
					
					playerEntity = playerEntity.toBuilder()
						.removeInventorySlots(playerEntity.getSelectedSlot())
						.build();
				}
				else {
					heldItem = heldItem.toBuilder()
							.setAmount(heldItem.getAmount()-1)
							.build();
					entityManager.updateEntity(heldItem);
					
					Entity clonedItem = heldItem.toBuilder()
							.setAmount(1)
							.setPosition(action.getInteractionPosition())
							.setMap(playerEntity.getMap())
							.setId(entityManager.reserveNewEntityId())
							.build();
					entityManager.updateEntity(clonedItem);
							
				}
				
				break;
			case(InteractionType.Craft_VALUE):
				playerEntity = craftingManager.attemptCraftingRecipe(playerEntity, interactingEntity);
				break;
			case(InteractionType.Hit_VALUE):
				System.out.println("Player combat attempted");
				combatManager.attemptToDamage(playerEntity, interactingEntity);
				playerEntity = entityManager.getEntity(player);
				break;
			case(InteractionType.Standard_VALUE):
				playerEntity = tagHandler.interact(playerEntity, interactingEntity);
				break;
		}
		
		
		return playerEntity;
	}
}
