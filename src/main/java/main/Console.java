package main;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import entity.EntityManager;
import file.ServerSaver;
import gamemode.GamemodeManager;
import generation.Generator;
import plane.PlaneManager;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import socket.Player;
import space.CelestialObjectManager;

public class Console {
    protected Server server;
    protected int countedTicks = 0;
    protected ServerSaver serverSaver;
    protected final double byteToGigaByteRatio = Math.pow(10, 9);
    protected final double byteToMegaByteRatio = Math.pow(10, 6);
	protected Generator generator;
	protected EntityManager entityManager;
	protected boolean headless;
	private PlaneManager planeManager;
	protected space.CelestialObjectManager celestialObjectManager;
	private GamemodeManager gamemodeManager;

    public Console(Server server) {
    	this(server, true);
    }
    
    public Console(Server server, boolean headless) {
    	
    	this.server = server;
    	this.headless = headless;
    	
    	if (headless) {
    		initHeadless();
    	}
    }
    
    protected void initHeadless() {
    	printWelcomeMessage();
    	startHeadlessInputThread();
    	startUpdateThread();
    }
    
    private void startHeadlessInputThread() {
    	Thread inputThread = new Thread(() -> {
    		try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
    			while (true) {
    				if (scanner.hasNextLine()) {
    					String input = scanner.nextLine().trim();
    					if (!input.isEmpty()) {
    						System.out.println("> " + input);
    						processInput(input);
    					}
    				}
    			}
    		}
    	});
    	inputThread.setDaemon(true);
    	inputThread.start();
    }
    
    protected void startUpdateThread() {
    	try {
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			
			Runnable task = () -> {
				try {
					updateBar();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			
			scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.SECONDS);
		} catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    protected void updateBar() throws Exception {
    	
    	if (countedTicks == 0 && server.isServerReady() && !server.getTPSPaused()) print("WARNING TPS is 0");
    	
    	Runtime runtime = Runtime.getRuntime();
    	
    	String infoText = "TPS: "+ countedTicks + "  " + 
        		"Players: " + server.getPlayers().size() + "  " + 
        		"Memory: " +  Math.round((runtime.totalMemory() - runtime.freeMemory())/byteToMegaByteRatio) + "/" + Math.round(runtime.totalMemory()/byteToMegaByteRatio) + "MB";
    	
    	if (server.getTPSPaused()) infoText = infoText + "  " + "[PAUSED]";
    	
    	if (!headless) {
    		onUpdateBar(infoText);
    	}
        countedTicks = 0;
    }
    
    protected void onUpdateBar(String infoText) {
    	// Override in GUI implementation
    }

    protected void printWelcomeMessage() {
        print("Welcome to Proto Nova Server Console 0.0.1");
        print("Type 'help' for a list of commands, 'exit' to quit.");
        print("");
    }

    protected void processInput(String input) {
    	if (input.equalsIgnoreCase("exit")) {
    		print("Goodbye!");
    		System.exit(0);
    	} else if (input.equalsIgnoreCase("help")) {
    		print("Available commands:");
    		print(" - help: Show this help message");
    		print(" - time: Show current system time");
    		print(" - time set day: Sets all celestial objects to day (rotation 0.5)");
    		print(" - time set night: Sets all celestial objects to night (rotation 0.0)");
    		print(" - echo [text]: Repeat the text");
    		print(" - kick [name]: Kicks the player with the correlated name");
    		print(" - save: Saves all data to the world root");
    		print(" - players: Shows all currently connected players");
    		print(" - generate planet [generation type (optional)]: Generates a new planet optionally passing in a generation type");
    		print(" - state: shows all the players states");
    		print(" - gamemode: shows current gamemode and time");
    		print("");
    	} else if (input.equalsIgnoreCase("time")) {
    		print("Current time: " + LocalTime.now());
    		print("");
    	} else if (input.startsWith("time set ")) {
    		String timeType = input.substring(9).toLowerCase();
    		setGameTime(timeType);
    	} else if (input.startsWith("echo ")) {
    		print(input.substring(5));
    		print("");
    	} else if (input.startsWith("kick ")) {
    		String name = input.substring(5);
    		ArrayList<Player> playerList = server.getPlayers();
    		boolean found = false;
    		
    		for (int i=0;i<playerList.size();i++) {
    			if (playerList.get(i).getUsername().equals(name)) {
    				playerList.get(i).disconnect();
    				playerList.remove(i);
    				print("Kicked "+name);
    				found = true;
    				break;
    			}
    		}
    		
    		if (!found) {
    			print("Failed to kick "+name);
    		}
    	} else if (input.equalsIgnoreCase("save")) {
    		if (serverSaver != null) {
    			print(serverSaver.save());
    		}
    		else {
    			print("No server saver attached to console");
    		}
    	} else if (input.equalsIgnoreCase("players")) {
    		print("Players:");
    		ArrayList<Player> players = server.getPlayers();
    		
        	for (int i=0;i<players.size();i++) {
        		print(players.get(i).getUsername());
        	}
    	} else if (input.startsWith("generate planet")) {
    		if (input.length() == 15) {
    			generator.generatePlanet();
    		} else if (input.length() > 16) {
        		String generationType = input.substring(16);
    			generator.generatePlanet(generationType);
    		} else {
    			print("Improper arguments");
    		}
    	} else if (input.startsWith("tp ")) {
    		String args = input.substring(3);
    		
    		String playerName;
    		float x;
    		float y;
    		int p;
    		
    		try {
    			int index1 = args.indexOf(' ');
    			playerName = args.substring(0,index1);
            	
            	int index2 = args.substring(index1+1).indexOf(' ')+index1+1;
            	x = Float.valueOf(args.substring(index1+1,index2));
            	
            	int index3 = args.substring(index2+1).indexOf(' ')+index2+1;
            	y = Float.valueOf(args.substring(index2+1,index3));
            	
            	p = Integer.valueOf(args.substring(index3+1));
            	
            	if (!planeManager.getPlanes().containsKey(p)) throw new Exception("Plane Id: "+p+" does not exist");
    		}
    		catch(Exception argumentError) {
    			print(argumentError.getMessage());
    			return;
    		}
    		Player selectedPlayer = null;
    		
    		for (Player player : server.getPlayers()) {
    			if (player.getUsername().equals(playerName)) {
    				selectedPlayer = player;
    				break;
    			}
    		}
    		
    		if (selectedPlayer == null) {
    			print("Couldn't find player: "+playerName);
    			return;
    		}
    		
    		Entity playerEntity = entityManager.getEntity(selectedPlayer);
    		
    		Vector newPosition = Vector.newBuilder()
    				.setX(x)
    				.setY(y)
    				.build();
    		
    		playerEntity = playerEntity.toBuilder()
    				.setPosition(newPosition)
    				.setMap(p)
    				.build();
    		
    		entityManager.updateEntity(playerEntity);
    	} else if (input.equalsIgnoreCase("state")) {
    		for (Player player : server.getPlayers()) {
    			print(player.getUsername()+": "+player.getState()+" Map: "+entityManager.getEntity(player).getMap());
    		}
    	} else if (input.equalsIgnoreCase("gamemode")) {
    		JSONObject gamemode = gamemodeManager.getGamemode();
    		print("Gamemode data:");
    		
    		for (String key : gamemode.keySet()) {
    			print(key+": "+gamemode.get(key));
    		}
    	} else {
    		print("Unknown command. Type 'help' for options.");
    		print("");
    	}
    }

    public void print(String output) {
    	System.out.println(output);
    }
    
    public void print(Boolean output) {
    	System.out.println(output);
    }
    
    public void print(int output) {
    	print(String.valueOf(output));
    }
    
    public void print(double output) {
    	print(String.valueOf(output));
    }

	public void addTick() {
		countedTicks++;		
	}
	
	public void setCommandClasses(ServerSaver serverSaver, Generator generator, EntityManager entityManager, PlaneManager planeManager, CelestialObjectManager celestialObjectManager, GamemodeManager gamemodeManager) {
		this.serverSaver = serverSaver;
		this.generator = generator;
		this.entityManager = entityManager;
		this.planeManager = planeManager;
		this.celestialObjectManager = celestialObjectManager;
		this.gamemodeManager = gamemodeManager;
	}
	
	private void setGameTime(String timeType) {
		if (celestialObjectManager == null) {
			print("Celestial object manager not initialized");
			print("");
			return;
		}
		
		double rotation;
		
		if (timeType.equals("day")) {
			rotation = 0.5;
		} else if (timeType.equals("night")) {
			rotation = 0.0;
		} else {
			print("Unknown time: " + timeType);
			print("Use 'time set day' or 'time set night'");
			print("");
			return;
		}
		
		// Update all celestial objects to the specified time
		for (protonova.protobuf.CelestialObjectProto.CelestialObject object : celestialObjectManager.getCelestialObjects().values()) {
			object = object.toBuilder()
					.setCurrentRotation(rotation)
					.build();
			celestialObjectManager.updateCelestialObject(object);
		}
		
		print("Set game time to " + timeType + " (rotation: " + rotation + ")");
		print("");
	}
	
	public void shutdown() {
		print("Server shutting down...");
	}
}
