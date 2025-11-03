package socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import main.Console;

public class ServerSocketHandler {

	private static final int PORT = 7675;
	private ServerSocket serverSocket;
	private Console console;
	private ArrayList<Player> playerList;
	
	public ServerSocketHandler(Console console) {
		this.console = console;
		playerList = new ArrayList<Player>();
		
		Thread thread = new Thread(() -> {
			try {
				serverSocket = new ServerSocket(PORT);
	
				console.print("Hosting on port: " + String.valueOf(PORT));
				console.print("Hosting on IP: " + String.valueOf(InetAddress.getLocalHost().getHostAddress()));
				console.print("NOTICE: Unless you're using a port forward or SSH tunnel.\n Only users on your network can connect");
	
				while (!serverSocket.isClosed()) {
					Socket clientSocket = serverSocket.accept();
					playerList.add(new Player(clientSocket));
					console.print("New player joined");
				}
			
				
			} catch (IOException e) {
				e.printStackTrace();
				console.print(e.getMessage());
			}
		});
		thread.start();
	}
	
	public ArrayList<Player> getPlayerList() {
		return playerList;
	}
}
