package main;

import javax.swing.*;

import socket.Player;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    public Console(Server server) {
    	
    	this.server = server;
    	
        setTitle("Proto Nova Server Console");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
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
        infoBar.setText(
        		"TPS: "+ countedTicks + "  " + 
        		"Players: " + server.getPlayers().size()
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
                outputArea.append(" - kick [name]: Kicks the player with the correlated name\n");
                outputArea.append(" - echo [text]: Repeat the text\n\n");
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
            } else {
                outputArea.append("Unknown command. Type 'help' for options.\n\n");
            }
        }
    }

    public void print(String output) {
    	outputArea.append(output+"\n");
    }

	public void addTick() {
		countedTicks++;		
	}
}
