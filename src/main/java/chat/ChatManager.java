package chat;

import java.util.ArrayList;

import protonova.protobuf.ChatProto.ChatMessage;;

public class ChatManager {
	private ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
	public ChatManager() {
		
	}
	
	public void proccessMessage(ChatMessage chatMessage) {
		if(!chatMessage.getServerProssesed()) {
			chatMessages.add(chatMessage);
		}
	}
	
	public ArrayList<ChatMessage> getChatTable() {
		return chatMessages;
	}
	
	public void resetChatTable() {
		chatMessages.clear();
	}
	
}
