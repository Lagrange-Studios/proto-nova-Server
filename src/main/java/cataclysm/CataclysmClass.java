package cataclysm;

import org.json.JSONObject;

import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import gamemode.Cataclysm;
import gamemode.GamemodeManager;
import main.Console;
import plane.PlaneManager;
import tag.TagHandler;

public class CataclysmClass {

	protected Console console;
	protected EntityManager entityManager;
	protected EntityFinder entityFinder;
	protected PlaneManager planeManager;
	protected GamemodeManager gamemodeManager;
	protected Cataclysm cataclysm;
	protected JSONObject gamemode;
	protected AssetManager assetManager;
	protected TagHandler tagHandler;
	
	protected int introStartTime;
	protected int endStartTime;
	
	public CataclysmClass(Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager, GamemodeManager gamemodeManager, AssetManager assetManager, Cataclysm cataclysm, TagHandler tagHandler, String ... arguments) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.gamemodeManager = gamemodeManager;
		this.cataclysm = cataclysm;
		this.assetManager = assetManager;
		this.gamemode = gamemodeManager.getGamemode();
		this.tagHandler = tagHandler;
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
	 * Checks whether to switch the state. This is not called automatically
	 */
	public void checkStateSwitch() {
		int time = (int) gamemodeManager.getGamemodeTimeSeconds();
		
		if (time > introStartTime) gamemode.put("cataclysmState", "intro");
		if (time > endStartTime) gamemode.put("cataclysmState", "end");
	}
	
	/**
	 * Returns the winner of the cataclysm
	 * @return players or the name of the cataclysm or if no one won yet returns null
	 */
	public String getWinner() {
		return null;
	}
}
