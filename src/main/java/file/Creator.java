package file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import protonova.protobuf.PlaneProto.Plane;

public class Creator {

	public static void createEmptyPlane(File mapDirectory, int id) {
		Plane plane = Plane.newBuilder()
				.setId(id)
				.build();
		
		File planeFile = new File(mapDirectory.getPath() + "/plane.data");
		
		try {
			Files.write(Paths.get(planeFile.getPath()), plane.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
