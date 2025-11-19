package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import enums.Player.State;
import file.ServerLoader;
import protonova.protobuf.PlaneProto.Plane;
import protonova.protobuf.VectorProto.Vector;
import socket.PacketMaker;
import socket.Player;
import socket.ServerSocketHandler;

public class Server {
	
	public Console console;
	private ServerSocketHandler serverSocket;
	private final int TPS = 20;
	private ServerLoader serverLoader;
	private PacketMaker packetMaker;
	private HashMap<Integer, Plane> planes;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private ChunkManager chunkManager;
	
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
		entityManager = new EntityManager(serverLoader,console);
		
		chunkManager = new ChunkManager(entityManager.getAllEntities());
		chunkManager.groupAllEntites();
		entityManager.setChunkManager(chunkManager);
		
		entityFinder = new EntityFinder(entityManager.getAllEntities(),chunkManager);
		
		serverSocket = new ServerSocketHandler(console);
		startThread();
		
		packetMaker = new PacketMaker(serverSocket,serverLoader,entityManager,entityFinder,planes);
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
				console.print("Removed player: " +player.getUsername());
				// TODO: before removing save the player data
			}
			else {
				packetMaker.sendPacket(player);
			}
		}
		
		console.addTick();
	}
	
	public ArrayList<Player> getPlayers() {
		return serverSocket.getPlayerList();
	}
}
