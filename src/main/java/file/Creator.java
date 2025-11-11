package file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.PlayerDataProto.PlayerData;

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
	
	public static PlayerData createNewPlayer(String username, int id) {
		PlayerData data = PlayerData.newBuilder()
				.setUsername(username)
				.setEntityId(id)
				.build();
		return data;
	}
}
