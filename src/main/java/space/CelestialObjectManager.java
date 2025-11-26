package space;

import java.util.HashMap;

import file.ServerLoader;
import main.Console;
import protonova.protobuf.CelestialObjectProto.CelestialObject;

public class CelestialObjectManager {

	private Console console;
	private HashMap<Integer,CelestialObject> celesitalObjects;
	
	public CelestialObjectManager(ServerLoader serverLoader, Console console) {
		this.console = console;
		celesitalObjects = serverLoader.loadCelestialObjects();
	}

}
