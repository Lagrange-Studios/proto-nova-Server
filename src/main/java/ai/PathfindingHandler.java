package ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import entity.EntityFinder;
import entity.EntityManager;
import main.Server;
import protonova.protobuf.VectorProto.Vector;

public class PathfindingHandler {
	private ConcurrentHashMap<Integer,Agent> agents = new ConcurrentHashMap<>();
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private Server server;
	
	public PathfindingHandler(EntityManager entityManager, EntityFinder EntityFinder, Server server) {
		this.entityManager = entityManager;
		this.entityFinder = EntityFinder;
		this.server = server;
	}
	
	public void tick() {
		for (int id : agents.keySet()) {
			Agent agent = agents.get(id);
			
			agent.tick();
			
			if (agent.isCompleted()) agents.remove(id);
		}
	}
	
	public void pathTo(int id, Vector position) {
		if (!agents.contains(id)) {
			Agent agent = new Agent(id,position,entityManager,entityFinder,server);
			
			agents.put(id, agent);
		}
	}
	
	public boolean hasAgent(int id) {
		return agents.contains(id);
	}
	
}
