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
	
	private ArrayList<ChatMessage> chats;
	private ServerLoader serverLoader;
	private ChunkManager chunkManager;
	private Console console;
	private Server server;
	
	public ChatManager(ServerLoader serverLoader,Console console, Server server) {
		this.serverLoader = serverLoader;
		this.server = server;
		chats = new ArrayList<ChatMessage>();
		this.console = console;
	}
	
	public ChatMessage makeNewChat(ChatMessage message) {
		
		chats.add(message);		
		
		if (chunkManager != null) {
			chunkManager.addChatMessage(message);
		}
		else {
			console.print("WARNING: created Chat without adding it to the chunk manager");
		}
		
		return message;
		
	}
	
	public ArrayList<ChatMessage> getAllChats() {
		return chats;
	}
}
