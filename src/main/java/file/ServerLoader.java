package file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import main.Console;
import protonova.protobuf.PlaneProto;
import protonova.protobuf.PlaneProto.Plane;

public class ServerLoader {

	private Console console;
	private Validater validater;
	
	public ServerLoader(Console console) {
		this.console = console;
		validater = new Validater(console);
	}
	
	private Plane loadPlane(File directory) {
		
		File plane = new File(directory.getPath() + "/plane.data");
		
		try {
			byte[] array = Files.readAllBytes(Paths.get(plane.getPath()));
			
			Plane planeObject = PlaneProto.Plane.parseFrom(array);
			
			return planeObject;
			
		} catch (IOException e) {
			e.printStackTrace();
			console.print("ERROR: " + plane.getPath() + " not found");
		}
		
		return null;
		
	}
	
	public HashMap<Integer, Plane> loadWorld() {
		
		validater.validateWorldFiles();
		
		HashMap<Integer, Plane> planes = new HashMap<Integer,Plane>();
		
		File[] planesDirectories = new File("worldRoot/planes").listFiles();
		
		for (int i=0;i<planesDirectories.length;i++) {
			Plane newPlane = loadPlane(planesDirectories[i]);
			planes.put(newPlane.getId(), newPlane);
		}
		
		return planes;
	}
}
