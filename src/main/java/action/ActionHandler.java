package action;

import java.util.HashMap;

import entity.EntityFinder;
import entity.EntityManager;
import main.Console;
import protonova.protobuf.ActionProto.Action;
import protonova.protobuf.ActionProto.ActionType;
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
		console.print(playerEntity.getSelectedSlot());
		
		for (String slotName : playerEntity.getInventorySlotsMap().keySet()) {
			System.out.println(slotName);
		}
		
		if (action.getActionType() == ActionType.Interact) {
			Entity interactingEntity = entityManager.getEntity(action.getInteractingEntityId());

			System.out.println(interactingEntity == null);
			System.out.println(playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot()));
			if (interactingEntity != null && interactingEntity.getIsItem()) {
				if (playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) {
					
				}
				else {
					interactingEntity = interactingEntity.toBuilder()
							.setMap(0)
							.build();
					entityManager.updateEntity(interactingEntity);
					
					playerEntity = playerEntity.toBuilder()
							.putInventorySlots(playerEntity.getSelectedSlot(), interactingEntity.getId())
							.build();
					
					
				}
			}
			else if (interactingEntity == null && playerEntity.getInventorySlotsMap().containsKey(playerEntity.getSelectedSlot())) {
				Entity item = entityManager.getEntity(playerEntity.getInventorySlotsMap().get(playerEntity.getSelectedSlot()));
				item = item.toBuilder()
						.setPosition(action.getInteractionPosition())
						.setMap(playerEntity.getMap())
						.build();
				
				playerEntity = playerEntity.toBuilder()
						.removeInventorySlots(playerEntity.getSelectedSlot())
						.build();
				
			}
		}
		return playerEntity;
	}
}
