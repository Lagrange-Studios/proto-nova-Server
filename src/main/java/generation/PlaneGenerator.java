package generation;

import java.util.HashMap;

import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlaneProto.Plane.Builder;
import protonova.protobuf.TileProto.Tile;
import util.CoordinateConverter;
import util.Id;

public class PlaneGenerator {

	private HashMap<Integer, Plane> planes;
	
	public PlaneGenerator(HashMap<Integer, Plane> planes) {
		this.planes = planes;
	}
	
	public Plane generatePlane(int sizeX, int sizeY, String tileName) {
		
		int planeId = Id.getNewId(planes.keySet());
		
		Coordinate size = Coordinate.newBuilder()
				.setX(sizeX)
				.setY(sizeY)
				.build();
		
		Builder plane = Plane.newBuilder()
				.setId(planeId)
				.setSize(size);
				
		
		if (tileName != null) {
			for (int x=-sizeX/2;x<=sizeX/2;x++) {
				for (int y=-sizeY/2;y<=sizeY/2;y++) {
					
					Coordinate coordinate = Coordinate.newBuilder()
							.setX(x)
							.setY(y)
							.build();
					
					Tile tile = Tile.newBuilder()
							.setName(tileName)
							.setCoordinate(coordinate)
							.build();
					
					plane.putTiles(CoordinateConverter.convert(coordinate), tile);
				}
			}
		}
		
		return plane.build();
	}
	
}
