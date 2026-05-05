package tag;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import entity.EntityManager;
import file.AssetManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import main.Console;
import main.Server;
import protonova.protobuf.EntityProto.Entity;

public class TagHandler {
	
	private EntityManager entityManager;
	private HashMap<String, ArrayList<Integer>> tagToEntities;
	private HashMap<String, TagClass> tagToClass;
	private Server server; 
	private int tickCount = 0;
	private AssetManager assetManager;

	public TagHandler(Server server, EntityManager entityManager, AssetManager assetManager) {
		this.server = server;
		this.entityManager = entityManager;
		this.assetManager = assetManager;
		tagToEntities = new HashMap<>();
		tagToClass = new HashMap<>();
		loadAllTagClasses();
		loadAllTagEntities();
	}
	
	public void addEntity(Entity entity) {
		if (entity.getTagsCount() > 0) {
			for (String tag : entity.getTagsList()) {
				if (!tagToClass.containsKey(tag)) {
					String errorMessage = "Error: entity named "+entity.getName()+" has a tag called "+tag+" without a matching class";
					System.err.print(errorMessage);
					server.console.print(errorMessage);
					continue;
				}
				
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
		
		for (String tag : tagToEntities.keySet()) {
			for (int entityId : tagToEntities.get(tag)) {
				Entity entity = entityManager.getEntity(entityId);
				
				if (entity != null) {
					TagClass tagClass = tagToClass.get(tag);
					tagClass.tick(this,entity);
					if (secondTick) tagClass.secondTick(this, entity);
				}
				else tagToEntities.get(tag).remove(entityId);
			}
		}
	}
	
	public Entity interact(Entity interactingEntity, Entity tagEntity) {
		
		for (String tag : tagEntity.getTagsList()) {
			interactingEntity = tagToClass.get(tag).interact(this, interactingEntity, tagEntity);
		}
		
		return interactingEntity;
	}
	
	/**
	 * Shorthand for update entity
	 */
	public void updateEntity(Entity entity) {
		entityManager.updateEntity(entity);
	}
	
	public AssetManager getAssetManager() {
		return assetManager;
	}
	
	public EntityManager getEntityManager() {
		return entityManager;
	}
	
	public Console getConsole() {
		return server.console;
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
