package main;

import javax.swing.SwingUtilities;

public class Server {
	
	public Console console;
	
	public Server() {
		
		// start console
		SwingUtilities.invokeLater(() -> {
            console = new Console();
            console.setVisible(true);
        });
	}
}
