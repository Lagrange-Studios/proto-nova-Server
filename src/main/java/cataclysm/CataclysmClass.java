package cataclysm;

import org.json.JSONObject;

import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import gamemode.Cataclysm;
import gamemode.GamemodeManager;
import main.Console;
import plane.PlaneManager;

public class CataclysmClass {

	protected Console console;
	protected EntityManager entityManager;
	protected EntityFinder entityFinder;
	protected PlaneManager planeManager;
	protected GamemodeManager gamemodeManager;
	protected Cataclysm cataclysm;
	protected JSONObject gamemode;
	protected AssetManager assetManager;
	
	protected int introStartTime;
	protected int endStartTime;
	
	public CataclysmClass(Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager, GamemodeManager gamemodeManager, AssetManager assetManager, Cataclysm cataclysm, String ... arguments) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.gamemodeManager = gamemodeManager;
		this.cataclysm = cataclysm;
		this.assetManager = assetManager;
		this.gamemode = gamemodeManager.getGamemode();
	}
	
	/**
	 * @return The name of this cataclysm
	 */
	public static String getName() {
		return "null";
	}
	
	/**
	 * occurs once per tick
	 */
	public void tick() {
		
	}
	
	/**
	 * occurs once per second
	 */
	public void secondTick() {
		
	}
	
	/**
	 * Checks wether to switch the state. This is not called automatically
	 */
	public void checkStateSwitch() {
		int time = (int) gamemodeManager.getGamemodeTimeSeconds();
		
		if (time > introStartTime) gamemode.put("cataclysmState", "intro");
		if (time > endStartTime) gamemode.put("cataclysmState", "end");
	}
}
