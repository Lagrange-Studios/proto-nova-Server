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
import file.AssetManager;
import file.ServerLoader;
import file.ServerSaver;
import file.Validater;
import generation.Generator;
import protonova.protobuf.PlaneProto.Plane;
import socket.PacketMaker;
import socket.PacketReciver;
import socket.Player;
import socket.ServerSocketHandler;
import sound.SoundFinder;
import sound.SoundManager;
import space.CelestialObjectManager;

public class Server {
	
	public Console console;
	private ServerSocketHandler serverSocket;
	private final int TPS = 60;
	public Long globalTicks = 0L;
	private ServerLoader serverLoader;
	private ServerSaver serverSaver;
	private PacketMaker packetMaker;
	private PacketReciver packetReciver;
	private HashMap<Integer, Plane> planes;
	private EntityManager entityManager;
	private SoundManager soundManager;
	private EntityFinder entityFinder;
	private SoundFinder soundFinder;
	private ChunkManager chunkManager;
	private CelestialObjectManager celestialObjectManager;
	private Validater validater;
	private Generator generator;
	private AssetManager assetManager;
	
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
		soundManager = new SoundManager(serverLoader,console, this);
		assetManager = new AssetManager(entityManager,serverLoader.loadEntityAssets());
		
		chunkManager = new ChunkManager(entityManager.getAllEntities());
		chunkManager.groupAllEntites();
		entityManager.setChunkManager(chunkManager);
		soundManager.setChunkManager(chunkManager);
		
		entityFinder = new EntityFinder(entityManager.getAllEntities(),chunkManager);
		soundFinder = new SoundFinder(entityManager.getAllEntities(),soundManager.getAllSounds(),chunkManager);
		
		celestialObjectManager = new CelestialObjectManager(serverLoader, console);
		
		generator = new Generator(console, planes, entityManager, assetManager, entityFinder);
		if (shouldGenerate) {
			generator.generateWorld();
		}
		
		packetReciver = new PacketReciver(entityFinder, chunkManager, entityManager, soundManager);
		
		serverSocket = new ServerSocketHandler(console, packetReciver);
		serverSaver = new ServerSaver(this, entityManager, planes);
		
		startThread();
		
		packetMaker = new PacketMaker(serverSocket,serverLoader,entityManager,entityFinder,soundFinder,planes);
		
		console.setCommandClasses(serverSaver,generator,entityManager);

	}
	
	private void startThread() {
		try {
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			
			Runnable task = () -> {
				try {
					tick();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			
			scheduler.scheduleAtFixedRate(task, 1, Math.round(1000/TPS), TimeUnit.MILLISECONDS);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void tick() throws Exception {
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
		globalTicks++;
		
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
