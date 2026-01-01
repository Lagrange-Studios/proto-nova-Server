package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import protonova.protobuf.ChunkProto.Chunk;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;

public class ChunkManager {

	private HashMap<Integer, HashMap<Coordinate, Chunk>> chunks;
	private HashMap<Integer, Entity> entities;
	
	public ChunkManager(HashMap<Integer, Entity> allEntities) {
		entities = allEntities;
		chunks = new HashMap<Integer, HashMap<Coordinate, Chunk>>();
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
			.addEntityIds(entity.getId())
			.build();	
		
		chunkMap.put(coordinate, selectedChunk);
	}
	
	public void addEntity(Entity entity) {
		addEntityToChunk(getPlaneChunks(entity),CoordinateConverter.toChunkCoordinates(entity.getPosition()),entity);
	}
	
	private void removeEntityFromChunk(HashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, Entity entity) {
		if (chunkMap.containsKey(coordinate)) {
			Chunk selectedChunk = chunkMap.get(coordinate);
			
			for (int i=0;i<selectedChunk.getEntityIdsCount();i++) {
				if (entity.getId() == selectedChunk.getEntityIds(i)) {

					// This is the proper way to do it but its long and boring
					
					Chunk.Builder builder = selectedChunk.toBuilder();
					List<Integer> list = builder.getEntityIdsList();
					List<Integer> modifableList = new ArrayList<>(list);
					modifableList.remove(i);
					builder.clearEntityIds();
					builder.addAllEntityIds(modifableList);
					selectedChunk = builder.build();
					
					if (selectedChunk.getEntityIdsCount() == 0) {
						chunkMap.remove(coordinate);
					}
					else {
						chunkMap.put(coordinate, selectedChunk);
					}
							
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
		
		Coordinate coordinate = CoordinateConverter.toChunkCoordinates(entity.getPosition());
		
		addEntityToChunk(chunkMap,coordinate,entity);
	}
	
	public void groupAllEntites() {
		chunks = new HashMap<Integer, HashMap<Coordinate, Chunk>>();
		
		for (Map.Entry<Integer, Entity> entry : entities.entrySet()) {
			Entity entity = entry.getValue();
			groupEntity(entity);
			
        }
	}
	
	public void updateEntityChunck(Entity oldEntity, Entity newEntity) {
		removeEntityFromChunk(getPlaneChunks(oldEntity), CoordinateConverter.toChunkCoordinates(oldEntity.getPosition()), oldEntity);
		
		addEntityToChunk(getPlaneChunks(newEntity), CoordinateConverter.toChunkCoordinates(newEntity.getPosition()), newEntity);
	}
	
	public HashMap<Integer, HashMap<Coordinate, Chunk>> getChunks() {
		return chunks;
	}
	
	public HashMap<Coordinate, Chunk> getPlaneChunks(int mapId) {
		return chunks.get(mapId);
	}
}
