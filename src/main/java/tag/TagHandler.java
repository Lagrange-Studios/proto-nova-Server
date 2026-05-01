package tag;

import java.util.ArrayList;
import java.util.HashMap;

import entity.EntityManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import main.Server;
import protonova.protobuf.EntityProto.Entity;

public class TagHandler {
	
	private EntityManager entityManager;
	private HashMap<String, ArrayList<Integer>> tagToEntities;
	private HashMap<String, Class> tagToClass;
	private Server server; 
	private int tickCount = 0;

	public TagHandler(Server server, EntityManager entityManager) {
		this.server = server;
		this.entityManager = entityManager;
		tagToEntities = new HashMap<>();
		tagToClass = new HashMap<>();
		loadAllTagEntities();
		loadAllTagClasses();
	}
	
	public void addEntity(Entity entity) {
		if (entity.getTagsCount() > 0) {
			for (String tag : entity.getTagsList()) {
				if (!tagToEntities.containsKey(tag)) tagToEntities.put(tag, new ArrayList<>());
				 tagToEntities.get(tag).add(entity.getId());
			}
		}
	}

	public void tick() {
		boolean secondTick = false;
		
		if (tickCount >= server.TPS) {
			secondTick = true;
			tickCount = 0;
		}
		else tickCount++;
		
		
	}
	
	/**
	 * Shorthand for update entity
	 */
	protected void updateEntity(Entity entity) {
		entityManager.updateEntity(entity);
	}
	
	protected EntityManager getEntityManager() {
		return entityManager;
	}
	
	private void loadAllTagEntities() {
		for (Entity entity : entityManager.getAllEntities().values()) {
			addEntity(entity);
		}
	}
	
	private void loadAllTagClasses() {
		try (ScanResult scanResult = new ClassGraph()
		        .acceptPackages("tag")
		        .scan()) {
		    for (ClassInfo classInfo : scanResult.getAllClasses()) {
		    	tagToClass.put(classInfo.getName(), classInfo.getClass());
		    }
		}
	}
	
}
