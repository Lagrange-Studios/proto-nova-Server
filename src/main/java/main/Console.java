package main;

import javax.imageio.ImageIO;
import javax.swing.*;

import entity.EntityManager;
import file.ServerSaver;
import generation.Generator;
import protonova.protobuf.EntityProto.Entity;
import protonova.protobuf.VectorProto.Vector;
import socket.Player;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("serial")
public class Console extends JFrame {
    private JTextArea outputArea;
    private JTextField inputField;
    private JLabel infoBar;
    private Server server;
    private int countedTicks = 0;
    private ServerSaver serverSaver;
    private final double byteToGigaByteRatio = Math.pow(10, 9);
    private final double byteToMegaByteRatio = Math.pow(10, 6);
	private Generator generator;
	private EntityManager entityManager;

    public Console(Server server) {
    	
    	this.server = server;
    	
        setTitle("Proto Nova Server Console");
        setSize(600, 400);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set dark theme colors
        Color backgroundColor = new Color(30, 30, 30);
        Color textColor = new Color(220, 220, 220);
        Color inputBackground = new Color(45, 45, 45);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setBackground(backgroundColor);
        outputArea.setForeground(textColor);
        outputArea.setCaretColor(textColor);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.getViewport().setBackground(backgroundColor);

        inputField = new JTextField();
        inputField.setBackground(inputBackground);
        inputField.setForeground(textColor);
        inputField.setCaretColor(textColor);
        inputField.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputField.addActionListener(new InputListener());

        infoBar = new JLabel();
        infoBar.setOpaque(true);
        infoBar.setBackground(backgroundColor);
        infoBar.setForeground(textColor);
        infoBar.setFont(new Font("Consolas", Font.PLAIN, 14));
        infoBar.setText("TPS: 0");
        
        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);
        add(infoBar, BorderLayout.NORTH);

        
        try {
			this.setIconImage(ImageIO.read(new File("assets/ui/front.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        // Add a window listener for custom behavior
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Perform cleanup if needed
                System.out.println("Saving data");
                System.out.println(serverSaver.save());
                System.exit(0);  // Exit the application
            }
        });
        
        printWelcomeMessage();
        startUpdateThread();
    }
    
    private void startUpdateThread() {
    	try {
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			
			Runnable task = () -> updateBar();
			
			scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.SECONDS);
		} catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    private void updateBar() {
    	
    	if (countedTicks == 0) print("WARNING TPS is 0");
    	
    	Runtime runtime = Runtime.getRuntime();
    	
        infoBar.setText(
        		"TPS: "+ countedTicks + "  " + 
        		"Players: " + server.getPlayers().size() + "  " + 
        		"Memory: " +  Math.round((runtime.totalMemory() - runtime.freeMemory())/byteToMegaByteRatio) + "/" + Math.round(runtime.totalMemory()/byteToMegaByteRatio) + "MB"
        		);
        countedTicks = 0;
    }

    private void printWelcomeMessage() {
        outputArea.append("Welcome to Proto Nova Server Console 0.0.1\n");
        outputArea.append("Type 'help' for a list of commands, 'exit' to quit.\n\n");
    }

    private class InputListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String input = inputField.getText().trim();
            inputField.setText("");
            outputArea.append("> " + input + "\n");

            if (input.equalsIgnoreCase("exit")) {
                outputArea.append("Goodbye!\n");
                System.exit(0);
            } else if (input.equalsIgnoreCase("help")) {
                outputArea.append("Available commands:\n");
                outputArea.append(" - help: Show this help message\n");
                outputArea.append(" - time: Show current system time\n");
                outputArea.append(" - echo [text]: Repeat the text\n");
                outputArea.append(" - kick [name]: Kicks the player with the correlated name\n");
                outputArea.append(" - save: Saves all data to the world root\n");
                outputArea.append(" - players: Shows all currently connected players\n");
                outputArea.append(" - generate plane [generation type (optional)]: Generates a new plane optionally passing in a generation type\n");
                outputArea.append(" - state: shows all the players states");
                outputArea.append("\n");
            } else if (input.equalsIgnoreCase("time")) {
                outputArea.append("Current time: " + LocalTime.now() + "\n\n");
            } else if (input.startsWith("echo ")) {
                outputArea.append(input.substring(5) + "\n\n");
            } else if (input.startsWith("kick ")) {
            	String name = input.substring(5);
            	ArrayList<Player> playerList = server.getPlayers();
            	boolean found = false;
            	
            	for (int i=0;i<playerList.size();i++) {
            		if (playerList.get(i).getUsername().equals(name)) {
            			playerList.get(i).disconnect();
            			playerList.remove(i);

                    	outputArea.append("Kicked "+name+"\n");
            			found = true;
            			break;
            		}
            	}
            	
            	if (!found) {
                	outputArea.append("Failed to kick "+name+"\n");
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
            } else if (input.startsWith("generate plane")) {
            	if (input.length() == 14) {
            		generator.generateWorld();
            	} else if (input.length() > 15) {
                	String generationType = input.substring(15);
            		generator.generateWorld(generationType);
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
            		print(player.getUsername()+": "+player.getState());
            	}
            } else {
                outputArea.append("Unknown command. Type 'help' for options.\n\n");
            }
        }
    }

    public void print(String output) {
    	outputArea.append(output+"\n");
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
	
	public void setCommandClasses(ServerSaver serverSaver, Generator generator, EntityManager entityManager) {
		this.serverSaver = serverSaver;
		this.generator = generator;
		this.entityManager = entityManager;
	}
}
