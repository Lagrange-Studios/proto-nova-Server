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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import main.Console;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.PlayerDataProto.PlayerData;
import protonova.protobuf.UserDataProto;
import protonova.protobuf.UserDataProto.UserData;
import enums.Player.State;
import diagnostics.ResourceDiagnostics;

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
	            	
	                int length = input.readInt();
	                
	                byte[] data = new byte[length];
	                input.readFully(data);
	                ResourceDiagnostics.recordNetworkRead(length + Integer.BYTES);

	                if (!tokenValidated) {
	                    String receivedToken = new String(data, "UTF-8");
	                    
	                    if (serverSocketHandler != null && serverSocketHandler.getTokenManager() != null) {
	                        if (serverSocketHandler.getTokenManager().validateClientToken(receivedToken)) {
	                            tokenValidated = true;
	                            clientToken = receivedToken;
	                            
	                            String newToken = serverSocketHandler.getTokenManager().generateRenewedToken(receivedToken);
	                            if (newToken != null) {
	                                console.print("✓ Client authenticated successfully - Token renewed for 30 days");
	                            } else {
	                                console.print("✓ Client authenticated successfully");
	                            }
	                        } else {
	                            console.print("✗ Invalid token received, disconnecting client");
	                            disconnect();
	                            return;
	                        }
	                    } else {
	                        tokenValidated = true;
	                    }
	                }
	                else if (username == null) {
	                    UserData user = UserData.parseFrom(data);
	                    username = user.getUsername();
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
			ResourceDiagnostics.recordNetworkWrite(bytes.length + Integer.BYTES);
		} catch (SocketException e) {
	        disconnect(); // client is gone
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
