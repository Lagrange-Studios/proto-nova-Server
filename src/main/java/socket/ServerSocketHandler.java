package socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
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
	private ExecutorService outboundThreadPool;
	private Thread serverThread;
	private ServerSaver serverSaver;
	private volatile boolean closing;
	
	public ServerSocketHandler(Console console, PacketReciver packetReciver, ArrayList<Player> playerList,ServerSaver serverSaver) {
		this.console = console;
		this.playerList = playerList;
		this.serverSaver = serverSaver;
		threadPool = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
				new SynchronousQueue<>(),
				ResourceDiagnostics.threadFactory("Player-Worker"));
		outboundThreadPool = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(Math.max(THREAD_POOL_SIZE * 4, 20)),
				ResourceDiagnostics.threadFactory("Player-Writer"));
		
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
	
				while (!closing && !serverSocket.isClosed()) {
					Socket clientSocket = serverSocket.accept();
					try {
						clientSocket.setKeepAlive(true);
						clientSocket.setTcpNoDelay(true);
						Player player = new Player(clientSocket, console, packetReciver, this);
						threadPool.execute(player::listen);
					} catch (RejectedExecutionException overloaded) {
						clientSocket.close();
					} catch (Exception connectionError) {
						clientSocket.close();
					}
				}
			
				
			} catch (SocketException e) {
				if (!closing) {
					console.print("ERROR: Secure game connection failed: " + safeMessage(e));
				}
			} catch (Exception e) {
				if (!closing && (serverSocket == null || !serverSocket.isClosed())) {
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
		synchronized (this) {
			if (closing || player.getState() == enums.Player.State.DISCONNECTED || playerList.contains(player)) return;
			playerList.add(player);
		}
		console.print("Player joined: " + player.getUsername());
	}
	
	/**
	 * Called by Player when they disconnect
	 */
	public void removePlayer(Player player) {
		synchronized (this) {
			if (!playerList.remove(player)) return;
		}
		console.print("Removed player: " + (player.getUsername() != null ? player.getUsername() : "unknown"));
		if (player.data != null) {
			serverSaver.savePlayer(player);
		}
	}
	
	public synchronized ArrayList<Player> getPlayerList() {
		return new ArrayList<>(playerList);
	}

	boolean scheduleWrite(Player player) {
		if (closing || outboundThreadPool == null || outboundThreadPool.isShutdown()) return false;
		try {
			outboundThreadPool.execute(player::drainOutboundPackets);
			return true;
		} catch (RejectedExecutionException overloaded) {
			return false;
		}
	}
	
	public void close() {
		if (closing) return;
		closing = true;
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			console.print("WARNING: Error while closing the secure game connection.");
		}
		
		for (Player player : getPlayerList()) {
			player.disconnect();
		}
		synchronized (this) {
			playerList.clear();
		}

		shutdownPool(threadPool);
		shutdownPool(outboundThreadPool);
	}

	private void shutdownPool(ExecutorService pool) {
		if (pool == null || pool.isShutdown()) return;
		pool.shutdownNow();
		try {
			if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
				console.print("WARNING: A socket worker did not stop cleanly.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
