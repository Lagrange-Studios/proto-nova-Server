package entity;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

	private volatile ConcurrentHashMap<Integer, ConcurrentHashMap<Coordinate, Chunk>> chunks;
	private Map<Integer, Entity> entities;
	private PlaneManager planeManager;
	
	public ChunkManager(Map<Integer, Entity> allEntities, PlaneManager planeManager) {
		this.planeManager = planeManager;
		entities = allEntities;
		chunks = new ConcurrentHashMap<>();
	}

	public ConcurrentHashMap<Coordinate, Chunk> getPlaneChunks(int mapId) {
		return chunks.computeIfAbsent(mapId, selectedMapId -> {
			if (!planeManager.planeExists(selectedMapId) && selectedMapId != 0) {
				System.err.println("Warning: just tried to make chunks for plane id: " + selectedMapId + " which does not exist");
			}
			return new ConcurrentHashMap<>();
		});
	}

	private ConcurrentHashMap<Coordinate, Chunk> getPlaneChunks(Entity entity) {
		
		return getPlaneChunks(entity.getMap());
	}

	private void addEntityToChunk(ConcurrentHashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, Entity entity) {
		chunkMap.compute(coordinate, (selectedCoordinate, selectedChunk) -> {
			Chunk.Builder chunkBuilder = selectedChunk == null
					? Chunk.newBuilder().setCoordinate(selectedCoordinate)
					: selectedChunk.toBuilder();
			return chunkBuilder.addEntityIds(entity.getId()).build();
		});
	}

	private void addSoundToChunk(ConcurrentHashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, Audio audio) {
		chunkMap.compute(coordinate, (selectedCoordinate, selectedChunk) -> {
			Chunk.Builder chunkBuilder = selectedChunk == null
					? Chunk.newBuilder().setCoordinate(selectedCoordinate)
					: selectedChunk.toBuilder();
			return chunkBuilder.addSounds(audio).build();
		});
	}

	private void addChatMessageToChunk(ConcurrentHashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, ChatMessage message) {
		chunkMap.compute(coordinate, (selectedCoordinate, selectedChunk) -> {
			Chunk.Builder chunkBuilder = selectedChunk == null
					? Chunk.newBuilder().setCoordinate(selectedCoordinate)
					: selectedChunk.toBuilder();
			return chunkBuilder.addChats(message).build();
		});
	}

	public void addEntity(Entity entity) {
		if (!canUseWorldChunks(entity)) {
			return;
		}
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

	public void removeAllChatMessages() {
		for (ConcurrentHashMap<Coordinate, Chunk> planeChunks : chunks.values()) {
			planeChunks.replaceAll((coordinate, chunk) -> chunk.getChatsCount() == 0
					? chunk
					: chunk.toBuilder().clearChats().build());
		}
	}

	public void removeAllSounds() {
		for (ConcurrentHashMap<Coordinate, Chunk> planeChunks : chunks.values()) {
			planeChunks.replaceAll((coordinate, chunk) -> chunk.getSoundsCount() == 0
					? chunk
					: chunk.toBuilder().clearSounds().build());
		}
	}

	private void removeEntityFromChunk(ConcurrentHashMap<Coordinate, Chunk> chunkMap, Coordinate coordinate, Entity entity) {
		chunkMap.computeIfPresent(coordinate, (selectedCoordinate, selectedChunk) -> {
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
					
					return selectedChunk.getEntityIdsCount() == 0 ? null : selectedChunk;
				}
			}
			return selectedChunk;
		});
	}
	
	private void groupEntity(Entity entity) {
		if (!canUseWorldChunks(entity)) {
			return;
		}
		ConcurrentHashMap<Coordinate, Chunk> chunkMap = getPlaneChunks(entity);
		
		Coordinate coordinate = CoordinateConverter.toChunkCoordinates(entity.getPosition());
		
		addEntityToChunk(chunkMap,coordinate,entity);
	}
	
	public void groupAllEntites() {
		chunks = new ConcurrentHashMap<>();
		
		for (Map.Entry<Integer, Entity> entry : entities.entrySet()) {
			Entity entity = entry.getValue();
			groupEntity(entity);
			
        }
	}
	
	public void updateEntityChunck(Entity oldEntity, Entity newEntity) {
		if (canUseWorldChunks(oldEntity)) {
			removeEntityFromChunk(oldEntity);
		}
		if (canUseWorldChunks(newEntity)) {
			addEntityToChunk(getPlaneChunks(newEntity), CoordinateConverter.toChunkCoordinates(newEntity.getPosition()), newEntity);
		}
	}
	
	public ConcurrentHashMap<Integer, ConcurrentHashMap<Coordinate, Chunk>> getChunks() {
		return chunks;
	}
	
	public void removeEntityFromChunk(Entity entity) {
		if (!canUseWorldChunks(entity)) {
			return;
		}
		removeEntityFromChunk(getPlaneChunks(entity), CoordinateConverter.toChunkCoordinates(entity.getPosition()), entity);
	}

	private boolean canUseWorldChunks(Entity entity) {
		return isWorldEntity(entity) && planeManager.planeExists(entity.getMap());
	}

	static boolean isWorldEntity(Entity entity) {
		if (entity.getMap() <= 0) {
			return false;
		}
		return !entity.hasOrganComponent()
				|| !entity.getOrganComponent().hasInstalledInEntityId();
	}
}
