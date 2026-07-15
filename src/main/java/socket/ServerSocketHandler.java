package socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import file.ServerSaver;

import javax.net.ssl.SSLContext;
import main.Console;
import diagnostics.ResourceDiagnostics;
import security.ServerTlsContext;

public class ServerSocketHandler {

	private static final int PORT = main.ServerConfig.getInstance().getGameSocketPort();
	private static final int THREAD_POOL_SIZE = main.ServerConfig.getInstance().getThreadPoolSize();
	private SSLServerSocket serverSocket;
	private Console console;
	private ArrayList<Player> playerList;
	private ExecutorService threadPool;
	private Thread serverThread;
	private PacketMaker packetMaker;
	private ServerSaver serverSaver;
	
	public ServerSocketHandler(Console console, PacketReciver packetReciver, ArrayList<Player> playerList,ServerSaver serverSaver) {
		this.console = console;
		this.playerList = playerList;
		this.serverSaver = serverSaver;
		threadPool = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(Math.max(THREAD_POOL_SIZE * 4, 20)),
				ResourceDiagnostics.threadFactory("Player-Worker"));
		
		serverThread = ResourceDiagnostics.newThread("Server-Socket-Acceptor", () -> {
			try {
				// Initialize SSL context for secure connections
				// Uses embedded keystore from resources - no file system dependency
				SSLContext sslContext = ServerTlsContext.create();
				SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
				// Wildcard binding accepts LAN and public/port-forwarded connections.
				serverSocket = (SSLServerSocket) ssf.createServerSocket();
				serverSocket.setReuseAddress(true);
				serverSocket.bind(new InetSocketAddress(PORT), 50);
				
				// Optional: Configure cipher suites and protocols for enhanced security
				String[] enabledProtocols = {"TLSv1.2", "TLSv1.3"};
				serverSocket.setEnabledProtocols(enabledProtocols);
	
				console.print("Server ready: secure game listener active on TCP port " + PORT + ". Type 'help' for commands.");
	
				while (!serverSocket.isClosed()) {
					Socket clientSocket = serverSocket.accept();
					try {
						Player player = new Player(clientSocket, console, packetReciver, this);
						threadPool.execute(player::listen);
					} catch (RejectedExecutionException overloaded) {
						clientSocket.close();
					}
				}
			
				
			} catch (Exception e) {
				if (serverSocket == null || !serverSocket.isClosed()) {
					console.print("ERROR: Secure game connection failed: " + safeMessage(e));
				}
				
			}
		});
		serverThread.setDaemon(true);
		serverThread.start();
	}

	private static String safeMessage(Exception exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
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
		console.print("Removed player: " + (player.getUsername() != null ? player.getUsername() : "unknown"));
		if (player.data != null) {
			serverSaver.savePlayer(player);
		}
	}
	
	public ArrayList<Player> getPlayerList() {
		return playerList;
	}
	
	public void close() {
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			console.print("WARNING: Error while closing the secure game connection.");
		}
		
		if (threadPool != null && !threadPool.isShutdown()) {
			threadPool.shutdown();
		}
		
		for (Player player : new ArrayList<>(playerList)) {
			player.disconnect();
		}
		playerList.clear();
	}
}
