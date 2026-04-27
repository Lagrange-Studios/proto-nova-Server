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
import java.util.concurrent.ExecutorService;

import main.Console;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.PlayerDataProto.PlayerData;
import protonova.protobuf.UserDataProto;
import protonova.protobuf.UserDataProto.UserData;
import enums.Player.State;

public class Player {
	public Socket socket;
	private String username;
	private String clientToken; // Token provided by client for authentication
	
	private DataInputStream input;
	private DataOutputStream output;
	private volatile State state = State.DISCONNECTED;
	private Console console;
	public PlayerData data;
	private PacketReciver packetReciver;
	public boolean shouldReconcile = false;
	private ServerSocketHandler serverSocketHandler;
	private boolean addedToGame = false;
	private boolean tokenValidated = false; // Track if client has been authenticated
	
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
			e.printStackTrace();
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

	                // First message should contain token for authentication
	                if (!tokenValidated) {
	                    // Read token as string
	                    String receivedToken = new String(data, "UTF-8");
	                    
	                    // Validate token
	                    if (serverSocketHandler != null && serverSocketHandler.getTokenManager() != null) {
	                        if (serverSocketHandler.getTokenManager().validateClientToken(receivedToken)) {
	                            tokenValidated = true;
	                            clientToken = receivedToken;
	                            // Revoke token after use
	                            serverSocketHandler.getTokenManager().revokeClientToken(receivedToken);
	                            console.print("✓ Client authenticated successfully");
	                        } else {
	                            console.print("✗ Invalid token received, disconnecting client");
	                            disconnect();
	                            return;
	                        }
	                    } else {
	                        // No token manager, allow connection (backward compatibility)
	                        tokenValidated = true;
	                    }
	                }
	                // Second message should contain username
	                else if (username == null) {
	                    UserData user = UserData.parseFrom(data);
	                    username = user.getUsername();
	                    state = State.AWAITING_SERVER_PACKET;
	                    
	                    // Add to game when handshake complete
	                    if (serverSocketHandler != null) {
	                        addedToGame = true;
	                        serverSocketHandler.addPlayerToGame(this);
	                    }
	                    
	                    console.print("Received user: " + user.getUsername());
	                }
	                else {
	                    packetReciver.recivePacket(this, ClientToServerPacket.parseFrom(data));
	                }
	            }
	        } catch (IOException e) {
	            // Only log disconnection if player actually joined the game
	            if (addedToGame && username != null) {
	                console.print("Connection closed or error: " + e.getMessage());
	            }
	            disconnect();
	        }
	    });
	    thread.setName("Player-Listener-" + socket.getInetAddress().getHostAddress());
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

