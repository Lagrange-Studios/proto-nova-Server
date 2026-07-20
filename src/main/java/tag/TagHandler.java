 package tag;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
import diagnostics.ResourceDiagnostics;
import plane.PlaneManager;
import protonova.protobuf.EntityProto.Entity;

public class TagHandler {
	
	private EntityManager entityManager;
	private ConcurrentHashMap<Integer,String[]> tickMap;
	private HashMap<Integer,ConcurrentHashMap<Integer,String[]>> secondTickMap;
	private ConcurrentHashMap<String, TagClass> tagToClass;
	private Server server; 
	private AssetManager assetManager;
	private EntityFinder entityFinder;
	private PlaneManager planeManager;
	private CombatManager combatManager;
	private PathfindingHandler pathfindingHandler;
	private HealthManager healthManager;

	public TagHandler(Server server, EntityManager entityManager, AssetManager assetManager, EntityFinder entityFinder, PlaneManager planeManager, CombatManager combatManager, PathfindingHandler pathfindingHandler, HealthManager healthManager) {
		this.server = server;
		this.entityManager = entityManager;
		this.assetManager = assetManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.combatManager = combatManager;
		this.pathfindingHandler = pathfindingHandler;
		this.healthManager = healthManager;
		tagToClass = new ConcurrentHashMap<>();

		tickMap = new ConcurrentHashMap<>();
		secondTickMap = new HashMap<>();
		
		//making intial tick values
		for (int i=0;i<server.TPS;i++) {
			secondTickMap.put(i, new ConcurrentHashMap<>());
		}
		
		loadAllTagClasses();
	}
	
	/**
	 * Updates wether the entity has tags to preform actions for
	 * should be called anytime the entity is updated from entity manager
	 * @param entity
	 */
	public void updateEntityTag(Entity entity) {
		removeEntity(entity);
		addEntity(entity);	
	}
	
	public void addEntity(Entity entity) {
		// array lists for collecting all tags with tick functions
		ArrayList<String> tick = new ArrayList<>();
		ArrayList<String> secondTick = new ArrayList<>();
		
		// looping through the entities tags to find the ones with tick functions
		for (String tag : entity.getTagsList()) {
			TagClass tagClass = tagToClass.get(tag);
			
			if (tagClass != null) {
				if (tagClass.hasTick()) tick.add(tag);
				if (tagClass.hasSecondTick()) secondTick.add(tag);
			}
			
		}
		
		// add the tick to the general map
		if (!tick.isEmpty()) tickMap.put(entity.getId(), tick.toArray(new String[0]));
		
		// find the smallest second tick map
		int smallestIndex = 0;
		int smallestSize = secondTickMap.get(0).size();
		
		for (int i=1;i<server.TPS;i++) {
			int size = secondTickMap.get(i).size();
			
			if (size < smallestSize) {
				smallestIndex = i;
				smallestSize = size;
			}
		}
		
		// add the entity and its tags to the second tick map
		if (!secondTick.isEmpty()) secondTickMap.get(smallestIndex).put(entity.getId(), secondTick.toArray(new String[0]));
	}
	
	public void removeEntity(Entity entity) {
		// remove from tick map
		tickMap.remove(entity.getId());
		
		// remove from second tick map
		for (int i=0;i<server.TPS;i++) {
			if (secondTickMap.get(i).remove(entity.getId()) != null) 
				break;
		}
	}

	public void tick() {
		ArrayList<Future<?>> threads = new ArrayList<>();
		
		// get the tick number for determining which group of second ticks to update
		int tickNumber = (int) (server.globalTicks % server.TPS);
		
		// get a total count of entities we are updating this tick to split them into threads
		int tickEntitiesCount = tickMap.size();
		// get all open threads
		int idleThreads = Math.max(1, server.getOpenThreads());
		
		// calcualte how many entities we will put in each thread
		int entitiesPerThread = (int)Math.ceil((double)tickEntitiesCount / idleThreads);
		entitiesPerThread = Math.max(1, entitiesPerThread);
		
		Integer[] tickKeyArray = tickMap.keySet().toArray(new Integer[0]);
		
		for (int i=0;i<tickEntitiesCount;i+=entitiesPerThread) {
			threads.add(tickEntities(tickKeyArray,i,i+entitiesPerThread,tickEntitiesCount));
		}
		
		for (Future<?> thread : threads) {
			try {
				thread.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		threads.clear();
		
		// repeat same process for this tick second tick updates
		int secondTickEntitiesCount = secondTickMap.get(tickNumber).size();
		
		int secondEntitiesPerThread = (int)Math.ceil((double)secondTickEntitiesCount / idleThreads);
		secondEntitiesPerThread = Math.max(1, secondEntitiesPerThread);
		
		Integer[] secondTickKeyArray = secondTickMap.get(tickNumber).keySet().toArray(new Integer[0]);
		
		for (int i=0;i<secondTickEntitiesCount;i+=secondEntitiesPerThread) {
			threads.add(secondTickEntities(secondTickKeyArray,i,i+secondEntitiesPerThread,secondTickEntitiesCount,tickNumber));
		}
		
		for (Future<?> thread : threads) {
			try {
				thread.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		threads.clear();
	}
	
	private Future<?> tickEntities(Integer[] ids,int start, int end, int tickEntitiesCount) {
		return server.threadPool.submit(() -> {
			for (int keyIndex=start;keyIndex<Math.min(tickEntitiesCount, end);keyIndex++) {
				
				int key = ids[keyIndex];
				for (String tag : tickMap.get(key)) {
					tagToClass.get(tag).tick(this, entityManager.getEntity(key));
				}
			}
		});
	}
	
	private Future<?> secondTickEntities(Integer[] ids,int start, int end, int tickEntitiesCount,int tickIndex) {
		return server.threadPool.submit(() -> {
			try {
				for (int keyIndex=start;keyIndex<Math.min(tickEntitiesCount, end);keyIndex++) {
					
					int key = ids[keyIndex];
					for (String tag : secondTickMap.get(tickIndex).get(key)) {
						tagToClass.get(tag).secondTick(this, entityManager.getEntity(key));
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	public Entity interact(Entity interactingEntity, Entity tagEntity) {
		
		for (String tag : tagEntity.getTagsList()) {
			if (!tagToClass.containsKey(tag)) continue;
			interactingEntity = tagToClass.get(tag).interact(this, interactingEntity, tagEntity);
		}
		
		return interactingEntity;
	}
	
	public int getTagAmount(String tagName) {
		// TODO: fix this
		return 0;
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
