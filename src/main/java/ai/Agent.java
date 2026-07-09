package ai;

import entity.EntityManager;
import main.Server;
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
	private boolean completed = false;
	
	private Vector goalPosition;
	private int entityGoal = 0;
	
	public Agent(int entityId, EntityManager entityManager, EntityFinder entityFinder, Server server) {
		this.entityId = entityId;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.server = server;
	}
	
	public void tick() {
		Entity entity = entityManager.getEntity(entityId);
		
		// checking to see if our entity still exits and is alive
		if (entity != null) {
			Vector goal = getGoal();
			
			Vector difference = VectorMath.minus(entity.getPosition(), goal);
			difference = VectorMath.unitVector(difference);
			
			difference = Vector.newBuilder()
					.setX((float) (difference.getX()*entity.getSpeed()/server.TPS))
					.setY((float) (difference.getY()*entity.getSpeed()/server.TPS))
					.build();
			
			difference = VectorMath.add(entity.getPosition(), difference);
			
			entity = entity.toBuilder()
					.setPosition(difference)
					.build();
			
			entityManager.updateEntity(entity);
			
			if (VectorMath.distanceSquared(difference, goal) <= minimumDistanceSquared)	completed = true;
			
		}
		else {
			completed = true;
		}
		
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
	
	private Vector getGoal() {
		if (entityGoal != 0 && entityManager.entityExist(entityGoal)) {
			return entityManager.getEntity(entityGoal).getPosition();
		}
		else return goalPosition;
	}
}
