package chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import entity.ChunkManager;
import protonova.protobuf.ChatProto.ChatMessage;
import protonova.protobuf.ChunkProto.Chunk;
import protonova.protobuf.CoordinateProto.Coordinate;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import util.CoordinateConverter;
import util.VectorMath;

public class ChatFinder {
	private HashMap<Integer, Entity> entities;
	private ArrayList<ChatMessage> chats;
	private ChunkManager chunkManager;
	
	public ChatFinder(HashMap<Integer, Entity> allEntities, ArrayList<ChatMessage> allChats, ChunkManager chunkManager) {
		chats = allChats;
		this.entities = allEntities;
		this.chunkManager = chunkManager;
	}
	
	public ArrayList<ChatMessage> getAllChatsInRadius(Vector start, int mapId, double radius) {
		
		HashMap<Coordinate,Chunk> chunkMap = chunkManager.getPlaneChunks(mapId);
		Coordinate chunkCoordinate = CoordinateConverter.toChunkCoordinates(start);
		
		int radiusInChunks = (int) (Math.round(radius/CoordinateConverter.CHUNK_SIZE) + 1);
		
		/*
		chunkCoordinate = chunkCoordinate.toBuilder()
				.setX(chunkCoordinate.getX() - radiusInChunks)
				.setY(chunkCoordinate.getY() - radiusInChunks)
				.build();*/
		
		ArrayList<ChatMessage> foundChats = new ArrayList<ChatMessage>();
		
		for (int x=-radiusInChunks;x<radiusInChunks;x++) {
			for (int y=-radiusInChunks;y<radiusInChunks;y++) {
				
				Coordinate coordinate = Coordinate.newBuilder()
						.setX(x + chunkCoordinate.getX())
						.setY(y + chunkCoordinate.getY())
						.build();
				
				if (chunkMap.containsKey(coordinate)) {
					List<ChatMessage> chunkChats = chunkMap.get(coordinate).getChatsList();
					
					for (int i=0;i<chunkChats.size();i++) {
						ChatMessage selectedChat = chunkChats.get(i);
						Vector position;
						
						if (selectedChat.getPosition() == null) {
							position = entities.get(selectedChat.getEntityID()).getPosition();
						} else {
							position = selectedChat.getPosition();
						}
						
						
						
						double distance = VectorMath.distance(start, position);
						
						if (distance <= radius) {
							foundChats.add(selectedChat);
						}
					}
				}
			}
		}
		
		return foundChats;
	}
	
	public ArrayList<ChatMessage> getAllChatsInRadis(Entity startingEntity, double radius) {
		return getAllChatsInRadius(startingEntity.getPosition(), startingEntity.getMap(), radius);
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
