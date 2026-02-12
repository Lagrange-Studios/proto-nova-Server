package action;

import java.util.HashMap;

import entity.EntityFinder;
import entity.EntityManager;
import main.Console;
import plane.PlaneManager;
import protonova.protobuf.ActionProto.Action;
import protonova.protobuf.ActionProto.ActionType;
import protonova.protobuf.ActionProto.InteractionType;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import socket.Player;

public class ActionHandler {

	private Console console;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private PlaneManager planeManager;

	public ActionHandler(Console console, EntityManager entityManager, EntityFinder entityFinder, PlaneManager planeManager) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
	}

	public Entity executeAction(Player player, Action action) {
		Entity playerEntity = entityManager.getEntity(player);
		
		if (action.getActionType() != ActionType.Interact) return playerEntity;
		
		switch(action.getInteractionType().getNumber()) {
			case(InteractionType.PickUp_VALUE):
				
				if (action.getInteractingEntityId() == 0) break;
				
				Entity interactingEntity = entityManager.getEntity(action.getInteractingEntityId());
				
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
		}
		
		
		return playerEntity;
	}
}
