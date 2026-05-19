package socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLContext;
import main.Console;

public class ServerSocketHandler {

	private static final int PORT = main.ServerConfig.getInstance().getGameSocketPort();
	private static final int THREAD_POOL_SIZE = main.ServerConfig.getInstance().getThreadPoolSize();
	private SSLServerSocket serverSocket;
	private Console console;
	private ArrayList<Player> playerList;
	private ExecutorService threadPool;
	private Thread serverThread;
	private PacketMaker packetMaker;
	private socket.TokenManager tokenManager;
	
	public ServerSocketHandler(Console console, PacketReciver packetReciver) {
		this(console, packetReciver, null);
	}
	
	public ServerSocketHandler(Console console, PacketReciver packetReciver, socket.TokenManager tokenManager) {
		this.console = console;
		this.tokenManager = tokenManager;
		playerList = new ArrayList<Player>();
		threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		
		serverThread = new Thread(() -> {
			try {
				// Initialize SSL context for secure connections
				// Uses embedded keystore from resources - no file system dependency
				SSLContext sslContext = EmbeddedSSLProvider.getServerSSLContext();
				SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
				// Bind to 0.0.0.0 to allow connections from any network interface (NOT just localhost)
				serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));
				serverSocket.setReuseAddress(true);
				
				// Optional: Configure cipher suites and protocols for enhanced security
				String[] enabledProtocols = {"TLSv1.2", "TLSv1.3"};
				serverSocket.setEnabledProtocols(enabledProtocols);
	
				console.print("Hosting on port: " + String.valueOf(PORT) + " (SSL/TLS Enabled)");
				console.print("Hosting on 0.0.0.0 (all network interfaces)");
				console.print("Server is reachable from any connected network");
	
				while (!serverSocket.isClosed()) {
					Socket clientSocket = serverSocket.accept();
					// Don't add to playerList yet - only add when they send username
					Player player = new Player(clientSocket, console, packetReciver, this);
					threadPool.execute(() -> player.listen());
				}
			
				
			} catch (Exception e) {
				e.printStackTrace();
				console.print("SSL Error: " + e.getMessage());
			}
		});
		serverThread.setDaemon(true);
		serverThread.start();
	}
	
	/**
	 * Called by Player when username is received (handshake complete)
	 */
	public void addPlayerToGame(Player player) {
		playerList.add(player);
		console.print("Player joined: " + player.getUsername());
	}
	
	/**
	 * Called by Player when they disconnect
	 */
	public void removePlayer(Player player) {
		playerList.remove(player);
		if (player.getUsername() != null) {
			console.print("Player left: " + player.getUsername());
		}
	}
	
	public ArrayList<Player> getPlayerList() {
		return playerList;
	}
	
	public socket.TokenManager getTokenManager() {
		return tokenManager;
	}
	
	public void close() {
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (threadPool != null && !threadPool.isShutdown()) {
			threadPool.shutdown();
		}
		
		for (Player player : playerList) {
			player.disconnect();
		}
		playerList.clear();
	}
}