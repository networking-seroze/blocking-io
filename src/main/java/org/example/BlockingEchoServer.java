package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket; // java net socket

public class BlockingEchoServer {

    public static void main(String[] args) throws IOException, RuntimeException {
        String host = "localhost";
        int port = 9091;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Echo Server started, listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Blocking until a client connects
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                handleClient(clientSocket);

                // In Multi-threaded EchoServer we just create new thread for each client
                /**
                 * Each thread uses 1MB of stack can be configurable via -Xss
                 * if there is 1000 clients then this can mean heavy context switching
                 * Thread handler = new Thread(() -> handleClient(clientSocket));
                 * handler.start();
                 *
                 */

//                Thread handler = new Thread(() -> handleClient(clientSocket));
//                handler.start();
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            BufferedReader clientSocketReader = new BufferedReader(new InputStreamReader(in));
            PrintWriter writer = new PrintWriter(out, true);
        ) {
            String line;
            while ((line = clientSocketReader.readLine()) != null ) { // Blocking until a line arrives
                System.out.println("Received : "+ line);
                writer.println("Echo: " + line); // Send back the same message
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
                System.out.println("Client disconnected");
            }
        }
    }
}
