package tag;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
	private HashMap<String, Class<?>> tagToClass;
	private Server server; 
	private int tickCount = 0;

	public TagHandler(Server server, EntityManager entityManager) {
		this.server = server;
		this.entityManager = entityManager;
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
					try {
						Class<?> tagClass = tagToClass.get(tag);
						tagClass.getDeclaredMethod("tick",TagHandler.class, Entity.class).invoke(tagClass, this, entity);
						if (secondTick) tagClass.getMethod("secondTick", Entity.class);
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
						e.printStackTrace();
					}
				}
				else tagToEntities.get(tag).remove(entityId);
			}
		}
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
		    	Class<?> staticClass = classInfo.loadClass();
		    	String className = staticClass.getName();
		    	className = className.substring(className.indexOf('.')+1);
		    	
		    	if (className.equals("TagHandler") || className.equals("TagClass")) continue;
		    		
		    	try {
					tagToClass.put((String) staticClass.getField("tag").get(classInfo), staticClass);
					System.out.println(staticClass.getField("tag").get(classInfo));
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
						| SecurityException e) {
					System.err.println("Error for class: "+staticClass.getName());
					e.printStackTrace();
				}
		    }
		}
	}
	
}
