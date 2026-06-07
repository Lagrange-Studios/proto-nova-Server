package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import jdk.dynalink.StandardOperation;
import protonova.protobuf.DamageProto.Damage;
import protonova.protobuf.DamageProto.DamageMultiplier;
import protonova.protobuf.DamageProto.HitDamage;
import protonova.protobuf.EntityProto.Direction;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;

public class AssetMaker {
    public static void main(String[] args) {
    	DamageMultiplier damageMult = DamageMultiplier.newBuilder()
			    .setBrute(1)
			    .setAsphyxiation(1)
			    .setBurn(1)
			    .setToxin(1)
			    .setGenetic(1)
			    .setStructural(1)
			    .setBleeding(1)
			    .build();
    	HitDamage hitDamageStruc = HitDamage.newBuilder()
			    .setBruteDamage(0)
			    .setAsphyxiationDamage(0)
			    .setBurnDamage(0)
			    .setToxinDamage(0)
			    .setGeneticDamage(0)
			    .setStructuralDamage(10)
			    .setBleedingPerTick(0)
			    .build();
    	Damage damage = Damage.newBuilder()
				.setBruteDamage(0)
				.setAsphyxiationDamage(0)
				.setBurnDamage(0)
				.setToxinDamage(0)
				.setGeneticDamage(0)
				.setStructuralDamage(0)
				.setBleedingPerTick(0)
				.setDamageMultiplier(damageMult)
				.build();
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
    			.setDirection(Direction.Down)
    			.setName("fungus spore")
    			.setPosition(zero)
    			.setSize(one)
    			//.setIsItem(true)
    			.setCanCollide(true)
    			.setAnchored(true)
    			.setDamage(damage)
    			.setHitDamage(hitDamageStruc)
    			//.setCastShadow(false)
    			.addTags("fungus")
    			//.addTags("plant")
    			//.setSelectedSlot("berries")
    			//.setStackable(false)
    			//.setAmount(1)
    			//.setHexColor("FF0000")
    			//.setLightRange(5)
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
