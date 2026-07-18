package chat;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.protobuf.UInt64Value;

import entity.ChunkManager;
import file.ServerLoader;
import main.Console;
import main.Server;
import protonova.protobuf.AudioProto.Audio;
import protonova.protobuf.ChatProto.ChatMessage;
import protonova.protobuf.EntityProto.Entity;
import socket.Player;;

public class ChatManager {
	
	private ArrayList<ChatMessage> chatQueue;
	private ArrayList<ChatMessage> chats;
	private HashMap<Long, Long> chatCreationTime;
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;
	private Server server;
	private long chatID = 0;
	private final long CHAT_LIFETIME_MS = 100000; // 100 seconds in milliseconds
	private ArrayList<Player> playerList;
	
	public ChatManager(ServerLoader serverLoader,Console console, Server server, ArrayList<Player> playerList) {
		this.serverLoader = serverLoader;
		this.server = server;
		this.playerList = playerList;
		chatQueue = new ArrayList<ChatMessage>();
		chats = new ArrayList<ChatMessage>();
		chatCreationTime = new HashMap<>();
		this.console = console;
	}
	
	public void addChatToQueue(ChatMessage message) {
		message = message.toBuilder().setChatID(chatID).build();
		chatID++;
		chatQueue.add(message);
	}
	
	public void processChatMessagesToSend() {
		removeAllChatsFromChuncks();
		for (ChatMessage message : chatQueue) {
			makeNewChat(message);
		}
		chatQueue.clear();
		
		// Clean up old chat messages to prevent memory leak
		cleanupOldChats();
	}
	
	public void messagePlayer(String name, String message) {
		for (Player player : playerList) {
			if (player.getUsername().equals(name)) {
				player.messageList.add(message);
				console.print("Sent message to: "+name);
				return;
			}
		}
		
		console.print("Could'nt find player: "+name);
	}
	
	public void messageAllPlayers(String message) {
		for (Player player : playerList) {
			player.messageList.add(message);
		}
		
		console.print("Messaged all players: "+message);
	}
	
	private ChatMessage makeNewChat(ChatMessage message) {
		
		chats.add(message);
		chatCreationTime.put(message.getChatID(), System.currentTimeMillis());
		
		if (chunkManager != null) {
			chunkManager.addChatMessage(message);
		}
		else {
			console.print("WARNING: created Chat without adding it to the chunk manager");
		}
		
		return message;
		
	}
	
	// Removes chat messages older than CHAT_LIFETIME_MS
	private void cleanupOldChats() {
		long currentTime = System.currentTimeMillis();
		ArrayList<Long> keysToRemove = new ArrayList<>();
		
		for (Long chatID : chatCreationTime.keySet()) {
			if (currentTime - chatCreationTime.get(chatID) > CHAT_LIFETIME_MS) {
				keysToRemove.add(chatID);
			}
		}
		
		for (Long chatID : keysToRemove) {
			chatCreationTime.remove(chatID);
			// Remove from chats list
			chats.removeIf(msg -> msg.getChatID() == chatID);
		}
	}
	
	public void removeAllChatsFromChuncks() {
		chunkManager.removeAllChatMessages();
	}
	
	public ArrayList<ChatMessage> getAllChats() {
		return chats;
	}
	
	public void setChunkManager(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
	}
}
