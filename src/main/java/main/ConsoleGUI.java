package main;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import diagnostics.AdvancedDiagnosticsDialog;

@SuppressWarnings("serial")
public class ConsoleGUI extends Console {
    private JTextArea outputArea;
    private JTextField inputField;
    private JLabel infoBar;
    private JFrame frame;
    private JButton diagnosticsButton;
    private AdvancedDiagnosticsDialog diagnosticsDialog;

    public ConsoleGUI(Server server) {
    	// Call parent constructor with headless=false, but don't run initHeadless
    	super(server, false);
    	initGUI();
    }
    
    public ConsoleGUI(Server server, boolean headless) {
    	// Call parent constructor with headless=false, but don't run initHeadless
    	super(server, false);
    	initGUI();
    }
    
    private void initGUI() {
        frame = new JFrame("Proto Nova Server Console");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLocationRelativeTo(null);

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

        diagnosticsButton = new JButton("Advanced Diagnostics");
        diagnosticsButton.setFocusable(false);
        diagnosticsButton.addActionListener(event -> {
            if (server.getDiagnostics() != null) {
                if (diagnosticsDialog == null || !diagnosticsDialog.isDisplayable()) {
                    diagnosticsDialog = new AdvancedDiagnosticsDialog(frame, server.getDiagnostics());
                }
                diagnosticsDialog.setVisible(true);
                diagnosticsDialog.toFront();
            }
        });

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(backgroundColor);
        topBar.add(infoBar, BorderLayout.CENTER);
        topBar.add(diagnosticsButton, BorderLayout.EAST);
        
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputField, BorderLayout.SOUTH);
        frame.add(topBar, BorderLayout.NORTH);

        
        try {
			frame.setIconImage(ImageIO.read(new File("assets/ui/front.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        // Add a window listener for custom behavior
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Perform cleanup if needed
                System.out.println("Saving data");
                if (serverSaver != null) {
                    System.out.println(serverSaver.save());
                }
                else {
                	System.err.println("Failed to save data due to no serverSaver");
                }
                System.exit(0);  // Exit the application
            }
        });
        
        frame.setVisible(true);
        
        printWelcomeMessage();
        startUpdateThread();
    }
    
    private class InputListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String input = inputField.getText().trim();
            inputField.setText("");
            outputArea.append("> " + input + "\n");

            if (!input.isEmpty()) {
            	processInput(input);
            }
        }
    }

    @Override
    public void print(String output) {
    	if (headless) {
    		System.out.println(output);
    	} else {
    		outputArea.append(output+"\n");
    	}
    }
    
    @Override
    protected void onUpdateBar(String infoText) {
    	if (infoBar != null) {
    		infoBar.setText(infoText);
    	}
    }
}
