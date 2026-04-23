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
    	
    	Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
    		System.err.println("Uncaught exception in thread " + thread.getName());
    		throwable.printStackTrace();
    		System.exit(1);
    	});
    	
    	Server server = new Server(headless);
    }
}