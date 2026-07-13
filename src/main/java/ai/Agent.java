package ai;

import entity.EntityManager;
import health.CombatManager;
import main.Server;

import java.util.ArrayList;
import java.util.HashSet;

import collision.EntityCollision;
import entity.EntityFinder;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import util.VectorMath;

public class Agent {

	private static float minimumDistanceSquared = (float) Math.pow(0.1f, 2);
	
	private int entityId;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private Server server;
	private CombatManager combatManager;
	private boolean completed = false;
	private AgentParameter parameters;
	
	private Vector goalPosition = null;
	private int entityGoal = 0;


	
	public Agent(int entityId, EntityManager entityManager, EntityFinder entityFinder, Server server, CombatManager combatManager, AgentParameter parameters) {
		this.entityId = entityId;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.server = server;
		this.parameters = parameters;
		this.combatManager = combatManager;
	}
	
	/**
	 * Updates the agent which computates a linear path and moves the entity
	 * @param supplier a class that implements the {@link ai.AgentSupplier} interface
	 */
	public void tick() {
		Entity entity = entityManager.getEntity(entityId);
		
		// checking to see if our entity still exits and is alive
		if (entity != null && (!parameters.mustBeAlive() || parameters.mustBeAlive() && entity.getAlive())) {
			ArrayList<Entity> entities = entityFinder.getAllEntitiesInRadis(entity, parameters.getRange());
			
			Vector oldPosition = entity.getPosition();
			
			Vector goal = getGoal(entities);
			
			// math for direction and speed
			Vector difference = VectorMath.minus(goal, entity.getPosition());
			difference = VectorMath.unitVector(difference);
			
			difference = Vector.newBuilder()
					.setX((float) (difference.getX()*entity.getSpeed()/server.TPS))
					.setY((float) (difference.getY()*entity.getSpeed()/server.TPS))
					.build();
			
			difference = VectorMath.add(entity.getPosition(), difference);
			
			entity = entity.toBuilder()
					.setPosition(difference)
					.build();
			
			boolean revertPosition = false;
			
			// Collision and damage check
			for (Entity closeEntity : entities) {
				if (EntityCollision.checkCollision(closeEntity, entity)) {
					if (parameters.canDamage(closeEntity))
						combatManager.attemptToDamage(entity, closeEntity);
					
					if (closeEntity.getCanCollide())
						revertPosition = true;
				}
			}
			
			if (revertPosition)
				entity = entity.toBuilder()
					.setPosition(oldPosition)
					.build();
			
			
			entityManager.updateEntity(entity);
			double distanceSqaured = VectorMath.distanceSquared(difference, goal);
			
			// complete path
			if (distanceSqaured <= minimumDistanceSquared && parameters.closeOnPathEnd()) completed = true;
			
			// loose target
			if (distanceSqaured > Math.pow(parameters.getRange(),2)) setGoal(0);
			
		}
		else {
			completed = true;
		}
		
	}
	
	/**
	 * This method finds a goal based on the agentSupplier 
	 * @param supplier the parameters for finding a goal
	 */
	public void findGoal(ArrayList<Entity> closeEntities) {
		Entity entity = entityManager.getEntity(entityId);
		
		Entity closestTarget = null;
		double closestDistanceSquared = 0;
		
		for (Entity target : closeEntities) {
			if (parameters.canTarget(target)) {
				double distanceSquared = VectorMath.distance(entity.getPosition(), target.getPosition());
				
				if (closestTarget == null || closestDistanceSquared > distanceSquared) {
					closestTarget = target;
					closestDistanceSquared = distanceSquared;
				}
			}
		}
		
		if (closestTarget != null)
			setGoal(closestTarget.getId());
		else setGoal(entityId); // go to self
	}
	
	public void setGoal(int entityId) {
		this.entityGoal = entityId;
	}
	
	public void setGoal(Vector position) {
		this.goalPosition = position;
	}
	
	public boolean isCompleted() {
		return completed;
	}
	
	private Vector getGoal(ArrayList<Entity> closeEntities) {
		if (parameters.canFindNewTarget() && goalPosition == null && entityGoal == 0) {
			Thread thread = new Thread(() ->{findGoal(closeEntities);});
			thread.run();
		}
		
		if (entityGoal != 0 && entityManager.entityExist(entityGoal)) {
			return entityManager.getEntity(entityGoal).getPosition();
		}
		else return goalPosition;
	}
}
