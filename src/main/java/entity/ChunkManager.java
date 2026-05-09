package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import plane.PlaneManager;

import java.util.List;

import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.ChatProto.ChatMessage;
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
	
	public HashMap<Coordinate, Chunk> getPlaneChunks(int mapId) {
		
		if (!chunks.containsKey(mapId)) {
			chunks.put(mapId, new HashMap<Coordinate, Chunk>());
		}
		
		return chunks.get(mapId);
	}
	
	private HashMap<Coordinate, Chunk> getPlaneChunks(Entity entity) {
		
		return getPlaneChunks(entity.getMap());
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
	
	private void addSoundToChunk(HashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, Audio audio) {
		if (!chunkMap.containsKey(coordinate)) {
			Chunk newChunk = Chunk.newBuilder()
					.setCoordinate(coordinate)
					.build();
			
			chunkMap.put(coordinate,newChunk);
		}
		
		Chunk selectedChunk = chunkMap.get(coordinate);
		selectedChunk = selectedChunk.toBuilder()
			.addSounds(audio)
			.build();
		
		chunkMap.put(coordinate, selectedChunk);
	}
	
	private void addChatMessageToChunk(HashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, ChatMessage message) {
		if (!chunkMap.containsKey(coordinate)) {
			Chunk newChunk = Chunk.newBuilder()
					.setCoordinate(coordinate)
					.build();
			
			chunkMap.put(coordinate,newChunk);
		}
		
		Chunk selectedChunk = chunkMap.get(coordinate);
		selectedChunk = selectedChunk.toBuilder()
			.addChats(message)
			.build();
		
		chunkMap.put(coordinate, selectedChunk);
	}
	
	public void addEntity(Entity entity) {
		addEntityToChunk(getPlaneChunks(entity),CoordinateConverter.toChunkCoordinates(entity.getPosition()),entity);
	}
	
	public void addSound(Audio audio) {
		Vector position;
		
		if (audio.getPosition() == null) {
			position = entities.get(audio.getEntityID()).getPosition();
		} else {
			position = audio.getPosition();
		}
		
		addSoundToChunk(getPlaneChunks(audio.getMap()),CoordinateConverter.toChunkCoordinates(position),audio);
	}
	
	public void addChatMessage(ChatMessage message) {
		Vector position;
		
		if (message.getPosition() == null) {
			position = entities.get(message.getEntityID()).getPosition();
		} else {
			position = message.getPosition();
		}
		
		addChatMessageToChunk(getPlaneChunks(message.getMap()),CoordinateConverter.toChunkCoordinates(position),message);
	}
	
	public synchronized void removeAllChatMessages() {
		for(Entry<Integer, HashMap<Coordinate, Chunk>> map : chunks.entrySet()) {
			for (Entry<Coordinate, Chunk> entry : map.getValue().entrySet()) {
				if (entry.getValue().getChatsCount() != 0) {
					entry.setValue(entry.getValue().toBuilder().clearChats().build());
				}
			}
		}
	}
	
	public synchronized void removeAllSounds() {
		for(Entry<Integer, HashMap<Coordinate, Chunk>> map : chunks.entrySet()) {
			for (Entry<Coordinate, Chunk> entry : map.getValue().entrySet()) {
				if (entry.getValue().getSoundsCount() != 0) {
					entry.setValue(entry.getValue().toBuilder().clearSounds().build());
				}
			}
		}
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
		// Silent fail - chunk doesn't exist (expected in some edge cases)
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
		removeEntityFromChunk(oldEntity);
		
		addEntityToChunk(getPlaneChunks(newEntity), CoordinateConverter.toChunkCoordinates(newEntity.getPosition()), newEntity);
	}
	
	public HashMap<Integer, HashMap<Coordinate, Chunk>> getChunks() {
		return chunks;
	}
	
	public void removeEntityFromChunk(Entity entity) {
		removeEntityFromChunk(getPlaneChunks(entity), CoordinateConverter.toChunkCoordinates(entity.getPosition()), entity);
	}
}
