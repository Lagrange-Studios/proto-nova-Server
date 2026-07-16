package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private static final int HANDSHAKE_TIMEOUT_MILLIS = 10_000;
	private static final int IDLE_TIMEOUT_MILLIS = Math.multiplyExact(
			main.ServerConfig.getInstance().getGameSocketIdleTimeoutSeconds(), 1_000);
	
	private DataInputStream input;
	private DataOutputStream output;
	private volatile State state = State.DISCONNECTED;
	private Console console;
	public PlayerData data;
	private PacketReciver packetReciver;
	public boolean shouldReconcile = false;
	private ServerSocketHandler serverSocketHandler;
	private volatile boolean addedToGame = false;
	private final AtomicBoolean disconnected = new AtomicBoolean(false);
	private final AtomicBoolean writeScheduled = new AtomicBoolean(false);
	private final ArrayBlockingQueue<byte[]> outboundPackets = new ArrayBlockingQueue<>(
			main.ServerConfig.getInstance().getGameSocketOutboundQueueSize());
	
	public final HashSet<Integer> entitiesSent = new HashSet<>();
	public final Set<Integer> updateList =  ConcurrentHashMap.newKeySet();
	public final Set<Integer> deleteList = ConcurrentHashMap.newKeySet();
	
	public Player(Socket socket, Console console, PacketReciver packetReciver, ServerSocketHandler serverSocketHandler) throws IOException {
		this.socket = socket;
		this.console = console;
		this.packetReciver = packetReciver;
		this.serverSocketHandler = serverSocketHandler;
		
		input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		state = State.AWAITING_CLIENT_PACKET;
	}
	
	public void disconnect() {
		if (!disconnected.compareAndSet(false, true)) return;
		state = State.DISCONNECTED;
		outboundPackets.clear();
		
		// Remove from game if added
		if (addedToGame && serverSocketHandler != null) {
			serverSocketHandler.removePlayer(this);
		}

	    try {
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
	            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
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
	                    socket.setSoTimeout(IDLE_TIMEOUT_MILLIS);
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
	        } catch (SocketTimeoutException e) {
	            console.print("Disconnected an inactive client" + (username == null ? "." : ": " + username));
	            disconnect();
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
		if (bytes == null || disconnected.get()) return;
		if (!outboundPackets.offer(bytes)) {
			console.print("Disconnected a client that could not receive data fast enough: "
					+ (username == null ? "unknown" : username));
			disconnect();
			return;
		}
		scheduleWriteIfNeeded();
	}

	private void scheduleWriteIfNeeded() {
		if (writeScheduled.compareAndSet(false, true)) {
			if (!serverSocketHandler.scheduleWrite(this)) {
				writeScheduled.set(false);
				disconnect();
			}
		}
	}

	void drainOutboundPackets() {
		try {
			byte[] bytes;
			while (!disconnected.get() && (bytes = outboundPackets.poll()) != null) {
				output.writeInt(bytes.length);
				output.write(bytes);
				output.flush();
				ResourceDiagnostics.recordNetworkWrite(bytes.length + Integer.BYTES);
			}
		} catch (SocketException e) {
			disconnect();
		} catch (IOException e) {
			disconnect();
		} finally {
			writeScheduled.set(false);
			if (!outboundPackets.isEmpty() && !disconnected.get()) {
				scheduleWriteIfNeeded();
			}
		}
	}
}
