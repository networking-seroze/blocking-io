package org.example;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientStressTest {
    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 9091;
        int totalConnections = 10000; // try higher until failure

        List<Socket> sockets = new ArrayList<>();

        for (int i = 0; i < totalConnections; i++) {
            try {
                Socket socket = new Socket(host, port);
                sockets.add(socket);
                if (i % 100 == 0) {
                    System.out.println("Opened " + i + " connections");
                }
            } catch (IOException e) {
                System.err.println("Failed at connection # " + i + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("All connections opened. Press Enter to close.");
        System.in.read();

        for (Socket socket : sockets) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
