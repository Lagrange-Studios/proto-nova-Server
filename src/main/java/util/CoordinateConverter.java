package util;

import protonova.protobuf.CoordinateProto.Coordinate;

public class CoordinateConverter {

	public static String convert(Coordinate coordinate) {
		String x = String.valueOf(coordinate.getX());
		String y = String.valueOf(coordinate.getY());
		
		return x + "," + y;
	}
}
