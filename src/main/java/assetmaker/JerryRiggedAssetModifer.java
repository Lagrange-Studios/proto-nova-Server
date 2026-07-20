package assetmaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import protonova.protobuf.EntityProto.Entity;

public class JerryRiggedAssetModifer {

	public static void main(String[] args) {
		Path path = Path.of("assets/entities/fungus monster.data");
		
		try {
			Entity entity = Entity.parseFrom(Files.readAllBytes(path));
			entity = entity.toBuilder()
					.setHitDamage(entity.getHitDamage().toBuilder()
							.setCanAttack(true).build()).build();
			Files.write(path, entity.toByteArray());
			System.out.println("done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
