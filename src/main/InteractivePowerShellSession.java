package main;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Scanner;

public class InteractivePowerShellSession {
    public static void main(String[] args) {
        Process process;
        PrintWriter writer;
        Scanner scanner = new Scanner(System.in);

        try {
            // Start a basic PowerShell session
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass");
            builder.redirectErrorStream(true); // Combine stdout and stderr for simpler reading
            process = builder.start();

            // Setup the writer to send commands TO PowerShell (Java's Output Stream)
            writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);

            // Setup a separate thread to constantly READ FROM PowerShell (Java's Input Stream)
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[PS Output] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            System.out.println("PowerShell session started. Type commands below (type 'exit_java' to quit session):");
            
            // Loop in Java to send user input to the PowerShell process
            String command;
            while (true) {
                System.out.print("> ");
                command = scanner.nextLine();
                
                if ("exit_java".equalsIgnoreCase(command)) {
                    // Send the "exit" command to PowerShell to terminate it gracefully
                    writer.println("exit");
                    break;
                }
                
                // Send the command typed by the user to the PowerShell process
                writer.println(command);
            }

            // Clean up resources
            writer.close();
            scanner.close();
            executor.shutdown();
            
            // Wait for the process to fully terminate
            int exitCode = process.waitFor();
            System.out.println("PowerShell process exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}