package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import main.Console;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.PlayerDataProto.PlayerData;
import protonova.protobuf.UserDataProto;
import protonova.protobuf.UserDataProto.UserData;
import enums.Player.State;

public class Player {
	public Socket socket;
	private String username;
	
	private DataInputStream input;
	private DataOutputStream output;
	private volatile State state = State.DISCONNECTED;
	private Console console;
	public PlayerData data;
	private PacketReciver packetReciver;
	public boolean shouldReconcile = false;
	
	public Player(Socket socket, Console console, PacketReciver packetReciver) {
		this.socket = socket;
		this.console = console;
		this.packetReciver = packetReciver;
		
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
		if (state == State.DISCONNECTED) return;
		state = State.DISCONNECTED;

	    try {
	    	input.close();
			output.close();
			socket.close();
	    } catch (IOException ignored) {}
	}
	
	public State getState() {
		return state;
	}
	
	public void setState(State state) {
		this.state = state;
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
		                UserData user = UserData.parseFrom(data);
		                username = user.getUsername();
		                state = State.AWAITING_SERVER_PACKET;
		                console.print("Received user: " + user.getUsername());
	                }
	                else {
	                	packetReciver.recivePacket(this, ClientToServerPacket.parseFrom(data));
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
	
	public void send(byte[] bytes) {
		
		try {
			output.writeInt(bytes.length);
			output.write(bytes);
			output.flush();
		} catch (SocketException e) {
	        disconnect(); // client is gone
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
