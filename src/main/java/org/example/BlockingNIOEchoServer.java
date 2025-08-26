package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class BlockingNIOEchoServer {
    public static void main(String[] args) {
        int port = 9090;
        System.out.println("Blocking NIO Echo Server started on port " + port);

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(true); // explicit, default anyway

            while (true) {
                SocketChannel clientChannel = serverChannel.accept(); // blocking
                System.out.println("Client connected: " + clientChannel.getRemoteAddress());

                handleClient(clientChannel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(SocketChannel clientChannel) {
        ByteBuffer buffer = ByteBuffer.allocate(5); // small for demo

        try (clientChannel) {
            while (true) {
                int bytesRead = clientChannel.read(buffer); // read into buffer (write mode)

                if (bytesRead == -1) { // client closed stream
                    System.out.println("Client disconnected");
                    break;
                }

                buffer.flip(); // switch to read mode

                // Echo back
                clientChannel.write(buffer); // write sends from buffer in read mode

                buffer.clear(); // back to write mode for next read
            }
        } catch (IOException e) {
            System.err.println("Client I/O error: " + e.getMessage());
        }
    }
}
