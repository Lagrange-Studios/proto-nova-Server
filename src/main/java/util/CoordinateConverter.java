package util;

import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.VectorProto.Vector;

public class CoordinateConverter {

	public static final int CHUNK_SIZE = 10;
	
	public static String convert(Coordinate coordinate) {
		String x = String.valueOf(coordinate.getX());
		String y = String.valueOf(coordinate.getY());
		
		return x + "," + y;
	}
	
	public static Coordinate toChunkCoordinates(Vector vector) {
		Coordinate coordinate = Coordinate.newBuilder()
				.setX(Math.round(vector.getX()/CHUNK_SIZE))
				.setY(Math.round(vector.getY()/CHUNK_SIZE))
				.build();
		
		return coordinate;
	}
}
