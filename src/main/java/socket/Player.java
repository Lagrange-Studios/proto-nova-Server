package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import main.Console;
import protonova.protobuf.UserDataProto;
import enums.Player.State;

public class Player {
	private Socket socket;
	private String username;
	
	private DataInputStream input;
	private DataOutputStream output;
	private State state = State.DISCONNECTED;
	private Console console;
	
	public Player(Socket socket, Console console) {
		this.socket = socket;
		this.console = console;
		
		try {
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			state = State.AWAITING_CLIENT_PACKET;
			listen();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		try {
			input.close();
			output.close();
			socket.close();
			state = State.DISCONNECTED;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public State getState() {
		return state;
	}
	
	public void listen() {
	    Thread thread = new Thread(() -> {
	        try {
	            while (state != State.DISCONNECTED) {
	            	
	                int length = input.readInt(); // length of incoming message
	                
	                byte[] data = new byte[length];
	                input.readFully(data); // read exactly 'length' bytes

	                // Deserialize
	                if (username == null ) {
		                UserDataProto.UserData user = UserDataProto.UserData.parseFrom(data);
		                username = user.getUsername();
		                console.print("Received user: " + user.getUsername());
	                }
	                else {
	                	
	                }
	            }
	        } catch (IOException e) {
	            console.print("Connection closed or error: " + e.getMessage());
	            disconnect();
	        }
	    });
	    thread.start();
	}

	public String getUsername() {
		return username;
	}
}
