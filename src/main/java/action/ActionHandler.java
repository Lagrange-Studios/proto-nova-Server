package action;

import java.util.HashMap;

import entity.EntityFinder;
import entity.EntityManager;
import main.Console;
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
	private HashMap<Integer, Plane> planes;

	public ActionHandler(Console console, EntityManager entityManager, EntityFinder entityFinder, HashMap<Integer, Plane> planes) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planes = planes;
	}

	public Entity executeAction(Player player, Action action) {
		Entity playerEntity = entityManager.getEntity(player);
		
		if (action.getActionType() != ActionType.Interact) return playerEntity;
		
		switch(action.getInteractionType().getNumber()) {
			case(InteractionType.PickUp_VALUE):
				
				if (action.getInteractingEntityId() == 0) break;
				if (playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) break; //TODO: add stacking
				
				Entity interactingEntity = entityManager.getEntity(action.getInteractingEntityId());
				
				if (!interactingEntity.getIsItem()) break;
				
				interactingEntity = interactingEntity.toBuilder()
						.setMap(0)
						.build();
				
				entityManager.updateEntity(interactingEntity);
		
				playerEntity = playerEntity.toBuilder()
					.putInventorySlots(playerEntity.getSelectedSlot(), interactingEntity.getId())
					.build();
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
