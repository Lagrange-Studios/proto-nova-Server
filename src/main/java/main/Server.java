package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import action.ActionHandler;
import action.CraftingManager;
import chat.ChatFinder;
import chat.ChatManager;
import entity.ChunkManager;
import entity.EntityFinder;
import entity.EntityManager;
import enums.Player.State;
import file.AssetManager;
import file.ServerLoader;
import file.ServerSaver;
import file.Validater;
import generation.Generator;
import plane.PlaneManager;
import protonova.protobuf.PlaneProto.Plane;
import socket.PacketMaker;
import socket.PacketReciver;
import socket.Player;
import socket.ServerSocketHandler;
import socket.ServerStatusHandler;
import sound.SoundFinder;
import sound.SoundManager;
import space.CelestialObjectManager;

public class Server {
	
	public Console console;
	private ServerSocketHandler serverSocket;
	private ServerStatusHandler statusHandler;
	public final int TPS = 60;
	public Long globalTicks = 0L;
	private ServerLoader serverLoader;
	private ServerSaver serverSaver;
	private PacketMaker packetMaker;
	private PacketReciver packetReciver;
	private PlaneManager planeManager;
	private EntityManager entityManager;
	private SoundManager soundManager;
	private ChatManager chatManager;
	private EntityFinder entityFinder;
	private SoundFinder soundFinder;
	private ChatFinder chatFinder;
	private ChunkManager chunkManager;
	private CelestialObjectManager celestialObjectManager;
	private Validater validater;
	private Generator generator;
	private AssetManager assetManager;
	private ActionHandler actionHandler;
	private CraftingManager craftingManager;
	private boolean headless;
	
	private int saveCounter = 0;
	private int saveInterval = 15 * 60 * TPS; // Minutes
	
	public Server() {
		this(false);
	}
	
	public Server(boolean headless) {
		this.headless = headless;
		
		
		// start console
		if (headless) {
			console = new Console(this, true);
		} else {
			SwingUtilities.invokeLater(() -> {
                console = new ConsoleGUI(this, false);
            });
			
			while (console == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		validater = new Validater(console);
		
		boolean shouldGenerate = validater.validateWorldFiles();
		
		serverLoader = new ServerLoader(console);
		planeManager = new PlaneManager(serverLoader.loadWorld());
		entityManager = new EntityManager(serverLoader,console);
		soundManager = new SoundManager(serverLoader,console, this);
		chatManager = new ChatManager(serverLoader,console, this);
		
		assetManager = new AssetManager(entityManager,serverLoader.loadEntityAssets(), console);
		
		chunkManager = new ChunkManager(entityManager.getAllEntities());
		chunkManager.groupAllEntites();
		entityManager.setChunkManager(chunkManager);
		soundManager.setChunkManager(chunkManager);
		chatManager.setChunkManager(chunkManager);
		
		entityFinder = new EntityFinder(entityManager.getAllEntities(),chunkManager);
		soundFinder = new SoundFinder(entityManager.getAllEntities(),soundManager.getAllSounds(),chunkManager);
		chatFinder = new ChatFinder(entityManager.getAllEntities(), chatManager.getAllChats(), chunkManager);
		
		celestialObjectManager = new CelestialObjectManager(serverLoader, console, this);
		
		generator = new Generator(console, planeManager, entityManager, assetManager, entityFinder, celestialObjectManager);
		
		craftingManager = new CraftingManager(entityManager, serverLoader.loadCraftingRecipes(), console);
		
		actionHandler = new ActionHandler(console, entityManager, entityFinder, planeManager);
		
		if (shouldGenerate) {
			generator.generatePlanet("continents");
		}

		packetReciver = new PacketReciver(entityManager, soundManager, chatManager, console, actionHandler, entityFinder);
		
		serverSocket = new ServerSocketHandler(console, packetReciver);
		statusHandler = new ServerStatusHandler(serverSocket, console);
		
		try {
			statusHandler.start();
		} catch (Exception e) {
			console.print("Failed to start status HTTP server: " + e.getMessage());
		}
		
		serverSaver = new ServerSaver(this, entityManager, planeManager, celestialObjectManager);
		
		startThread();
		
		packetMaker = new PacketMaker(serverSocket,serverLoader,entityManager,entityFinder,soundFinder,chatFinder,planeManager,celestialObjectManager);
		
		console.setCommandClasses(serverSaver,generator,entityManager,planeManager);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown();
		}));

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
		// Create a copy to avoid ConcurrentModificationException when players disconnect during iteration
		ArrayList<Player> playerList = new ArrayList<>(serverSocket.getPlayerList());
		
		for (Player player : playerList) {
			if (player.getState() == State.DISCONNECTED) {
				serverSocket.getPlayerList().remove(player);
				console.print("Removed player: " + (player.getUsername() != null ? player.getUsername() : "unknown"));
				serverSaver.savePlayer(player);
			}
			else {
				packetMaker.sendPacket(player);
				player.shouldReconcile = false; // Reset reconciliation flag after sending
			}
		}
		chatManager.processChatMessagesToSend();
		
		celestialObjectManager.tickCelestialObjects();
		
		console.addTick();
		globalTicks++;
		
		saveCheck();
		
		entityManager.clearRemovedEntities();
	}
	
	private void saveCheck() {
		saveCounter++;
		
		if (saveCounter > saveInterval) {
			saveCounter = 0;
			console.print(serverSaver.save());
		}
	}
	
	public ArrayList<Player> getPlayers() {
		return serverSocket!=null?serverSocket.getPlayerList():new ArrayList<Player>();
	}
	
	private void shutdown() {
		try {
			console.shutdown();
			if (statusHandler != null) {
				statusHandler.stop();
			}
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
