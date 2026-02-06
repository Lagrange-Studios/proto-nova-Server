package file;

import java.io.File;
import java.util.HashMap;

import main.Console;
import protonova.protobuf.PlaneProto.Plane;

public class Validater {

	private Console console;
	
	public Validater(Console console) {
		this.console = console;
	}
	
	public boolean validateWorldFiles() {
		
		boolean shouldGenerate = false;
		
		File root = new File("worldRoot");
		if (!root.exists()) {
			console.print("Missing world root files");
			root.mkdir();
			console.print("Added world root file");
		}
		
		File planes = new File("worldRoot/planes");
		if (!planes.exists()) {
			planes.mkdir();
			
			shouldGenerate = true;
		}
		
		
		
		File playerData = new File("worldRoot/playerData");
		if (!playerData.exists()) {
			console.print("WARNING: no player data found");
			playerData.mkdir();
		}
		
		File entities = new File("worldRoot/entities");
		if (!entities.exists()) {
			console.print("WARNING: no entity data found");
			entities.mkdir();
		}
		
		File celestialObjects = new File("worldRoot/celestialObjects");
		if (!celestialObjects.exists()) {
			console.print("WARNING: no celestial objects data found");
			celestialObjects.mkdir();
		}
		
		// if there's no planes then we should generate one
		if (!shouldGenerate) {
			shouldGenerate = celestialObjects.listFiles().length == 0;
		}
		
		console.print("Validated world files");
		return shouldGenerate;
	}
}
