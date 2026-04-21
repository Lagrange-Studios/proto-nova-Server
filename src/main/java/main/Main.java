package main;

public class Main {
    public static void main(String[] args) {
    	boolean headless = false;
    	
    	for (String arg : args) {
    		if (arg.equals("-headless")) {
    			headless = true;
    			break;
    		}
    	}
    	
    	Server server = new Server(headless);
    }
}