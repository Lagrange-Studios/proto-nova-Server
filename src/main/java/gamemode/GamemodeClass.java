package gamemode;

import entity.EntityFinder;
import entity.EntityManager;
import main.Console;
import plane.PlaneManager;

public class GamemodeClass {

	@SuppressWarnings("unused")
	private Console console;
	@SuppressWarnings("unused")
	private EntityManager entityManager;
	@SuppressWarnings("unused")
	private EntityFinder entityFinder;
	@SuppressWarnings("unused")
	private PlaneManager planeManager;
	
	public GamemodeClass(Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager) {
		this.console = console;
		this.entityManager = entityManager;
		this.entityFinder = entityFinder;
		this.planeManager = planeManager;
	}
	
	/**
	 * @return The tag of this specific class that should be on entities
	 */
	public String getName() {
		return "null";
	}
	
	/**
	 * occurs once per tick
	 * @param isSecond only true once a second
	 */
	public void tick(boolean isSecond) {
		
	}
}
