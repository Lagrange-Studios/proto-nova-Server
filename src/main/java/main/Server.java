package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import entity.EntityManager;
import enums.Player.State;
import file.ServerLoader;
import protonova.protobuf.PlaneProto.Plane;
import socket.PacketMaker;
import socket.Player;
import socket.ServerSocketHandler;

public class Server {
	
	public Console console;
	private ServerSocketHandler serverSocket;
	private static final int TPS = 20;
	private ServerLoader serverLoader;
	private PacketMaker packetMaker;
	private HashMap<Integer, Plane> planes;
	private EntityManager entityManager;
	
	public Server() {
		
		// start console
		SwingUtilities.invokeLater(() -> {
            console = new Console(this);
            console.setVisible(true);
        });
		
		while (console == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		serverLoader = new ServerLoader(console);
		planes = serverLoader.loadWorld();
		entityManager = new EntityManager(serverLoader);
		
		serverSocket = new ServerSocketHandler(console);
		startThread();
		
		packetMaker = new PacketMaker(serverSocket,serverLoader);
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
			
			if (player.getState() == State.DISCONNECTED) {
				playerList.remove(i);
				// TODO: before removing save the player data
			}
			else {
				packetMaker.SendPacket(player);
			}
		}
	}
	
	public ArrayList<Player> getPlayers() {
		return serverSocket.getPlayerList();
	}
}
