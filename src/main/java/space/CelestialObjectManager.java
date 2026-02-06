package space;

import java.util.HashMap;

import file.ServerLoader;
import main.Console;
import main.Server;
import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.PlaneProto.Plane;

public class CelestialObjectManager {

	private Console console;
	private HashMap<Integer,CelestialObject> celesitalObjects;
	private HashMap<Integer,Integer> readBackMap; // the readback map is a quick way to find a celestialObject from a plane
	private Server server;
	
	public CelestialObjectManager(ServerLoader serverLoader, Console console, Server server) {
		this.console = console;
		this.server = server;
		celesitalObjects = serverLoader.loadCelestialObjects();
		readBackMap = new HashMap<Integer,Integer>();
		
		for (CelestialObject object : celesitalObjects.values()) {
			updateReadBack(object,object.getSurfacePlaneId());
		}
	}
	
	public HashMap<Integer,CelestialObject> getCelestialObjects() {
		return celesitalObjects;
	}
	
	public CelestialObject getCelestialObject(int id) {
		return celesitalObjects.get(id);
	}

	public void updateCelestialObject(int id, CelestialObject celestialObject) {
		updateReadBack(id,celestialObject.getSurfacePlaneId());
		celesitalObjects.put(id, celestialObject);
	}
	
	public void updateCelestialObject(CelestialObject celestialObject) {
		updateCelestialObject(celestialObject.getId(),celestialObject);
	}
	
	public void tickCelestialObjects() {
		for (CelestialObject object : celesitalObjects.values()) {
			double currentRotation = object.getCurrentRotation();
			//console.print("Current Rotation: "+currentRotation);
			
			// add time
			currentRotation += (1/(object.getRotationPeroidMinutes()*60))/server.TPS;
			
			// loop if we complete a rotation
			currentRotation = currentRotation>1?currentRotation-1:currentRotation;
			
			// update value
			object = object.toBuilder()
					.setCurrentRotation(currentRotation)
					.build();
			
			updateCelestialObject(object);
		}
	}
	
	public CelestialObject getCelestialObjectFromPlane(Plane plane) {
		return getCelestialObjectFromPlane(plane.getId());
	}
	
	public CelestialObject getCelestialObjectFromPlane(int id) {
		return celesitalObjects.get(readBackMap.get(id));
	}
	
	private void updateReadBack(int celestialId, int planeId) {
		if (planeId != 0) {
			if (readBackMap.containsKey(planeId)) readBackMap.remove(planeId);
			
			readBackMap.put(planeId,celestialId);
		}
	}
	
	private void updateReadBack(CelestialObject celestialObject, int planeId) {
		updateReadBack(celestialObject.getId(),planeId);
	}
}
