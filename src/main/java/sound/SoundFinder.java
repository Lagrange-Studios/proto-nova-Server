package sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import entity.ChunkManager;
import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.ChunkProto.Chunk;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;
import util.VectorMath;

public class SoundFinder {
	private HashMap<Integer, Entity> entities;
	private ArrayList<Audio> sounds;
	private ChunkManager chunkManager;
	
	public SoundFinder(HashMap<Integer, Entity> allEntities, ArrayList<Audio> allSounds, ChunkManager chunkManager) {
		sounds = allSounds;
		this.entities = allEntities;
		this.chunkManager = chunkManager;
	}
	
	public ArrayList<Audio> getAllSoundsInRadius(Vector start, int mapId, double radius) {
		
		HashMap<Coordinate,Chunk> chunkMap = chunkManager.getPlaneChunks(mapId);
		Coordinate chunkCoordinate = CoordinateConverter.toChunkCoordinates(start);
		
		int radiusInChunks = (int) (Math.round(radius/CoordinateConverter.CHUNK_SIZE) + 1);
		
		/*
		chunkCoordinate = chunkCoordinate.toBuilder()
				.setX(chunkCoordinate.getX() - radiusInChunks)
				.setY(chunkCoordinate.getY() - radiusInChunks)
				.build();*/
		
		ArrayList<Audio> foundSounds = new ArrayList<Audio>();
		
		for (int x=-radiusInChunks;x<radiusInChunks;x++) {
			for (int y=-radiusInChunks;y<radiusInChunks;y++) {
				
				Coordinate coordinate = Coordinate.newBuilder()
						.setX(x + chunkCoordinate.getX())
						.setY(y + chunkCoordinate.getY())
						.build();
				
				if (chunkMap.containsKey(coordinate)) {
					List<Audio> chunkSounds = chunkMap.get(coordinate).getSoundsList();
					
					for (int i=0;i<chunkSounds.size();i++) {
						Audio selectedSound = chunkSounds.get(i);
						Vector position;
						
						if (selectedSound.getPosition() == null) {
							position = entities.get(selectedSound.getEntityID()).getPosition();
						} else {
							position = selectedSound.getPosition();
						}
						
						
						
						double distance = VectorMath.distance(start, position);
						
						if (distance <= radius) {
							foundSounds.add(selectedSound);
						}
					}
				}
			}
		}
		
		return foundSounds;
	}
	
	public ArrayList<Audio> getAllSoundsInRadis(Entity startingEntity, double radius) {
		return getAllSoundsInRadius(startingEntity.getPosition(), startingEntity.getMap(), radius);
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
