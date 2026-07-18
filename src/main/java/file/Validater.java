package file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.json.JSONObject;

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
			root.mkdir();
		}
		
		File planes = new File("worldRoot/planes");
		if (!planes.exists()) {
			planes.mkdir();
			
			shouldGenerate = true;
		}
		
		
		
		File playerData = new File("worldRoot/playerData");
		if (!playerData.exists()) {
			playerData.mkdir();
		}
		
		File entities = new File("worldRoot/entities.data");
		
		File celestialObjects = new File("worldRoot/celestialObjects");
		if (!celestialObjects.exists()) {
			celestialObjects.mkdir();
		}
		
		File gamemode = new File("worldRoot/gamemode.json");
		if (!gamemode.exists()) {
			JSONObject gamemodeJSON = new JSONObject();
			gamemodeJSON.put("name", "cataclysm");
			gamemodeJSON.put("time", 0);
			gamemodeJSON.put("gameOver", false);
			try {
				Files.write(Path.of(gamemode.getPath()), gamemodeJSON.toString().getBytes());
			} catch (IOException e) {
				console.print("ERROR: Could not create the default world data.");
			}
		}
		
		// if there's no planes then we should generate one
		if (!shouldGenerate) {
			shouldGenerate = celestialObjects.listFiles().length == 0;
		}
		
		return shouldGenerate;
	}
}
