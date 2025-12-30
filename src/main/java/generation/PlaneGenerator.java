package generation;

import java.util.HashMap;

import perlinNoise.OpenSimplex2S;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlaneProto.Plane.Builder;
import protonova.protobuf.TileProto.Tile;
import util.CoordinateConverter;
import util.Id;

public class PlaneGenerator {

	// Perlin noise generator https://github.com/LefMarOli/PerlinNoiseJava.git
	
	private HashMap<Integer, Plane> planes;
	private static final double frequency = 1;//0.05;
	
	public PlaneGenerator(HashMap<Integer, Plane> planes) {
		this.planes = planes;
	}
	
	public Plane generatePlane(int sizeX, int sizeY) {
		
		int planeId = Id.getNewId(planes.keySet());
		
		Coordinate size = Coordinate.newBuilder()
				.setX(sizeX)
				.setY(sizeY)
				.build();
		
		Builder plane = Plane.newBuilder()
				.setId(planeId)
				.setSize(size);		
		
		for (int x=-sizeX/2;x<=sizeX/2;x++) {
			for (int y=-sizeY/2;y<=sizeY/2;y++) {
				
				double value = OpenSimplex2S.noise2(System.currentTimeMillis(),(x+sizeX)*frequency,(y+sizeY)*frequency);
				
				String tileName;
				
				if (value > 0.75) {
					tileName = "stone";
				}else if (value > .45) {
					tileName = "grass";
				}else if (value > .4) {
					tileName = "sand";
				}else {
					tileName = "water";
				}
				
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
		
		
		return plane.build();
	}
	
}
