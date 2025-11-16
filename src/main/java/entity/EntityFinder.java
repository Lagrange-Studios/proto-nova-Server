package entity;

import java.util.HashMap;
import java.util.Set;

import protonova.protobuf.ChunkProto.Chunk;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;

public class EntityFinder {
	private HashMap<Integer, Entity> entities;
	private ChunkManager chunkManager;
	
	public EntityFinder(HashMap<Integer, Entity> allEntities, ChunkManager chunkManager) {
		entities = allEntities;
		this.chunkManager = chunkManager;
	}

	public void DEBUG_METHOD() {
		System.out.println("Start");
		HashMap<Integer, HashMap<Coordinate, Chunk>> chunks = chunkManager.getChunks();
		
		System.out.println("has: "+chunks.containsKey(1));
		if (chunks.containsKey(1)) {

			System.out.println("Size: "+String.valueOf(chunks.get(1).size()));
			
			if (chunks.get(1).size() > 0) {
				Set<Coordinate> keys = chunks.get(1).keySet();
				Coordinate[] chunkArray =  keys.toArray(new Coordinate[keys.size()]);
				Coordinate firstCord = chunkArray[0];
				
				System.out.println("Chunk: "+firstCord.getX()+","+firstCord.getY());
			}
		}
		
	}
}
