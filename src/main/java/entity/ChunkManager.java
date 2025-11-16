package entity;

import java.util.HashMap;
import java.util.Map;

import protonova.protobuf.ChunkProto.Chunk;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;

public class ChunkManager {

	private HashMap<Integer, HashMap<Coordinate, Chunk>> chunks;
	private HashMap<Integer, Entity> entities;
	private final int CHUNK_SIZE = 10;
	
	public ChunkManager(HashMap<Integer, Entity> allEntities) {
		entities = allEntities;
		chunks = new HashMap<Integer, HashMap<Coordinate, Chunk>>();
	}

	private Coordinate getChunkCoordinate(Vector position) {
		Coordinate coordinate = Coordinate.newBuilder()
				.setX(Math.round(position.getX()/CHUNK_SIZE))
				.setY(Math.round(position.getY()/CHUNK_SIZE))
				.build();
		
		return coordinate;
	}
	
	private HashMap<Coordinate, Chunk> getPlaneChunks(Entity entity) {
		
		if (!chunks.containsKey(entity.getMap())) {
			chunks.put(entity.getMap(), new HashMap<Coordinate, Chunk>());
		}
		
		return chunks.get(entity.getMap());
	}
	
	private void addEntityToChunk(HashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, Entity entity) {
		if (!chunkMap.containsKey(coordinate)) {
			Chunk newChunk = Chunk.newBuilder()
					.setCoordinate(coordinate)
					.build();
			
			chunkMap.put(coordinate,newChunk);
		}
		
		Chunk selectedChunk = chunkMap.get(coordinate);
		selectedChunk = selectedChunk.toBuilder()
			.addEntities(entity)
			.build();	
		
		chunkMap.put(coordinate, selectedChunk);
	}
	
	public void addEntity(Entity entity) {
		addEntityToChunk(getPlaneChunks(entity),getChunkCoordinate(entity.getPosition()),entity);
	}
	
	private void removeEntityFromChunk(HashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, Entity entity) {
		if (chunkMap.containsKey(coordinate)) {
			Chunk selectedChunk = chunkMap.get(coordinate);
			
			for (int i=0;i<selectedChunk.getEntitiesCount();i++) {
				if (entity.equals(selectedChunk.getEntities(i))) {
					selectedChunk = selectedChunk.toBuilder()
							.removeEntities(i)
							.build();
					chunkMap.put(coordinate, selectedChunk);
					break;
				}
			}
		}
		else {
			System.out.println("Tried to remove entity from chunk that dosent exist");
		}
	}
	
	private void groupEntity(Entity entity) {
		HashMap<Coordinate, Chunk> chunkMap = getPlaneChunks(entity);
		
		Coordinate coordinate = getChunkCoordinate(entity.getPosition());
		
		addEntityToChunk(chunkMap,coordinate,entity);
	}
	
	public void groupAllEntites() {
		chunks = new HashMap<Integer, HashMap<Coordinate, Chunk>>();
		
		for (Map.Entry<Integer, Entity> entry : entities.entrySet()) {
			Entity entity = entry.getValue();
			groupEntity(entity);
			
        }
	}
	
	public void updateEntityPosition(Entity entity, Vector newPosition) {
		removeEntityFromChunk(getPlaneChunks(entity), getChunkCoordinate(entity.getPosition()), entity);
		
		entity = entity.toBuilder()
				.setPosition(newPosition)
				.build();
		entities.put(entity.getId(), entity);
		
		addEntityToChunk(getPlaneChunks(entity), getChunkCoordinate(entity.getPosition()), entity);
	}
	
	public HashMap<Integer, HashMap<Coordinate, Chunk>> getChunks() {
		return chunks;
	}
}
