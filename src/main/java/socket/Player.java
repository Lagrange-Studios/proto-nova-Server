package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.security.SecureRandom;
import javax.net.ssl.SSLSocket;

import main.Console;
import protonova.protobuf.ClientToServerPacketProto.ClientToServerPacket;
import protonova.protobuf.PlayerDataProto.PlayerData;
import protonova.protobuf.UserDataProto.UserData;
import protonova.protobuf.ServerHandshakeProto.ServerHandshake;
import com.google.protobuf.ByteString;
import enums.Player.State;
import diagnostics.ResourceDiagnostics;
import security.GameAuthService;
import security.GameAuthService.JoinResult;
import security.GameAuthService.ValidationStatus;

public class Player {
	public Socket socket;
	private String username;
	private static final int MAX_PACKET_BYTES = 256 * 1024;
	private static final int MAX_USERNAME_LENGTH = 32;
	private static final int HANDSHAKE_TIMEOUT_MILLIS = 20_000;
	private static final int SECURITY_LEVEL = main.ServerConfig.getInstance().getSecurityLevel();
	private static final long LEVEL_3_RECHECK_MILLIS = Math.multiplyExact(
			main.ServerConfig.getInstance().getSecurityLevel3RecheckSeconds(), 1_000L);
	private static final long LEVEL_3_GRACE_MILLIS = Math.multiplyExact(
			main.ServerConfig.getInstance().getSecurityLevel3ApiGraceSeconds(), 1_000L);
	private static final int IDLE_TIMEOUT_MILLIS = Math.multiplyExact(
			main.ServerConfig.getInstance().getGameSocketIdleTimeoutSeconds(), 1_000);
	// The official client sends one gameplay packet per 60 Hz client tick. Allow
	// enough headroom for its bounded catch-up ticks while still rejecting a
	// sustained packet flood.
	private static final int MAX_PACKETS_PER_SECOND = 120;
	private static final long CHAT_INTERVAL_NANOS = 250_000_000L;
	private static final long INTERACTION_INTERVAL_NANOS = 20_000_000L;
	private static final int OUTBOUND_QUEUE_CAPACITY = Math.max(
			main.ServerConfig.getInstance().getGameSocketOutboundQueueSize(),
			Math.multiplyExact(
					main.ServerConfig.getInstance().getTicksPerSecond(),
					main.ServerConfig.getInstance().getGameSocketSlowClientTimeoutSeconds()));
	
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
	private final ArrayBlockingQueue<byte[]> outboundPackets = new ArrayBlockingQueue<>(OUTBOUND_QUEUE_CAPACITY);
	private long packetWindowStarted = System.nanoTime();
	private int packetsInWindow;
	private long lastChatMessage;
	private long lastInteraction;
	private final GameAuthService gameAuthService;
	private byte[] securityChallenge;
	private String apiSessionKey;
	private long nextApiValidation;
	private long apiUnavailableSince;
	private CompletableFuture<ValidationStatus> apiValidation;
	
	public final HashSet<Integer> entitiesSent = new HashSet<>();
	public final Set<Integer> updateList =  ConcurrentHashMap.newKeySet();
	public final Set<Integer> deleteList = ConcurrentHashMap.newKeySet();
	public final ArrayList<String> messageList = new ArrayList<>();
	
