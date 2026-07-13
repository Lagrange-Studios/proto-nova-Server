 package tag;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ai.PathfindingHandler;
import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import health.CombatManager;
import health.HealthManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import main.Console;
import main.Server;
import plane.PlaneManager;
import protonova.protobuf.EntityProto.Entity;

public class TagHandler {
	
	private EntityManager entityManager;
	private ConcurrentHashMap<String, Set<Integer>> tagToEntities;
	private ConcurrentHashMap<String, TagClass> tagToClass;
	private Server server; 
	private AssetManager assetManager;
	private EntityFinder entityFinder;
	private PlaneManager planeManager;
	private CombatManager combatManager;
	private PathfindingHandler pathfindingHandler;
	private HealthManager healthManager;
	private static final int entitiesPerThread = 200;

	public TagHandler(Server server, EntityManager entityManager, AssetManager assetManager, EntityFinder entityFinder, PlaneManager planeManager, CombatManager combatManager, PathfindingHandler pathfindingHandler, HealthManager healthManager) {
		this.server = server;
		this.entityManager = entityManager;
		this.assetManager = assetManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.combatManager = combatManager;
		this.pathfindingHandler = pathfindingHandler;
		this.healthManager = healthManager;
		tagToEntities = new ConcurrentHashMap<>();
		tagToClass = new ConcurrentHashMap<>();
		loadAllTagClasses();
	}
	
	
	public void addEntity(Entity entity) {
		if (entity.getTagsCount() > 0) {
			for (String tag : entity.getTagsList()) {
				if (!tagToClass.containsKey(tag)) {
					//String errorMessage = "Error: entity named "+entity.getName()+" has a tag called "+tag+" without a matching class";
					//System.err.print(errorMessage);
					//server.console.print(errorMessage);
					continue;
				}
				
				if (!tagToEntities.containsKey(tag)) tagToEntities.put(tag, ConcurrentHashMap.newKeySet());
				 tagToEntities.get(tag).add(entity.getId());
			}
		}
	}
	
	public void updateEntity(Entity oldEntity, Entity newEntity) {
		if (oldEntity.getTagsCount() > 0) {
			for (String oldTag : oldEntity.getTagsList()) {
				if (tagToEntities.containsKey(oldTag) && tagToEntities.get(oldTag).contains(oldEntity.getId())) tagToEntities.get(oldTag).remove(oldEntity.getId());
			}
		}
		addEntity(newEntity);
	}
	
	public void removeEntity(Entity entity) {
		for (Set<Integer> set : tagToEntities.values()) {
			if (set.contains(entity.getId()))
				set.remove(entity.getId());
		}
	}

	public void tick() {
		ArrayList<Thread> threads = new ArrayList<>();
		boolean secondTick = server.globalTicks % server.TPS == 0;
		
		if (secondTick) {
			for (String tag : tagToEntities.keySet()) {
				TagClass tagClass = tagToClass.get(tag);
				Integer[] entityIds = tagToEntities.get(tag).toArray(new Integer[0]);
				
				for (int i=0; i < entityIds.length; i += entitiesPerThread) {
					threads.add(secondTickEntities(entityIds,tagClass,i,Math.min(i+entitiesPerThread, entityIds.length)));
				}
			}
		}
		else {
			for (String tag : tagToEntities.keySet()) {
				TagClass tagClass = tagToClass.get(tag);
				Integer[] entityIds = tagToEntities.get(tag).toArray(new Integer[0]);
				
				for (int i=0; i < entityIds.length; i += entitiesPerThread) {
					threads.add(tickEntities(entityIds,tagClass,i,Math.min(i+entitiesPerThread, entityIds.length)));
				}
			}
		}
		
		
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Thread tickEntities(Integer[] ids, TagClass tagClass, int start, int end) {
		Thread thread = new Thread(() -> {
			for (int index = start;index<end;index++) {
				long started = System.nanoTime();
				
				Entity entity = entityManager.getEntity(ids[index]);
				
				if (entity != null)
					tagClass.tick(this,entity);
				
				if (server.getDiagnostics() != null) {
					server.getDiagnostics().recordEntityCpu(ids[index], System.nanoTime() - started);
				}
			}
		});
		thread.start();
		
		return thread;
	}
	
	private Thread secondTickEntities(Integer[] ids, TagClass tagClass, int start, int end) {
		Thread thread = new Thread(() -> {
			for (int index = start;index<end;index++) {
				long started = System.nanoTime();
				Entity entity = entityManager.getEntity(ids[index]);
				
				if (entity != null) {
					tagClass.tick(this,entity);
					tagClass.secondTick(this,entity);
				}
				server.getDiagnostics().recordEntityCpu(ids[index], System.nanoTime() - started);
			}
		});
		thread.start();
		
		return thread;
	}
	
	public Entity interact(Entity interactingEntity, Entity tagEntity) {
		
		for (String tag : tagEntity.getTagsList()) {
			if (!tagToClass.containsKey(tag)) continue;
			interactingEntity = tagToClass.get(tag).interact(this, interactingEntity, tagEntity);
		}
		
		return interactingEntity;
	}
	
	public int getTagAmount(String tagName) {
		if (tagToEntities.containsKey(tagName)) return tagToEntities.get(tagName).size();
		else return 0;
	}
	
	/**
	 * Shorthand for update entity
	 */
	public void updateEntity(Entity entity) {
		entityManager.updateEntity(entity);
	}
	
	public CombatManager getCombatManager() {
		return combatManager;
	}
	
	public PlaneManager getPlaneManager() {
		return planeManager;
	}
	
	public AssetManager getAssetManager() {
		return assetManager;
	}
	
	public EntityManager getEntityManager() {
		return entityManager;
	}
	
	public EntityFinder getEntityFinder() {
		return entityFinder;
	}
	
	public Console getConsole() {
		return server.console;
	}
	
	public Server getServer() {
		return server;
	}
	
	public int getTPS() {
		return server.TPS;
	}
	
	public PathfindingHandler getPathfindingHandler() {
		return pathfindingHandler;
	}
	
	public HealthManager getHealthManager() {
		return healthManager;
	}
	
	public void loadAllTagEntities() {
		for (Entity entity : entityManager.getAllEntities().values()) {
			addEntity(entity);
		}
	}
	
	private void loadAllTagClasses() {
		try (ScanResult scanResult = new ClassGraph()
		        .acceptPackages("tag")
		        .scan()) {
		    for (ClassInfo classInfo : scanResult.getAllClasses()) {
		    	Class<?> staticClass = classInfo.loadClass();
		    	String className = staticClass.getName();
		    	className = className.substring(className.indexOf('.')+1);
		    	
		    	if (className.equals("TagHandler") || className.equals("TagClass")) continue;
		    		
		    	try {
			    	TagClass newTagClass = (TagClass) staticClass.getDeclaredConstructor().newInstance();
			    	
					tagToClass.put(newTagClass.getTag(), newTagClass);
					//System.out.println(className);
					//System.out.println(newTagClass.getTag());
				} catch (IllegalArgumentException | IllegalAccessException | SecurityException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
					System.err.println("Error for class: "+staticClass.getName());
					e.printStackTrace();
				}
		    }
		}
	}
	
}
