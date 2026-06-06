package main;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
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
import health.CombatManager;
import plane.PlaneManager;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import socket.PacketMaker;
import socket.PacketReciver;
import socket.Player;
import socket.ServerSocketHandler;
import socket.ServerStatusHandler;
import sound.SoundFinder;
import sound.SoundManager;
import space.CelestialObjectManager;
import tag.TagHandler;

public class Server {
	
	public Console console;
	private ServerSocketHandler serverSocket;
	private ServerStatusHandler statusHandler;
	public int TPS;
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
	private socket.TokenManager tokenManager;
	private TagHandler tagHandler;
	private CombatManager combatManager;
	private boolean headless;
	
	private int saveCounter = 0;
	private long saveInterval = 15 * 60 * 1000; // 15 minutes in milliseconds (real time, not TPS-dependent)
	private long lastSaveTime = 0;
	
	// Resource limit monitoring
	private OperatingSystemMXBean osBean;
	private ThreadMXBean threadBean;
	private int processorLimit;
	private long ramLimit;
	private int workerThreadLimit;
	private ScheduledFuture<?> resourceCheckTask;
	
	// TPS pause/resume functionality
	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> tickTask;
	private boolean tpsPaused = false;
	private long lastPlayerLeaveTime = 0;
	private static final long IDLE_TIMEOUT_MS = 60 * 1000; // 1 minute in milliseconds
	private ScheduledFuture<?> idleCheckTask;
	
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
		
