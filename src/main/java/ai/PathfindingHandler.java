package ai;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import entity.EntityFinder;
import entity.EntityManager;
import health.CombatManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import main.Server;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import tag.TagClass;

public class PathfindingHandler {
	private ConcurrentHashMap<Integer,Agent> agents = new ConcurrentHashMap<>();
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private Server server;
	private HashMap<String, AgentParameter> parameters = new HashMap<>();
	private CombatManager combatManager;
	
	public PathfindingHandler(EntityManager entityManager, EntityFinder EntityFinder, Server server, CombatManager combatManager) {
		this.entityManager = entityManager;
		this.entityFinder = EntityFinder;
		this.server = server;
		this.combatManager = combatManager;
		
		loadParameters();
	}
	
	public void tick() {
		for (int id : agents.keySet()) {
			Agent agent = agents.get(id);
			
			agent.tick();
			
			if (agent.isCompleted()) agents.remove(id);
		}
	}
	
	public void newAgent(int id, Vector position, String parameter) {
		if (!agents.contains(id)) {
			Agent agent = new Agent(id,entityManager,entityFinder,server,combatManager, getParameter(parameter));
			agent.setGoal(position);
			
			agents.put(id, agent);
		}
		else changeGoal(id, position);
	}
	
	public void newAgent(int id, int otherEntity, String parameter) {
		if (!agents.contains(id)) {
			Agent agent = new Agent(id,entityManager,entityFinder,server,combatManager, getParameter(parameter));
			agent.setGoal(otherEntity);
			
			agents.put(id, agent);
		}
		else changeGoal(id, otherEntity);
	}
	
	public void newAgent(int id, String parameter) {
		if (!agents.contains(id)) {
			Agent agent = new Agent(id,entityManager,entityFinder,server,combatManager, getParameter(parameter));
			
			agents.put(id, agent);
		}
	}
	
	public boolean hasAgent(int id) {
		return agents.contains(id);
	}
	
	public Agent getAgent(int id) {
		return agents.get(id);
	}
	
	public void changeGoal(int id, Vector position) {
		agents.get(id).setGoal(position);
	}
	
	public void changeGoal(int id, int entityId) {
		agents.get(id).setGoal(entityId);
	}

	public void removeEntity(Entity entity) {
		if (agents.contains(entity.getId()))
			agents.remove(entity.getId());
	}
	
	private AgentParameter getParameter(String name) {
		return parameters.get(name);
	}
	
	private void loadParameters() {
		try (ScanResult scanResult = new ClassGraph()
		        .acceptPackages("agentParameters")
		        .scan()) {
		    for (ClassInfo classInfo : scanResult.getAllClasses()) {
		    	Class<?> staticClass = classInfo.loadClass();
		    	String className = staticClass.getName();
		    	className = className.substring(className.indexOf('.')+1);
		    	
		    		
		    	try {
			    	AgentParameter newParameter = (AgentParameter) staticClass.getDeclaredConstructor().newInstance();
			    	
			    	parameters.put(newParameter.getName(), newParameter);
				} catch (IllegalArgumentException | IllegalAccessException | SecurityException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
					System.err.println("Error for agent parameter: "+staticClass.getName());
					e.printStackTrace();
				}
		    }
		}
	}

}
