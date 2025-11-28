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
		
		/*
		chunkCoordinate = chunkCoordinate.toBuilder()
				.setX(chunkCoordinate.getX() - radiusInChunks)
				.setY(chunkCoordinate.getY() - radiusInChunks)
				.build();*/
		
		ArrayList<Entity> foundEntities = new ArrayList<Entity>();
		
		for (int x=-radiusInChunks;x<radiusInChunks;x++) {
			for (int y=-radiusInChunks;y<radiusInChunks;y++) {
				
				Coordinate coordinate = Coordinate.newBuilder()
						.setX(x + chunkCoordinate.getX())
						.setY(y + chunkCoordinate.getY())
						.build();
				
				if (chunkMap.containsKey(coordinate)) {
					List<Integer> chunkEntities = chunkMap.get(coordinate).getEntityIdsList();
					
					for (int i=0;i<chunkEntities.size();i++) {
						Entity selectedEntity = entities.get(chunkEntities.get(i));
						
						double distance = VectorMath.distance(start, selectedEntity.getPosition());
						
						if (distance <= radius) {
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
