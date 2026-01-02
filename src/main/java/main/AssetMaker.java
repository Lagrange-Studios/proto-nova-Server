package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;

public class AssetMaker {
    public static void main(String[] args) {
    	Vector zero = Vector.newBuilder()
    			.setX(0)
    			.setY(0)
    			.build();
    	
    	Entity entity = Entity.newBuilder()
    			.setDirection(Direction.Down)
    			.setName("frog")
    			.setPosition(zero)
    			.setVelocity(zero)
    			.setSpeed(4)
    			.setCanCollide(false)
    			.build();
    	
    	try {
			Files.write(Path.of("assets/bin/"+entity.getName()+".data"), entity.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
