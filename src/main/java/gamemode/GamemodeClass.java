package gamemode;

import org.json.JSONObject;

import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import main.Console;
import plane.PlaneManager;
import tag.TagHandler;

public class GamemodeClass {

	protected Console console;
	protected EntityManager entityManager;
	protected EntityFinder entityFinder;
	protected PlaneManager planeManager;
	protected GamemodeManager gamemodeManager;
	protected JSONObject gamemode;
	protected AssetManager assetManager;
	protected TagHandler tagHandler;
	
	public GamemodeClass(Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager, GamemodeManager gamemodeManager, AssetManager assetManager, TagHandler tagHandler,  String ... arguments) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
		this.gamemodeManager = gamemodeManager;
		this.assetManager = assetManager;
		this.gamemode = gamemodeManager.getGamemode();
		this.tagHandler = tagHandler;
	}
	
	/**
	 * @return The name of this gamemode
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
}
