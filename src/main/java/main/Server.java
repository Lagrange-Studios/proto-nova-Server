package main;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import socket.Player;
import socket.ServerSocketHandler;

public class Server {
	
	public Console console;
	private ServerSocketHandler serverSocket;
	private static final int TPS = 20;
	
	public Server() {
		
		// start console
		SwingUtilities.invokeLater(() -> {
            console = new Console();
            console.setVisible(true);
        });
		
		while (console == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		serverSocket = new ServerSocketHandler(console);
		startThread();
	}
	
	private void startThread() {
		try {
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			
			Runnable task = () -> tick();
			
			scheduler.scheduleAtFixedRate(task, 1, Math.round(1000/TPS), TimeUnit.MILLISECONDS);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void tick() {
		ArrayList<Player> playerList = serverSocket.getPlayerList();
		
		for (int i=0;i<playerList.size();i++) {
			Player player = playerList.get(i);
			
			if (!player.isConnected()) {
				playerList.remove(i);
			}
		}
	}
}
