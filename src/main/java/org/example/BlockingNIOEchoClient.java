package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class BlockingNIOEchoClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 9090;

        try (SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port))) {
            channel.configureBlocking(true);
            System.out.println("Connected to server");

            Scanner scanner = new Scanner(System.in);
            ByteBuffer buffer = ByteBuffer.allocate(256);

            while (true) {
                System.out.print("Enter message: ");
                String msg = scanner.nextLine();
                if ("quit".equalsIgnoreCase(msg)) break;

                // Send
                buffer.put(msg.getBytes());
                buffer.flip(); // switch to read mode for channel.write
                channel.write(buffer);
                buffer.clear(); // back to write mode for next message

                // Receive echo
                int bytesRead = channel.read(buffer); // write mode
                if (bytesRead == -1) break;

                buffer.flip(); // read mode
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                System.out.println("Server replied: " + new String(data));
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
