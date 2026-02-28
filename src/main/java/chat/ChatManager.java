package chat;

import java.util.ArrayList;
import java.util.HashMap;

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
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;
	private Server server;
	
	public ChatManager(ServerLoader serverLoader,Console console, Server server) {
		this.serverLoader = serverLoader;
		this.server = server;
		chatQueue = new ArrayList<ChatMessage>();
		chats = new ArrayList<ChatMessage>();
		this.console = console;
	}
	
	public void addChatToQueue(ChatMessage message) {
		chatQueue.add(message);
	}
	
	public void processChatMessagesToSend() {
		removeAllChatsFromChuncks();
		for (ChatMessage message : chatQueue) {
			makeNewChat(message);
		}
		chatQueue.clear();
		
	}
	
	private ChatMessage makeNewChat(ChatMessage message) {
		
		chats.add(message);		
		
		if (chunkManager != null) {
			chunkManager.addChatMessage(message);
		}
		else {
			console.print("WARNING: created Chat without adding it to the chunk manager");
		}
		
		return message;
		
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
