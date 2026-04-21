package socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import main.Console;

public class ServerSocketHandler {

	private static final int PORT = 7675;
	private static final int THREAD_POOL_SIZE = 50; // Max concurrent player listener threads
	private ServerSocket serverSocket;
	private Console console;
	private ArrayList<Player> playerList;
	private ExecutorService threadPool;
	private Thread serverThread;
	
	public ServerSocketHandler(Console console, PacketReciver packetReciver) {
		this.console = console;
		playerList = new ArrayList<Player>();
		threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		
		serverThread = new Thread(() -> {
			try {
				serverSocket = new ServerSocket(PORT);
				serverSocket.setReuseAddress(true);
	
				console.print("Hosting on port: " + String.valueOf(PORT));
				console.print("Hosting on IP: " + String.valueOf(InetAddress.getLocalHost().getHostAddress()));
				console.print("NOTICE: Unless you're using a port forward or SSH tunnel.\n Only users on your network can connect");
	
				while (!serverSocket.isClosed()) {
					Socket clientSocket = serverSocket.accept();
					Player player = new Player(clientSocket, console, packetReciver);
					playerList.add(player);
					console.print("New player joined");
				}
			
				
			} catch (IOException e) {
				e.printStackTrace();
				console.print(e.getMessage());
			}
		});
		serverThread.setDaemon(true);
		serverThread.start();
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
