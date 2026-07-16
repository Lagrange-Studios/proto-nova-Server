package cataclysm;

import entity.EntityFinder;
import entity.EntityManager;
import file.AssetManager;
import gamemode.Cataclysm;
import gamemode.GamemodeManager;
import main.Console;
import plane.PlaneManager;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.TileProto.Tile;
import protonova.protobuf.VectorProto.Vector;
import tag.Fungus;
import tag.TagHandler;
import util.CoordinateConverter;
import util.Random;

public class Fungi extends CataclysmClass {
	
	private final int tilesPerIntroSpore = 30000;
	private final double FUNGUS_WIN_PERCENTAGE  = 0.02;
	private final double FUNGUS_LOOSE_PERCENTAGE  = 0.001;
	
	/**
	 * The Fungi Cataclysm consists of fungus spores appearing across the planet and slowly growing and consuming the world
	 */
	public Fungi(Console console, EntityManager entityManager, EntityFinder entityFinder, PlaneManager planeManager,
			GamemodeManager gamemodeManager, AssetManager assetManager, Cataclysm cataclysm, TagHandler tagHandler, String[] arguments) {
		super(console, entityManager, entityFinder, planeManager, gamemodeManager, assetManager, cataclysm, tagHandler, arguments);
		
		introStartTime = 60;
		endStartTime = 600;
		
		if (!gamemode.has("spores")) gamemode.put("spores", 0);
	}

	public static String getName() {
		return "fungi";
	}
	
	public void tick() {}
	
	public void secondTick() {

		String state = gamemode.getString("cataclysmState");
		int spawnedSpores = gamemode.getInt("spores");
		int tileCount = planeManager.getPlane(1).getTilesCount();
		
		if (state.equals("intro") && spawnedSpores < tileCount / tilesPerIntroSpore ) {
			boolean result = spawnNewSpore();
			
			if (result) gamemode.put("spores", ++spawnedSpores);
		}
		
		checkStateSwitch();
		if (!state.equals(gamemode.get("cataclysmState"))) System.out.println("[Fungi]"+gamemode.get("cataclysmState"));
	}
	
	public String getWinner() {
		String state = gamemode.getString("cataclysmState");
		
		if (state.equals("end") || true ) {
			int fungusCount = tagHandler.getTagAmount("fungus");
			int tileCount = planeManager.getTileCount(1);
			
			double currentRatio = (double) fungusCount/tileCount;
			System.out.println("[Fungi] fungi percentage: ");
			System.out.printf("%.10f%n",currentRatio);
			
			if (currentRatio >= FUNGUS_WIN_PERCENTAGE) return getName();
			else if (currentRatio <= FUNGUS_LOOSE_PERCENTAGE) return "players";
		}
		
		return null;
	}
	
	
	private boolean spawnNewSpore() {
		Tile tile = Random.findRandomTile(planeManager.getPlane(1), Fungus.allowedTiles);
		Vector vector = CoordinateConverter.toVector(tile.getCoordinate());
		// checking if theres no structures or entities there
		for (Entity entity : entityFinder.getAllEntitiesInRadius(vector, 1, 1)) {
			if (!entity.getIsItem()) return false;
		}
		
		tile = Fungus.convertTile(tile);
		planeManager.updateTile(tile, 1);;
		
		Entity newSpore = assetManager.getEntity("fungus spore", 1);
		newSpore = Random.randomizeDirection(newSpore);
		newSpore = newSpore.toBuilder()
				.setPosition(vector)
				.putInventorySlots("parentSpore", newSpore.getId())
				.build();
		
		System.out.println("[Fungi] "+CoordinateConverter.convert(vector));
		
		entityManager.updateEntity(newSpore);
		return true;
	}
}
