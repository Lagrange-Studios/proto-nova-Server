package file;

import java.util.HashMap;

import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlaneProto.Plane.Builder;
import protonova.protobuf.TileProto.Tile;
import util.CoordinateConverter;

public class PlaneGenerator {

	private static final int SizeX = 100;
	private static final int SizeY = 100;
	
	public static Plane generatePlane(HashMap<Integer, Plane> planes) {
		
		int planeId = 1;
		
		while (true) {
			if (planes.containsKey(planeId)) {
				planeId++;
			}
			else {
				break;
			}
		}
		
		Builder plane = Plane.newBuilder()
				.setId(planeId);
		
		for (int x=-SizeX/2;x<SizeX/2;x++) {
			for (int y=-SizeY/2;y<SizeY/2;y++) {
				
				Coordinate coordinate = Coordinate.newBuilder()
						.setX(x)
						.setY(y)
						.build();
				
				Tile tile = Tile.newBuilder()
						.setName("Grass")
						.setCoordinate(coordinate)
						.build();
				
				plane.putTiles(CoordinateConverter.convert(coordinate), tile);
			}
		}
		
		return plane.build();
	}
	
}
