package gamemode;

import entity.EntityFinder;
import entity.EntityManager;
import main.Console;
import plane.PlaneManager;

public class Cataclysm extends GamemodeClass {
	
	public Cataclysm(Console console, EntityManager entityManager, EntityFinder entityFinder,
			PlaneManager planeManager) {
		super(console, entityManager, entityFinder, planeManager);
	}

	public String getName() {
		return "cataclysm";
	}
}
