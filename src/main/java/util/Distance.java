package util;

import protonova.protobuf.VectorProto.Vector;

public class Distance {

	public static double getDistance(Vector position1, Vector position2) {
		
		return Math.sqrt(Math.pow(position1.getX()-position2.getX(), 2) + Math.pow(position1.getY()-position2.getY(), 2));
		
	}
}
