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
import file.ServerSaver;
import file.Validater;
import generation.Generator;
import protonova.protobuf.PlaneProto.Plane;
import socket.PacketMaker;
import socket.PacketReciver;
import socket.Player;
import socket.ServerSocketHandler;
import space.CelestialObjectManager;

public class Server {
	
	public Console console;
	private ServerSocketHandler serverSocket;
	private final int TPS = 20;
	private ServerLoader serverLoader;
	private ServerSaver serverSaver;
	private PacketMaker packetMaker;
	private PacketReciver packetReciver;
	private HashMap<Integer, Plane> planes;
	private EntityManager entityManager;
	private EntityFinder entityFinder;
	private ChunkManager chunkManager;
	private CelestialObjectManager celestialObjectManager;
	private Validater validater;
	private Generator generator;
	
	private int saveCounter = 0;
	private int saveInterval = 15 * 60 * TPS; // Minutes
	
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
		
		validater = new Validater(console);
		
		boolean shouldGenerate = validater.validateWorldFiles();
		
		serverLoader = new ServerLoader(console);
		planes = serverLoader.loadWorld();
		entityManager = new EntityManager(serverLoader,console);
		
		chunkManager = new ChunkManager(entityManager.getAllEntities());
		chunkManager.groupAllEntites();
		entityManager.setChunkManager(chunkManager);
		
		entityFinder = new EntityFinder(entityManager.getAllEntities(),chunkManager);
		
		celestialObjectManager = new CelestialObjectManager(serverLoader, console);
		
		generator = new Generator(console, planes, entityManager);
		if (shouldGenerate) {
			generator.generateWorld();
		}
		
		packetReciver = new PacketReciver(entityFinder, chunkManager, entityManager);
		
		serverSocket = new ServerSocketHandler(console, packetReciver);
		serverSaver = new ServerSaver(this, entityManager, planes);
		
		startThread();
		
		packetMaker = new PacketMaker(serverSocket,serverLoader,entityManager,entityFinder,planes);
		
		console.setCommandClasses(serverSaver,generator,entityManager);

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
				serverSaver.savePlayer(player);
			}
			else {
				packetMaker.sendPacket(player);
			}
		}
		console.addTick();
		
		saveCheck();
	}
	
	private void saveCheck() {
		saveCounter++;
		
		if (saveCounter > saveInterval) {
			saveCounter = 0;
			console.print(serverSaver.save());
		}
	}
	
	public ArrayList<Player> getPlayers() {
		return serverSocket.getPlayerList();
	}
}