	public Player(Socket socket, Console console, PacketReciver packetReciver,
			ServerSocketHandler serverSocketHandler, GameAuthService gameAuthService) throws IOException {
		this.socket = socket;
		this.console = console;
		this.packetReciver = packetReciver;
		this.serverSocketHandler = serverSocketHandler;
		this.gameAuthService = gameAuthService;
		
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
	            sendSecurityHandshake();
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
	                    if (SECURITY_LEVEL >= 2) {
	                        JoinResult result = gameAuthService.consumeJoinTicket(
	                                user.getJoinTicket(), securityChallenge, SECURITY_LEVEL == 3);
	                        if (result == null) {
	                            console.print("WARNING: Rejected a client that failed account verification.");
	                            disconnect();
	                            return;
	                        }
	                        requestedUsername = result.getUsername().trim();
	                        apiSessionKey = result.getSessionKey();
	                        nextApiValidation = System.currentTimeMillis() + LEVEL_3_RECHECK_MILLIS;
	                    }
	                    if (!isValidUsername(requestedUsername)) {
	                        console.print("WARNING: Rejected a client with an invalid username.");
	                        disconnect();
	                        return;
	                    }
	                    username = requestedUsername;
	                    socket.setSoTimeout(IDLE_TIMEOUT_MILLIS);
	                    state = State.AWAITING_SERVER_PACKET;
	                    
	                    if (serverSocketHandler != null) {
	                        if (!serverSocketHandler.addPlayerToGame(this)) {
	                            console.print("WARNING: Rejected a duplicate or unavailable player session: " + username);
	                            disconnect();
	                            return;
	                        }
	                        addedToGame = true;
	                    }
	                    
	                }
	                else {
	                    if (!allowApiSession()) return;
	                    if (!allowGameplayPacket()) {
	                        console.print("WARNING: Disconnected a client sending packets too quickly: " + username);
	                        disconnect();
	                        return;
	                    }
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

	private void sendSecurityHandshake() throws IOException {
		ClientDistribution distribution = main.ServerConfig.getInstance().isStatusHttpEnabled()
				? ClientDistribution.getIfAvailable() : null;
		securityChallenge = new byte[SECURITY_LEVEL >= 2 ? 32 : 0];
		if (securityChallenge.length > 0) new SecureRandom().nextBytes(securityChallenge);
		ServerHandshake.Builder handshake = ServerHandshake.newBuilder()
				.setSecurityLevel(SECURITY_LEVEL)
				.setChallenge(ByteString.copyFrom(securityChallenge));
		if (distribution != null) {
			handshake.setClientVersion(distribution.getVersion())
					.setClientDownloadPort(main.ServerConfig.getInstance().getStatusHttpPort())
					.setClientManifestSha256(distribution.getManifestSha256());
		}
		byte[] bytes = handshake.build().toByteArray();
		output.writeInt(bytes.length);
		output.write(bytes);
		output.flush();
		ResourceDiagnostics.recordNetworkWrite(bytes.length + Integer.BYTES);
	}

	private boolean allowApiSession() {
		if (SECURITY_LEVEL != 3) return true;
		long now = System.currentTimeMillis();
		if (apiValidation != null && apiValidation.isDone()) {
			ValidationStatus status;
			try {
				status = apiValidation.getNow(ValidationStatus.UNAVAILABLE);
			} catch (Exception e) {
				status = ValidationStatus.UNAVAILABLE;
			}
			apiValidation = null;
			if (status == ValidationStatus.REJECTED) {
				console.print("Disconnected a player whose API session was revoked: " + username);
				disconnect();
				return false;
			}
			if (status == ValidationStatus.VALID) {
				apiUnavailableSince = 0;
				nextApiValidation = now + LEVEL_3_RECHECK_MILLIS;
			} else {
				if (apiUnavailableSince == 0) apiUnavailableSince = now;
				nextApiValidation = now + Math.min(LEVEL_3_RECHECK_MILLIS, 15_000L);
			}
		}
		if (apiUnavailableSince != 0 && now - apiUnavailableSince >= LEVEL_3_GRACE_MILLIS) {
			console.print("Disconnected a player because the security API grace period expired: " + username);
			disconnect();
			return false;
		}
		if (apiValidation == null && now >= nextApiValidation) {
			apiValidation = gameAuthService.validateSessionAsync(apiSessionKey);
		}
		return true;
	}

	private boolean allowGameplayPacket() {
		long now = System.nanoTime();
		if (now - packetWindowStarted >= 1_000_000_000L) {
			packetWindowStarted = now;
			packetsInWindow = 0;
		}
		return ++packetsInWindow <= MAX_PACKETS_PER_SECOND;
	}

	public boolean allowChatMessage() {
		long now = System.nanoTime();
		if (now - lastChatMessage < CHAT_INTERVAL_NANOS) return false;
		lastChatMessage = now;
		return true;
	}

	public boolean allowInteraction() {
		long now = System.nanoTime();
		if (now - lastInteraction < INTERACTION_INTERVAL_NANOS) return false;
		lastInteraction = now;
		return true;
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
