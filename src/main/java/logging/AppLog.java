package logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;

public final class AppLog {

  private static boolean initialized;

  private AppLog() {}

  public static synchronized void initialize(String component) {
    if (initialized) return;
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    try {
      Path directory = logDirectory(component);
      Files.createDirectories(directory);
      Path logFile = directory.resolve(component + "-" + LocalDate.now() + ".log");
      OutputStream file =
          Files.newOutputStream(
              logFile,
              StandardOpenOption.CREATE,
              StandardOpenOption.WRITE,
              StandardOpenOption.APPEND);
      Object lock = new Object();
      System.setOut(
          new PrintStream(
              new TeeOutputStream(originalOut, file, lock), true, StandardCharsets.UTF_8));
      System.setErr(
          new PrintStream(
              new TeeOutputStream(originalErr, file, lock), true, StandardCharsets.UTF_8));
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    synchronized (lock) {
                      try {
                        file.flush();
                        file.close();
                      } catch (IOException ignored) {
                      }
                    }
                  },
                  component + "-log-shutdown"));
      initialized = true;
      System.out.println(
          System.lineSeparator()
              + "["
              + OffsetDateTime.now()
              + "] "
              + component
              + " process started");
      System.out.println("Log file: " + logFile);
    } catch (Exception error) {
      originalErr.println("Unable to initialize file logging: " + error.getMessage());
    }
  }

  private static Path logDirectory(String component) {
    String configured = System.getProperty("protonova.logDir");
    if (configured != null && !configured.isBlank()) {
      return Path.of(configured).toAbsolutePath().normalize().resolve(component);
    }

    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String userHome = System.getProperty("user.home");
    if (os.contains("win")) {
      String appData = System.getenv("APPDATA");
      Path root =
          appData == null || appData.isBlank()
              ? Path.of(userHome, "AppData", "Roaming")
              : Path.of(appData);
      return root.resolve("Proto-Nova").resolve("logs").resolve(component);
    }
    if (os.contains("mac")) {
      return Path.of(userHome, "Library", "Logs", "Proto-Nova", component);
    }
    String stateHome = System.getenv("XDG_STATE_HOME");
    Path root =
        stateHome == null || stateHome.isBlank()
            ? Path.of(userHome, ".local", "state")
            : Path.of(stateHome);
    return root.resolve("proto-nova").resolve("logs").resolve(component);
  }

  private static final class TeeOutputStream extends OutputStream {

    private final OutputStream console;
    private final OutputStream file;
    private final Object lock;

    private TeeOutputStream(OutputStream console, OutputStream file, Object lock) {
      this.console = console;
      this.file = file;
      this.lock = lock;
    }

    @Override
    public void write(int value) throws IOException {
      synchronized (lock) {
        console.write(value);
        file.write(value);
      }
    }

    @Override
    public void write(byte[] values, int offset, int length) throws IOException {
      synchronized (lock) {
        console.write(values, offset, length);
        file.write(values, offset, length);
      }
    }

    @Override
    public void flush() throws IOException {
      synchronized (lock) {
        console.flush();
        file.flush();
      }
    }
  }
}

