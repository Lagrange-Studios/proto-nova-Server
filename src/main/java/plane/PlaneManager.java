package plane;

import java.util.HashMap;

import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.TileProto.Tile;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;

public class PlaneManager {

	private HashMap<Integer, Plane> planes;
	
	public PlaneManager(HashMap<Integer, Plane> planes) {
		this.planes = planes;
	}
	
	public HashMap<Integer, Plane> getPlanes() {
		return planes;
	}
	
	public Plane getPlane(int id) {
		return planes.get(id);
	}
	
	public Plane getPlane(Entity entity) {
		return planes.get(entity.getMap());
	}
	
	public void updatePlane(int id, Plane plane) {
		planes.put(id, plane);
	}
	
	public void updatePlane(Plane plane) {
		planes.put(plane.getId(), plane);
	}
	
	public void updateTile(Tile tile, int id) {
		Plane newPlane = planes.get(id).toBuilder()
				.putTiles(CoordinateConverter.convert(tile.getCoordinate()), tile)
				.build();
		
		updatePlane(newPlane);
	}
	
	public Tile getTileAt(String coordinate, int id) {
		return planes.get(id).getTilesMap().get(coordinate);
	}
	
	public Tile getTileAt(Vector coordinate, int id) {
		return planes.get(id).getTilesMap().get(CoordinateConverter.convert(coordinate));
	}
	
	public Tile getTileAt(Coordinate coordinate, int id) {
		return planes.get(id).getTilesMap().get(CoordinateConverter.convert(coordinate));
	}
	
	public boolean planeExists(int id) {
		return planes.containsKey(id);
	}
	
	public int getTileCount(int id) {
		if (planes.containsKey(id)) return planes.get(id).getTilesCount();
		else return 0;
	}
}
