package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;

public class Player {
	private Socket socket;
	private String username;
	
	private DataInputStream input;
	private DataOutputStream output;
	private boolean connected = false;
	
	public Player(Socket socket) {
		this.socket = socket;
		
		try {
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			connected = true;
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
}
