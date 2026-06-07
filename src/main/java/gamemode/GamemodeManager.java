package gamemode;

import java.awt.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashMap;

import org.json.JSONObject;

import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import main.Console;
import main.Server;
import plane.PlaneManager;
import tag.TagClass;

public class GamemodeManager {

	private Console console;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private PlaneManager planeManager;
	private JSONObject gamemode;
	private HashMap<String, Class> nameToStaticGamemode = new HashMap<>();
	private GamemodeClass currentGamemode;
	private Server server;
	private AssetManager assetManager;

	public GamemodeManager(Server server, Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager, AssetManager assetManager, JSONObject gamemode) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.gamemode = gamemode;
		this.server = server;
		this.assetManager = assetManager;
		
		loadAllGamemodes();
		loadGamemode(gamemode.getString("name"));
	}
	
	public void tickGamemode() {
		if (!gamemode.getBoolean("gameover")) {
			int newTime = gamemode.getInt("time");
			newTime++;
			
			gamemode.put("time", newTime);
			
			currentGamemode.tick();
			if (server.globalTicks % server.TPS == 0) currentGamemode.secondTick();
		}
	}
	
	public JSONObject getGamemode() {
		return gamemode;
	}

	public float getGamemodeTimeTicks() {
		return gamemode.getInt("time");
	}
	
	public float getGamemodeTimeSeconds() {
		return (float) (gamemode.getInt("time")/server.TPS);
	}
	
	@SuppressWarnings("unchecked")
	public void loadGamemode(String gamemodeName, String ... arguments) {
		if (!nameToStaticGamemode.containsKey(gamemodeName)) {
			console.print("Error: Gamemode "+gamemodeName+" not found");
			System.err.println("Error: Gamemode "+gamemodeName+" not found");
		}
		else {
			try {
				currentGamemode = (GamemodeClass) nameToStaticGamemode.get(gamemodeName).getDeclaredConstructor(
						Console.class,EntityManager.class,EntityFinder.class,PlaneManager.class,GamemodeManager.class,AssetManager.class,String[].class
						).newInstance(console,entityManager,entityFinder,planeManager,this,assetManager,arguments);
				
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadAllGamemodes() {
		try (ScanResult scanResult = new ClassGraph()
		        .acceptPackages("gamemode")
		        .scan()) {
		    for (ClassInfo classInfo : scanResult.getAllClasses()) {
		    	Class<?> staticClass = classInfo.loadClass();
		    	String className = staticClass.getName();
		    	className = className.substring(className.indexOf('.')+1);
		    	
		    	if (className.equals("GamemodeManager") || className.equals("GamemodeClass")) continue;
		    		
		    	try {
		    		//System.out.println((String) staticClass.getMethod("getName").invoke(staticClass));
		    		/*for (Parameter param : staticClass.getDeclaredConstructors()[0].getParameters()) {
		    			System.out.println(param.getType().getName());
		    		}*/
		    		nameToStaticGamemode.put((String) staticClass.getMethod("getName").invoke(staticClass), staticClass);
				} catch (IllegalArgumentException | SecurityException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					System.err.println("Error for class: "+staticClass.getName());
					e.printStackTrace();
				}
		    }
		}
	}
}
