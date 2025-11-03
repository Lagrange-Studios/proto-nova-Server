package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalTime;

@SuppressWarnings("serial")
public class Console extends JFrame {
    private JTextArea outputArea;
    private JTextField inputField;

    public Console() {
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

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        printWelcomeMessage();
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
                outputArea.append(" - echo [text]: Repeat the text\n\n");
            } else if (input.equalsIgnoreCase("time")) {
                outputArea.append("Current time: " + LocalTime.now() + "\n\n");
            } else if (input.startsWith("echo ")) {
                outputArea.append(input.substring(5) + "\n\n");
            } else {
                outputArea.append("Unknown command. Type 'help' for options.\n\n");
            }
        }
    }

    public void print(String output) {
    	outputArea.append(output+"\n");
    }
}
