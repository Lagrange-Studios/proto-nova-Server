package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import protonova.protobuf.ChunkProto.Chunk;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;
import util.VectorMath;
import diagnostics.ResourceDiagnostics;

public class EntityFinder {
	private HashMap<Integer, Entity> entities;
	private ChunkManager chunkManager;
	private final int CHUNKS_PER_THREAD = 10;
	
	public EntityFinder(HashMap<Integer, Entity> allEntities, ChunkManager chunkManager) {
		entities = allEntities;
		this.chunkManager = chunkManager;
	}

	public ArrayList<Entity> getAllEntitiesInRadis(Entity startingEntity, double radius) {
		return getAllEntitiesInRadius(startingEntity.getPosition(), startingEntity.getMap(), radius);
	}
	
	public ArrayList<Entity> getAllEntitiesInRadius(Vector start, int mapId, double radius) {
		
		HashMap<Coordinate,Chunk> chunkMap = chunkManager.getPlaneChunks(mapId);
		Coordinate chunkCoordinate = CoordinateConverter.toChunkCoordinates(start);
		
		int radiusInChunks = (int) (Math.round(radius/CoordinateConverter.CHUNK_SIZE) + 1);
		
		double radiusSquared = radius * radius; // Avoid sqrt calculation in loop
		
		// split each chunk search into threads for speed
		ExecutorService executor = Executors.newSingleThreadExecutor(
				ResourceDiagnostics.threadFactory("Entity-Finder"));
		ArrayList<Chunk> chunkList = new ArrayList<Chunk>();
		ArrayList<Future<ArrayList<Entity>>> futureLists = new ArrayList<>();
		
		// Reuse coordinate builder to reduce allocations
		Coordinate.Builder coordBuilder = Coordinate.newBuilder();
		
		// find all chunks to search
		for (int x=-radiusInChunks;x<radiusInChunks;x++) {
			for (int y=-radiusInChunks;y<radiusInChunks;y++) {
				
				// Reuse builder instead of creating new Coordinate every iteration
				coordBuilder.setX(x + chunkCoordinate.getX());
				coordBuilder.setY(y + chunkCoordinate.getY());
				Coordinate coordinate = coordBuilder.build();
				
				if (chunkMap.containsKey(coordinate))
					chunkList.add(chunkMap.get(coordinate));
			}
		}
		
		// search through chunks
		for (int i=0;i<chunkList.size();i+=CHUNKS_PER_THREAD) {
			Future<ArrayList<Entity>> future = executor.submit(findEntitiesInChunk(chunkList,i,Math.min(i+CHUNKS_PER_THREAD, chunkList.size()-1),
					start,radiusSquared));
			
			futureLists.add(future);
		}
		
		// collect results
		ArrayList<Entity> foundEntities = new ArrayList<Entity>();
		for (Future<ArrayList<Entity>> future : futureLists) {
			try {
				foundEntities.addAll(future.get());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		return foundEntities;
	}
	
	private Callable<ArrayList<Entity>> findEntitiesInChunk(ArrayList<Chunk> list,int indexStart, int indexEnd, Vector start, double radiusSquared  ) {
		// split each group chunk search into threads for speed
		Callable<ArrayList<Entity>> thread = () -> {
			ArrayList<Entity> chunkEntitiesFound = new ArrayList<>();
			
			for (int i=indexStart;i<indexEnd;i++) {
				Chunk chunk = list.get(i);
				
				for (int id : chunk.getEntityIdsList()) {
					Entity selectedEntity = entities.get(id);
					
					// Use squared distance to avoid expensive sqrt calculation
					double distanceSquared = VectorMath.distanceSquared(start, selectedEntity.getPosition());
					
					if (distanceSquared <= radiusSquared) {
						chunkEntitiesFound.add(selectedEntity);
					}
				}
			}
			
			return chunkEntitiesFound;
		};
		
		return thread;
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
