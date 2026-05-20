package gamemode;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.json.JSONObject;

import entity.EntityFinder;
import entity.EntityManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import main.Console;
import plane.PlaneManager;
import tag.TagClass;

public class GamemodeManager {

	private Console console;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private PlaneManager planeManager;
	private JSONObject gamemode;
	private HashMap<String, Class> nameToGamemode = new HashMap<>();

	public GamemodeManager(Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager, JSONObject gamemode) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.gamemode = gamemode;
		
		loadAllGamemodes();
	}
	
	public void tickGamemode() {
		
	}
	
	public JSONObject getGamemode() {
		return gamemode;
	}

	private void loadAllGamemodes() {
		try (ScanResult scanResult = new ClassGraph()
		        .acceptPackages("gamemode")
		        .scan()) {
		    for (ClassInfo classInfo : scanResult.getAllClasses()) {
		    	Class<?> staticClass = classInfo.loadClass();
		    	String className = staticClass.getName();
		    	className = className.substring(className.indexOf('.')+1);
		    	
		    	if (className.equals("GamemodeManager")) continue;
		    		
		    	try {
		    		//System.out.println((String) staticClass.getField("gamemodeName").get(staticClass));
			    	nameToGamemode.put((String) staticClass.getField("gamemodeName").get(staticClass), staticClass);
				} catch (IllegalArgumentException | SecurityException | IllegalAccessException | NoSuchFieldException e) {
					System.err.println("Error for class: "+staticClass.getName());
					e.printStackTrace();
				}
		    }
		}
	}
}
