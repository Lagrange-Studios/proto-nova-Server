package space;

import file.ServerLoader;
import java.util.HashMap;
import main.Console;
import main.Server;
import protonova.protobuf.CelestialObjectProto.CelestialObject;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;

public class CelestialObjectManager {

  private Console console;
  private HashMap<Integer, CelestialObject> celesitalObjects;
  private HashMap<Integer, Integer>
      readBackMap; // the readback map is a quick way to find a celestialObject from a plane
  private Server server;

  public CelestialObjectManager(ServerLoader serverLoader, Console console, Server server) {
    this.console = console;
    this.server = server;
    celesitalObjects = serverLoader.loadCelestialObjects();
    readBackMap = new HashMap<Integer, Integer>();

    for (CelestialObject object : celesitalObjects.values()) {
      updateReadBack(object);
    }
  }

  public HashMap<Integer, CelestialObject> getCelestialObjects() {
    return celesitalObjects;
  }

  public CelestialObject getCelestialObject(int id) {
    return celesitalObjects.get(id);
  }

  public void updateCelestialObject(int id, CelestialObject celestialObject) {
    updateReadBack(celestialObject);
    celesitalObjects.put(id, celestialObject);
  }

  public void updateCelestialObject(CelestialObject celestialObject) {
    updateCelestialObject(celestialObject.getId(), celestialObject);
  }

  public void tickCelestialObjects() {
    for (CelestialObject object : celesitalObjects.values()) {
      double currentRotation = object.getCurrentRotation();
      // console.print("Current Rotation: "+currentRotation);

      // add time
      currentRotation += (1 / (object.getRotationPeroidMinutes() * 60)) / server.TPS;

      // loop if we complete a rotation
      currentRotation = currentRotation > 1 ? currentRotation - 1 : currentRotation;

      // update value
      object = object.toBuilder().setCurrentRotation(currentRotation).build();

      updateCelestialObject(object);
    }
  }

  public CelestialObject getCelestialObjectFromPlane(Plane plane) {
    return getCelestialObjectFromPlane(plane.getId());
  }

  public CelestialObject getCelestialObjectFromPlane(int id) {
    return celesitalObjects.get(readBackMap.get(id));
  }
  
  public CelestialObject getCelestialObjectFromEntity(Entity entity) {
	  return getCelestialObjectFromPlane(entity.getMap());
  }

  private void updateReadBack(CelestialObject celestialObject) {
	int surface = celestialObject.getSurfacePlaneId();
	int underground = celestialObject.getUndergroundPlaneId();
	
	int celestialId = celestialObject.getId();
	  
    if (surface != 0) readBackMap.put(surface, celestialId);
    if (underground != 0) readBackMap.put(underground, celestialId);
  }
}
