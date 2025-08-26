package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class BlockingEchoClient {

    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 9090;
        try (
                Socket socket = new Socket(host, port);
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {

            System.out.println("Connected to server. Type messages:");
            String userInput;

            while ((userInput = consoleReader.readLine()) != null ) {
                writer.println(userInput); // Send to server
                String response = socketReader.readLine(); // Blocking; wait for server reply
                System.out.println("Server replied: " + response);
            }
        }
    }
}
