package file;

import java.io.File;

import protonova.protobuf.PlaneProto.Plane;

public class Creator {

	public static void createEmptyPlane(File mapDirectory, int id) {
		Plane plane = Plane.newBuilder()
				.setId(id)
				.build();
		
		
	}
}
