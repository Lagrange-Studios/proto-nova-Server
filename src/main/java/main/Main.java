package main;

public class Main {
    public static void main(String[] args) {
		boolean headless = java.awt.GraphicsEnvironment.isHeadless();
		for (String arg : args) {
			if (arg.equals("-headless") || arg.equals("--headless") || arg.equals("--nogui")) headless = true;
			else if (arg.equals("--gui")) headless = false;
			else if (arg.equals("--help") || arg.equals("-h")) {
				printHelp();
				return;
			} else if (arg.equals("--version")) {
				Package serverPackage = Main.class.getPackage();
				String version = serverPackage == null ? null : serverPackage.getImplementationVersion();
				System.out.println("Proto Nova Server " + (version == null ? "development" : version));
				return;
			} else if (arg.equals("--init-config") || arg.equals("--check-config")) {
				initializeOrCheckConfig(arg.equals("--init-config"));
				return;
			} else if (arg.equals("--healthcheck")) {
				runHealthCheck();
				return;
			} else if (!arg.isBlank()) {
				System.err.println("Unknown option: " + arg);
				printHelp();
				System.exit(2);
				return;
			}
		}
    	
    	Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
    		System.err.println("Uncaught exception in thread " + thread.getName());
    		throwable.printStackTrace();
    		System.exit(1);
    	});
    	
		new Server(headless);
    }

	private static void initializeOrCheckConfig(boolean initializeOnly) {
		try {
			if (!initializeOnly) System.setProperty("protonova.strictConfig", "true");
			ServerConfig.initialize(new Console(null, false));
			System.out.println((initializeOnly ? "Server configuration is ready: " : "Server configuration is valid: ")
					+ ServerConfig.getConfigPath().toAbsolutePath());
			System.out.println("Base game port: " + ServerConfig.getInstance().getGameSocketPort()
					+ "; companion HTTPS/client port: " + ServerConfig.getInstance().getStatusHttpPort());
		} catch (Exception failure) {
			System.err.println("Server configuration error: " + failure.getMessage());
			System.exit(1);
		}
	}

	private static void runHealthCheck() {
		try {
			ServerConfig.initialize(new Console(null, false));
			try (java.net.Socket socket = new java.net.Socket()) {
				socket.connect(new java.net.InetSocketAddress("127.0.0.1",
						ServerConfig.getInstance().getGameSocketPort()), 2_000);
			}
			System.out.println("healthy");
		} catch (Exception unavailable) {
			System.err.println("unhealthy: " + unavailable.getMessage());
			System.exit(1);
		}
	}

	private static void printHelp() {
		System.out.println("Proto Nova Server");
		System.out.println("Usage: Proto-Nova-Server [option]");
		System.out.println("  --headless, --nogui  Run with the terminal console (recommended for hosting)");
		System.out.println("  --gui                 Run the desktop server console");
		System.out.println("  --init-config         Create the default configuration without starting");
		System.out.println("  --check-config        Load and validate the configuration, then exit");
		System.out.println("  --healthcheck         Check whether the local game listener is reachable");
		System.out.println("  --version             Print the server version");
		System.out.println("  --help                Show this help");
	}
}
