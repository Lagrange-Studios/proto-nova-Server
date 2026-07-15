package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLSocket;

import main.Console;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.PlayerDataProto.PlayerData;
import protonova.protobuf.UserDataProto.UserData;
import enums.Player.State;
import diagnostics.ResourceDiagnostics;

public class Player {
	public Socket socket;
	private String username;
	private static final int MAX_PACKET_BYTES = 8 * 1024 * 1024;
	private static final int MAX_USERNAME_LENGTH = 32;
	
	private DataInputStream input;
	private DataOutputStream output;
	private volatile State state = State.DISCONNECTED;
	private Console console;
	public PlayerData data;
	private PacketReciver packetReciver;
	public boolean shouldReconcile = false;
	private ServerSocketHandler serverSocketHandler;
	private boolean addedToGame = false;
	
	public final HashSet<Integer> entitiesSent = new HashSet<>();
	public final Set<Integer> updateList =  ConcurrentHashMap.newKeySet();
	public final Set<Integer> deleteList = ConcurrentHashMap.newKeySet();
	
	public Player(Socket socket, Console console, PacketReciver packetReciver, ServerSocketHandler serverSocketHandler) {
		this.socket = socket;
		this.console = console;
		this.packetReciver = packetReciver;
		this.serverSocketHandler = serverSocketHandler;
		
		try {
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			state = State.AWAITING_CLIENT_PACKET;
		} catch (IOException e) {
			console.print("ERROR: Failed to initialize a client connection.");
		}
	}
	
	public void disconnect() {
		if (state == State.DISCONNECTED) return;
		state = State.DISCONNECTED;
		
		// Remove from game if added
		if (addedToGame && serverSocketHandler != null) {
			serverSocketHandler.removePlayer(this);
		}

	    try {
	    	if (input != null) input.close();
			if (output != null) output.close();
			if (socket != null) socket.close();
	    } catch (IOException ignored) {}
	}
	
	public State getState() {
		return state;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	public void listen() {
	        try {
	            socket.setSoTimeout(10_000);
	            if (socket instanceof SSLSocket) {
	                ((SSLSocket) socket).startHandshake();
	            }
	            while (state != State.DISCONNECTED) {
	            	
	                int length = input.readInt();
	                if (length <= 0 || length > MAX_PACKET_BYTES) {
	                    console.print("WARNING: Rejected an invalid packet size from a client.");
	                    disconnect();
	                    return;
	                }
	                byte[] data = new byte[length];
	                input.readFully(data);
	                ResourceDiagnostics.recordNetworkRead(length + Integer.BYTES);

	                if (username == null) {
	                    UserData user = UserData.parseFrom(data);
	                    String requestedUsername = user.getUsername().trim();
	                    if (!isValidUsername(requestedUsername)) {
	                        console.print("WARNING: Rejected a client with an invalid username.");
	                        disconnect();
	                        return;
	                    }
	                    username = requestedUsername;
	                    socket.setSoTimeout(0);
	                    state = State.AWAITING_SERVER_PACKET;
	                    
	                    if (serverSocketHandler != null) {
	                        addedToGame = true;
	                        serverSocketHandler.addPlayerToGame(this);
	                    }
	                    
	                }
	                else {
	                    packetReciver.recivePacket(this, ClientToServerPacket.parseFrom(data));
	                }
	            }
	        } catch (IOException e) {
	            disconnect();
	        }
	}

	private static boolean isValidUsername(String value) {
		return !value.isEmpty() && value.length() <= MAX_USERNAME_LENGTH
				&& value.matches("[A-Za-z0-9 _.-]+");
	}

	public String getUsername() {
		return username;
	}
	
	public void send(byte[] bytes) {
		
		try {
			output.writeInt(bytes.length);
			output.write(bytes);
			output.flush();
			ResourceDiagnostics.recordNetworkWrite(bytes.length + Integer.BYTES);
		} catch (SocketException e) {
	        disconnect(); // client is gone
	    } catch (IOException e) {
	        disconnect();
	    }
	}
}
