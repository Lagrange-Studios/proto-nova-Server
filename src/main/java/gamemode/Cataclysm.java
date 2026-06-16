package gamemode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashMap;

import cataclysm.CataclysmClass;
import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import main.Console;
import plane.PlaneManager;
import tag.TagHandler;
import util.Random;

public class Cataclysm extends GamemodeClass {
	

	private HashMap<String, Class> nameToStaticCataclysm = new HashMap<>();
	private CataclysmClass currentCataclysm;
	private final int winCheckIntervalSeconds = 60;
	private int winCheckInterval = 0;
	
	/**
	 * Cataclysm gamemode consists of a random cataclysm that occurs throughout the time of the server
	 * 
	 * state1: peace, the cataclysm does not act and the players have time to build
	 * state2: intro, the cataclysm begins to affect the world in noticeable ways
	 * state3: end, the cataclysm becomes full force and the effects are felt across the world
	 */
	public Cataclysm(Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager, GamemodeManager gamemodeManager, AssetManager assetManager, TagHandler tagHandler,  String ... arguments) {
		super(console, entityManager, entityFinder, planeManager, gamemodeManager, assetManager, tagHandler, arguments);
		
		loadAllloadCataclysms();
		
		// validate gamemode
		if (!gamemode.has("cataclysmState")) gamemode.put("cataclysmState", "peaceful");
		if (!gamemode.has("cataclysm")) {
			String[] keyArray = nameToStaticCataclysm.keySet().toArray(new String[0]);
			String randomKey = keyArray[Random.randomInt(0, keyArray.length-1)];

			System.out.println("[Cataclysm] random key: "+randomKey);
			gamemode.put("cataclysm", randomKey);
		}
		
		loadCataclysm(gamemode.getString("cataclysm"),arguments);
	}

	public static String getName() {
		return "cataclysm";
	}
	
	public void tick() {
		currentCataclysm.tick();
	}
	
	public void secondTick() {
		currentCataclysm.secondTick();
		
		winCheckInterval++;
		if (winCheckInterval >= winCheckIntervalSeconds) {
			winCheckInterval = 0;
			
			String winner = currentCataclysm.getWinner();
			
			if (winner != null) {
				gamemode.put("gameOver", true);
				gamemode.put("winner", winner);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadCataclysm(String cataclysmName, String ... arguments) {
		if (!nameToStaticCataclysm.containsKey(cataclysmName)) {
			console.print("Error: Cataclysm "+cataclysmName+" not found");
			System.err.println("Error: Cataclysm "+cataclysmName+" not found");
		}
		else {
			try {
				currentCataclysm = (CataclysmClass) nameToStaticCataclysm.get(cataclysmName).getDeclaredConstructor(
						Console.class,EntityManager.class,EntityFinder.class,PlaneManager.class,GamemodeManager.class,AssetManager.class,Cataclysm.class,TagHandler.class,String[].class
						).newInstance(console,entityManager,entityFinder,planeManager,gamemodeManager,assetManager,this,tagHandler,arguments);
				
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadAllloadCataclysms() {
		try (ScanResult scanResult = new ClassGraph()
		        .acceptPackages("cataclysm")
		        .scan()) {
		    for (ClassInfo classInfo : scanResult.getAllClasses()) {
		    	Class<?> staticClass = classInfo.loadClass();
		    	String className = staticClass.getName();
		    	className = className.substring(className.indexOf('.')+1);
		    	
		    	if (className.equals("CataclysmClass")) continue;
		    		
		    	try {
		    		//System.out.println((String) staticClass.getMethod("getName").invoke(staticClass));
		    		nameToStaticCataclysm.put((String) staticClass.getMethod("getName").invoke(staticClass), staticClass);
				} catch (IllegalArgumentException | SecurityException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					System.err.println("Error for class: "+staticClass.getName());
					e.printStackTrace();
				}
		    }
		}
	}
}
