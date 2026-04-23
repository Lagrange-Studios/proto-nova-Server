package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import protonova.protobuf.ChunkProto.Chunk;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;
import util.VectorMath;

public class EntityFinder {
	private HashMap<Integer, Entity> entities;
	private ChunkManager chunkManager;
	
	public EntityFinder(HashMap<Integer, Entity> allEntities, ChunkManager chunkManager) {
		entities = allEntities;
		this.chunkManager = chunkManager;
	}
	
	public ArrayList<Entity> getAllEntitiesInRadius(Vector start, int mapId, double radius) {
		
		HashMap<Coordinate,Chunk> chunkMap = chunkManager.getPlaneChunks(mapId);
		Coordinate chunkCoordinate = CoordinateConverter.toChunkCoordinates(start);
		
		int radiusInChunks = (int) (Math.round(radius/CoordinateConverter.CHUNK_SIZE) + 1);
		
		ArrayList<Entity> foundEntities = new ArrayList<Entity>();
		double radiusSquared = radius * radius; // Avoid sqrt calculation in loop
		
		// Reuse coordinate builder to reduce allocations
		Coordinate.Builder coordBuilder = Coordinate.newBuilder();
		
		for (int x=-radiusInChunks;x<radiusInChunks;x++) {
			for (int y=-radiusInChunks;y<radiusInChunks;y++) {
				
				// Reuse builder instead of creating new Coordinate every iteration
				coordBuilder.setX(x + chunkCoordinate.getX());
				coordBuilder.setY(y + chunkCoordinate.getY());
				Coordinate coordinate = coordBuilder.build();
				
				if (chunkMap.containsKey(coordinate)) {
					List<Integer> chunkEntities = chunkMap.get(coordinate).getEntityIdsList();
					
					for (int i=0;i<chunkEntities.size();i++) {
						Entity selectedEntity = entities.get(chunkEntities.get(i));
						
						// Use squared distance to avoid expensive sqrt calculation
						double distanceSquared = VectorMath.distanceSquared(start, selectedEntity.getPosition());
						
						if (distanceSquared <= radiusSquared) {
							foundEntities.add(selectedEntity);
						}
					}
				}
			}
		}
		
		return foundEntities;
	}
	
	public ArrayList<Entity> getAllEntitiesInRadis(Entity startingEntity, double radius) {
		return getAllEntitiesInRadius(startingEntity.getPosition(), startingEntity.getMap(), radius);
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
