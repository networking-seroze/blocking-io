package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/***
 * A Channel in NIO is like a modernized stream:
 *
 * Can support both read and write
 * Often supports non-blocking mode
 * Works with buffers
 * Main types relevant for networking:
 *
 * SocketChannel — client TCP
 * ServerSocketChannel — server TCP
 * DatagramChannel — UDP
 * (FileChannel for file I/O — useful to practice buffer operations)
 *
 */
public class FileChannelExample {
    public static void main(String[] args) throws IOException {
        // Writing
        try (FileChannel channel = FileChannel.open(
                Path.of("test.txt"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {

            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.put("Hello NIO".getBytes());
            buffer.flip(); // switch to read mode for channel
            channel.write(buffer);
        }

        // Reading
        try (FileChannel channel = FileChannel.open(
                Path.of("test.txt"),
                StandardOpenOption.READ)) {

            ByteBuffer buffer = ByteBuffer.allocate(16);
            int bytesRead = channel.read(buffer);
            System.out.println("Bytes read: " + bytesRead);

            buffer.flip();
            while (buffer.hasRemaining()) {
                System.out.print((char) buffer.get());
            }
        }
    }
}