		// Initialize configuration (read properties file once at startup)
		try {
			ServerConfig.initialize(console);
			this.TPS = ServerConfig.getInstance().getTicksPerSecond();
		} catch (Exception e) {
			console.print("✗ Failed to load configuration: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		ArrayList<Player> playerList = new ArrayList<>();
		
		validater = new Validater(console);
		
		boolean shouldGenerate = validater.validateWorldFiles();
		
		serverLoader = new ServerLoader(console);
		planeManager = new PlaneManager(serverLoader.loadWorld());
		entityManager = new EntityManager(serverLoader,console, playerList);
		soundManager = new SoundManager(serverLoader,console, this);
		chatManager = new ChatManager(serverLoader,console, this);
		
		assetManager = new AssetManager(entityManager,serverLoader.loadEntityAssets(), console);
		
		chunkManager = new ChunkManager(entityManager.getAllEntities());
		chunkManager.groupAllEntites();
		soundManager.setChunkManager(chunkManager);
		chatManager.setChunkManager(chunkManager);
		
		combatManager = new CombatManager(entityManager);
		
		entityFinder = new EntityFinder(entityManager.getAllEntities(),chunkManager);
		soundFinder = new SoundFinder(entityManager.getAllEntities(),soundManager.getAllSounds(),chunkManager);
		chatFinder = new ChatFinder(entityManager.getAllEntities(), chatManager.getAllChats(), chunkManager);
		
		celestialObjectManager = new CelestialObjectManager(serverLoader, console, this);
		
		generator = new Generator(console, planeManager, entityManager, assetManager, entityFinder, celestialObjectManager);

		craftingManager = new CraftingManager(entityManager, serverLoader.loadCraftingRecipes(), console, assetManager);

		tagHandler = new TagHandler(this, entityManager, assetManager, entityFinder, planeManager);
		entityManager.setClasses(chunkManager,tagHandler);
		
		if (shouldGenerate) {
			generator.generatePlanet("continents");
			
			// remove any entities at 0,0
			for (Entity entity : entityFinder.getAllEntitiesInRadius(Vector.newBuilder().setX(0).setY(0).build(),1,2)) {
				entityManager.removeEntity(entity);
			}
			
		}
		
		actionHandler = new ActionHandler(console, entityManager, entityFinder, planeManager, craftingManager, tagHandler, combatManager);

		packetReciver = new PacketReciver(entityManager, soundManager, chatManager, console, actionHandler, entityFinder);
		
		// Create TokenManager and pass it to ServerSocketHandler
		tokenManager = new socket.TokenManager(console);
		serverSocket = new ServerSocketHandler(console, packetReciver, tokenManager, playerList);
		statusHandler = new ServerStatusHandler(serverSocket, console, tokenManager);
		
		tagHandler.loadAllTagEntities();
		
		try {
			statusHandler.start();
		} catch (Exception e) {
			console.print("Failed to start status HTTP server: " + e.getMessage());
		}
		
		// Start certificate server for remote client connections
		try {
			socket.CertificateServer certServer = new socket.CertificateServer(console);
			certServer.start();
		} catch (Exception e) {
			console.print("Failed to start certificate server: " + e.getMessage());
		}
		
		serverSaver = new ServerSaver(this, entityManager, planeManager, celestialObjectManager);
		
		startThread();
		
		packetMaker = new PacketMaker(serverSocket,serverLoader,serverSaver,entityManager,entityFinder,soundFinder,chatFinder,planeManager,celestialObjectManager);
		
		console.setCommandClasses(serverSaver,generator,entityManager,planeManager,celestialObjectManager);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown();
		}));

	}
	
	private void startThread() {
		try {
			// Initialize resource monitoring
			java.lang.management.OperatingSystemMXBean osBeamBase = ManagementFactory.getOperatingSystemMXBean();
			if (osBeamBase instanceof com.sun.management.OperatingSystemMXBean) {
				osBean = (com.sun.management.OperatingSystemMXBean) osBeamBase;
			}
			threadBean = ManagementFactory.getThreadMXBean();
			processorLimit = ServerConfig.getInstance().getProcessorLimit();
			ramLimit = (long) ServerConfig.getInstance().getRamLimit() * 1024 * 1024; // Convert MB to bytes
			workerThreadLimit = ServerConfig.getInstance().getWorkerThreadLimit();
			
			scheduler = Executors.newScheduledThreadPool(3); // 3 threads: tick, idle check, resource check
			
			Runnable task = () -> {
				try {
					tick();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			
			// Schedule the main tick task (will be controlled by pause/resume logic)
			tickTask = scheduler.scheduleAtFixedRate(task, 1, Math.round(1000/TPS), TimeUnit.MILLISECONDS);
			
			// Schedule idle check task to run every 5 seconds
			Runnable idleCheck = () -> {
				try {
					checkPlayerIdleStatus();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			idleCheckTask = scheduler.scheduleAtFixedRate(idleCheck, 5, 5, TimeUnit.SECONDS);
			
			// Schedule resource check task to run every 10 seconds
			Runnable resourceCheck = () -> {
				try {
					checkResourceLimits();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			resourceCheckTask = scheduler.scheduleAtFixedRate(resourceCheck, 10, 10, TimeUnit.SECONDS);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void checkPlayerIdleStatus() {
		ArrayList<Player> playerList = getPlayers();
		
		if (playerList.isEmpty()) {
			// No players online
			if (!tpsPaused) {
				// Mark the time when players left
				if (lastPlayerLeaveTime == 0) {
					lastPlayerLeaveTime = System.currentTimeMillis();
				}
				
				// Check if idle timeout has been reached
				long idleTime = System.currentTimeMillis() - lastPlayerLeaveTime;
				if (idleTime >= IDLE_TIMEOUT_MS) {
					pauseTPS();
				}
			}
		} else {
			// Players are online
			if (tpsPaused) {
				// Resume TPS if it was paused
				resumeTPS();
			}
			// Reset idle timer
			lastPlayerLeaveTime = 0;
		}
	}
	
	private void pauseTPS() {
		if (!tpsPaused && tickTask != null) {
			console.print("No players online for 1 minute. Pausing TPS...");
			tpsPaused = true;
			tickTask.cancel(false);
		}
	}
	
	private void resumeTPS() {
		if (tpsPaused) {
			console.print("Player joined. Resuming TPS...");
			tpsPaused = false;
			lastPlayerLeaveTime = 0;
			
			try {
				Runnable task = () -> {
					try {
						tick();
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
				// Reschedule the tick task
				tickTask = scheduler.scheduleAtFixedRate(task, 1, Math.round(1000/TPS), TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void tick() throws Exception {
		// Create a copy to avoid ConcurrentModificationException when players disconnect during iteration
		ArrayList<Player> playerList = new ArrayList<>(serverSocket.getPlayerList());
		
		// Process player movements and generate walking sounds
		soundManager.processPlayerMovement(entityManager.getAllEntities());
		soundManager.processSoundMessagesToSend();
		
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
		tagHandler.tick();
		
		console.addTick();
		globalTicks++;
		
		saveCheck();
		
	}
	
	private void checkResourceLimits() {
		// Check processor usage
		if (processorLimit > 0) {
			double processorLoad = osBean.getProcessCpuLoad();
			int availableProcessors = osBean.getAvailableProcessors();
			int effectiveLimit = Math.min(processorLimit, availableProcessors);
			double limitedLoad = (double) effectiveLimit / availableProcessors;
			
			if (processorLoad > limitedLoad) {
				console.print("⚠ Processor usage exceeds limit: " + String.format("%.2f%%", processorLoad * 100) + 
					" (Limit: " + effectiveLimit + "/" + availableProcessors + " cores)");
			}
		}
		
		// Check memory usage
		if (ramLimit > 0) {
			Runtime runtime = Runtime.getRuntime();
			long usedMemory = runtime.totalMemory() - runtime.freeMemory();
			
			if (usedMemory > ramLimit) {
				console.print("⚠ RAM usage exceeds limit: " + (usedMemory / (1024 * 1024)) + " MB (Limit: " + (ramLimit / (1024 * 1024)) + " MB)");
				System.gc();
				console.print("✓ Garbage collection triggered");
			}
		}
		
		// Check thread count
		if (workerThreadLimit > 0) {
			long threadCount = threadBean.getThreadCount();
			
			if (threadCount > workerThreadLimit) {
				console.print("⚠ Thread count exceeds limit: " + threadCount + " (Limit: " + workerThreadLimit + ")");
			}
		}
	}
	
	private void saveCheck() {
		long currentTime = System.currentTimeMillis();
		
		if (lastSaveTime == 0) {
			lastSaveTime = currentTime;
		}
		
		if (currentTime - lastSaveTime > saveInterval) {
			lastSaveTime = currentTime;
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
			// Shutdown scheduler
			if (scheduler != null && !scheduler.isShutdown()) {
				if (resourceCheckTask != null) {
					resourceCheckTask.cancel(false);
				}
				if (idleCheckTask != null) {
					idleCheckTask.cancel(false);
				}
				if (tickTask != null) {
					tickTask.cancel(false);
				}
				scheduler.shutdown();
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
