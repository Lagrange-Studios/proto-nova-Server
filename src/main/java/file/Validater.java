package file;

import java.io.File;

import main.Console;

public class Validater {

	private Console console;
	
	public Validater(Console console) {
		this.console = console;
	}
	
	public void validateWorldFiles() {
		
		File root = new File("worldRoot");
		if (!root.exists()) {
			console.print("Missing world root files");
			root.mkdir();
			console.print("Added world root file");
		}
		
		File planes = new File("worldRoot/planes");
		if (!planes.exists()) {
			planes.mkdir();
			
			File map1 = new File("worldRoot/planes/map1");
			map1.mkdir();
			
			File entities = new File("worldRoot/planes/map1/entities");
			entities.mkdir();
		}
		
		File playerData = new File("worldRoot/playerData");
		if (!playerData.exists()) {
			console.print("WARNING: no player data found");
			playerData.mkdir();
		}
		
		console.print("Validated world files");
	}
}
