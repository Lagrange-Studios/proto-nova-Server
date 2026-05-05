package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import jdk.dynalink.StandardOperation;
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
    			.setX(1)
    			.setY(1)
    			.build();
    	
    	Vector oneHalfTwo = Vector.newBuilder()
    			.setX(1.5f)
    			.setY(2)
    			.build();
   
    	Vector pointFive = Vector.newBuilder()
    			.setX(.5f)
    			.setY(.5f)
    			.build();
    	Vector pointSevenFive = Vector.newBuilder()
    			.setX(.75f)
    			.setY(.75f)
    			.build();
    	
    	Entity entity = Entity.newBuilder()
    			//.setDirection(Direction.Down)
    			.setName("berries")
    			.setPosition(zero)
    			.setSize(pointFive)
    			.setIsItem(true)
    			.setCanCollide(false)
    			.setAnchored(false)
    			.setCastShadow(false)
    			//.addTags("harvestable")
    			//.setSelectedSlot("berries")
    			.setStackable(true)
    			.setAmount(1)
    			.build();
    	
    	try {
			Files.write(Path.of("bin/"+entity.getName()+".data"), entity.toByteArray(), StandardOpenOption.CREATE_NEW);
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
