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

public class Player {
	private Socket socket;
	private String username;
	
	private DataInputStream input;
	private DataOutputStream output;
	private boolean connected = false;
	private Console console;
	
	public Player(Socket socket, Console console) {
		this.socket = socket;
		this.console = console;
		
		try {
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			connected = true;
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
			connected = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public void listen() {
	    Thread thread = new Thread(() -> {
	        try {
	            while (connected) {
	                int length = input.readInt(); // length of incoming message
	                byte[] data = new byte[length];
	                input.readFully(data); // read exactly 'length' bytes

	                // Deserialize
	                UserDataProto.UserData user = UserDataProto.UserData.parseFrom(data);
	                console.print("Received user: " + user.getUsername());
	            }
	        } catch (IOException e) {
	            console.print("Connection closed or error: " + e.getMessage());
	            disconnect();
	        }
	    });
	    thread.start();
	}

}
