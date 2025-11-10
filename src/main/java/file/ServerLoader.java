package file;

import java.io.File;
import java.util.HashMap;

import protonova.protobuf.PlaneProto.Plane;

public class ServerLoader {

	public ServerLoader() {
		
	}
	
	public HashMap<Integer, Plane> loadWorld() {
		
		File worldRoot = new File("World");
		if (worldRoot.exists()) {
			
		}
		return null;
	}
}
