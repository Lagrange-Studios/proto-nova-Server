package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;

public class AssetMaker {
    public static void main(String[] args) {
    	Vector zero = Vector.newBuilder()
    			.setX(0)
    			.setY(0)
    			.build();
    	
    	Vector one = Vector.newBuilder()
    			.setX(.5f)
    			.setY(.5f)
    			.build();
    	
    	Entity entity = Entity.newBuilder()
    			.setDirection(Direction.Down)
    			.setName("stick")
    			.setPosition(zero)
    			.setSize(one)
    			.setIsItem(true)
    			.setStackable(true)
    			.setAmount(1)
    			.build();
    	
    	try {
			Files.write(Path.of("assets/bin/"+entity.getName()+".data"), entity.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	/*for (File file : new File("assets/entities").listFiles()) {
    		try {
				Entity entity = Entity.parseFrom(Files.readAllBytes(Paths.get(file.getPath())));
				entity = entity.toBuilder()
						.setSize(Vector.newBuilder().setX(1).setY(1).build())
						.build();
				Files.write(Paths.get(file.getPath()), entity.toByteArray());
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}*/
    }
}
