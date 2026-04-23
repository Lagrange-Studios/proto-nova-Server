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
import protonova.protobuf.EntityProto.Entity;;

public class ChatManager {
	
	private ArrayList<ChatMessage> chatQueue;
	private ArrayList<ChatMessage> chats;
	private HashMap<Long, Long> chatCreationTime; // chatID -> creation time in ticks
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;
	private Server server;
	private long chatID = 0;
	private final int CHAT_LIFETIME_TICKS = 6000; // ~100 seconds at 60 TPS
	
	public ChatManager(ServerLoader serverLoader,Console console, Server server) {
		this.serverLoader = serverLoader;
		this.server = server;
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
	
	private ChatMessage makeNewChat(ChatMessage message) {
		
		chats.add(message);
		chatCreationTime.put(message.getChatID(), server.globalTicks);
		
		if (chunkManager != null) {
			chunkManager.addChatMessage(message);
		}
		else {
			console.print("WARNING: created Chat without adding it to the chunk manager");
		}
		
		return message;
		
	}
	
	/**
	 * Removes chat messages older than CHAT_LIFETIME_TICKS
	 */
	private void cleanupOldChats() {
		long currentTick = server.globalTicks;
		ArrayList<Long> keysToRemove = new ArrayList<>();
		
		for (Long chatID : chatCreationTime.keySet()) {
			if (currentTick - chatCreationTime.get(chatID) > CHAT_LIFETIME_TICKS) {
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
