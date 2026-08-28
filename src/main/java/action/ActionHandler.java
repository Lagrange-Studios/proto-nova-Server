package action;

import entity.EntityFinder;
import entity.EntityManager;
import health.CombatManager;
import health.ConsumptionManager;
import health.Health;
import health.HealthManager;
import main.Console;
import plane.PlaneManager;
import protonova.protobuf.ActionProto.Action;
import protonova.protobuf.ActionProto.ActionType;
import protonova.protobuf.ActionProto.InteractionType;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import socket.Player;
import tag.TagHandler;
import util.VectorMath;

public class ActionHandler {

	private Console console;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private PlaneManager planeManager;
	private CraftingManager craftingManager;
	private TagHandler tagHandler;
	private CombatManager combatManager;
	private HealthManager healthManager;
	private ConsumptionManager consumptionManager;

	public ActionHandler(Console console, EntityManager entityManager, EntityFinder entityFinder, PlaneManager planeManager,
			CraftingManager craftingManager, TagHandler tagHandler, CombatManager combatManager, HealthManager healthManager,
			ConsumptionManager consumptionManager) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.craftingManager = craftingManager;
		this.tagHandler = tagHandler;
		this.combatManager = combatManager;
		this.healthManager = healthManager;
		this.consumptionManager = consumptionManager;
	}

	public Entity executeAction(Player player, Action action, Entity playerEntity) {
		Entity authoritativePlayerEntity = entityManager.getEntity(player);
		if (authoritativePlayerEntity != null) {
			playerEntity = authoritativePlayerEntity;
		}
		if (!healthManager.canPerformActions(playerEntity)) return playerEntity;

		if (action.getActionType() != ActionType.Interact) return playerEntity;
		Entity interactingEntity = entityManager.getEntity(action.getInteractingEntityId());
		
		switch(action.getInteractionType().getNumber()) {
			case(InteractionType.PickUp_VALUE):
				
				if (interactingEntity == null) {
					console.print("Warning: Null interaction entity");
					break;
				}
				if (!interactingEntity.getIsItem() || !isInRange(playerEntity, interactingEntity)) break;
				
				if (playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) {
					Entity heldItem = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
					if (heldItem == null) break;
					
					// check for same item and stacking
					if (interactingEntity.getName().equals(heldItem.getName()) && heldItem.getStackable() && interactingEntity.getId() != heldItem.getId()) {
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
								.clearVelocity()
								.build();
						
						entityManager.updateEntity(heldItem);
					}
				}
				else { 
					// Normal pickup
					interactingEntity = interactingEntity.toBuilder()
							.setMap(0)
							.clearVelocity()
							.build();
					
					entityManager.updateEntity(interactingEntity);
			
					playerEntity = playerEntity.toBuilder()
						.putInventorySlots(playerEntity.getSelectedSlot(), interactingEntity.getId())
						.build();
				}
				
				break;
				
			case(InteractionType.Drop_VALUE):
				
				if (!playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) break;
				if (!isValidDropPosition(playerEntity, action.getInteractionPosition())) break;
				
				Entity item = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
				if (item == null) break;
				item = item.toBuilder()
					.setPosition(action.getInteractionPosition())
					.setMap(playerEntity.getMap())
					.setVelocity(playerEntity.getVelocity())
					.build();
				entityManager.updateEntity(item);
			
				playerEntity = playerEntity.toBuilder()
					.removeInventorySlots(playerEntity.getSelectedSlot())
					.build();
				
				break;
			case(InteractionType.DropOne_VALUE):
				
				if (!playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) break;
				if (!isValidDropPosition(playerEntity, action.getInteractionPosition())) break;
				
				Entity heldItem = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
				if (heldItem == null) break;
				
				// item is not stackable or there is just one
				if (!heldItem.getStackable() || heldItem.getAmount() == 1) {
					heldItem = heldItem.toBuilder()
							.setPosition(action.getInteractionPosition())
							.setMap(playerEntity.getMap())
							.setVelocity(playerEntity.getVelocity())
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
							.setVelocity(playerEntity.getVelocity())
							.setId(entityManager.reserveNewEntityId())
							.build();
					entityManager.updateEntity(clonedItem);
							
				}
				
				break;
			case(InteractionType.Craft_VALUE):
				if (!isInRange(playerEntity, interactingEntity)) break;
				playerEntity = craftingManager.attemptCraftingRecipe(playerEntity, interactingEntity);
				break;
			case(InteractionType.Hit_VALUE):
				if (!isInRange(playerEntity, interactingEntity)) break;
				combatManager.attemptToDamage(playerEntity, interactingEntity);
				playerEntity = entityManager.getEntity(player);
				break;
			case(InteractionType.Standard_VALUE):
				if (!isInRange(playerEntity, interactingEntity)) break;
				playerEntity = tagHandler.interact(playerEntity, interactingEntity);
				break;
			case(InteractionType.Consume_VALUE):
				if (!playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) break;
				heldItem = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
				if (heldItem == null || !heldItem.getConsumable()) break;
				consumptionManager.consume(heldItem, playerEntity);
				playerEntity = entityManager.getEntity(player);
				break;
			case(InteractionType.Inventory_VALUE):
				
				// is holding item?
				if (playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) {
					
					// handle put case
					if (!playerEntity.containsInventorySlots(action.getSlotName())) {
						int id = playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot());
						
						playerEntity = playerEntity.toBuilder()
								.removeInventorySlots(playerEntity.getSelectedSlot())
								.putInventorySlots(action.getSlotName(), id)
								.build();
					}
					else {
						heldItem = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
						Entity slotItem = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(action.getSlotName()));
						
						System.out.println("held name: "+heldItem.getName());
						System.out.println("slotItem name: "+slotItem.getName());
						
						// check for same item and stacking
						if (slotItem.getName().equals(heldItem.getName()) && heldItem.getStackable() && slotItem.getId() != heldItem.getId()) {
							int newAmount = heldItem.getAmount() + slotItem.getAmount(); // for held item
							int leftOver = newAmount - 30; // for interacting entity
							newAmount = Math.min(newAmount, 30);
							
							if (leftOver < 1) {
								entityManager.removeEntity(slotItem);
								playerEntity = playerEntity.toBuilder()
										.removeInventorySlots(action.getSlotName())
										.build();
							}
							else {
								slotItem = slotItem.toBuilder()
										.setAmount(leftOver)
										.build();
								entityManager.updateEntity(slotItem);
							}
							
							heldItem = heldItem.toBuilder()
									.setAmount(newAmount)
									.clearVelocity()
									.build();
							
							entityManager.updateEntity(heldItem);
						}
					}
					
				}
				else {
					// not holding item >:(
					
					// is there somthing in the slot?
					if (playerEntity.containsInventorySlots(action.getSlotName())) {
						int id = playerEntity.getInventorySlotsMap().get(action.getSlotName());
						
						// dropping pockets TODO: fixed this jerry rigged system for mod support
						if (action.getSlotName().equals("pants")) {
							dropSlot(playerEntity,"pocket1");
							dropSlot(playerEntity,"pocket2");
							playerEntity = playerEntity.toBuilder()
									.removeInventorySlots("pocket1")
									.removeInventorySlots("pocket2")
									.build();
						}
						
						playerEntity = playerEntity.toBuilder()
								.removeInventorySlots(action.getSlotName())
								.putInventorySlots(playerEntity.getSelectedSlot(), id)
								.build();
					}
				}
				
		}
		
		
		return playerEntity;
	}
	
	// TODO remove this garbage
	private void dropSlot(Entity entity, String slot) {
		if (entity.containsInventorySlots(slot)) {
			Entity item = entityManager.getEntity(entity.getInventorySlotsMap().get(slot));
			item = item.toBuilder()
					.setMap(entity.getMap())
					.setPosition(entity.getPosition())
					.build();
			
			entityManager.updateEntity(item);
		}
	}

	private static boolean isInRange(Entity player, Entity target) {
		if (player == null || target == null || player.getMap() != target.getMap()) return false;
		double reach = player.getReach() > 0 ? player.getReach() : 1.5;
		return VectorMath.distanceSquared(player.getPosition(), target.getPosition()) <= reach * reach;
	}

	private static boolean isValidDropPosition(Entity player, Vector position) {
		if (position == null || !Float.isFinite(position.getX()) || !Float.isFinite(position.getY())) return false;
		double reach = player.getReach() > 0 ? player.getReach() : 1.5;
		return VectorMath.distanceSquared(player.getPosition(), position) <= reach * reach;
	}
}
