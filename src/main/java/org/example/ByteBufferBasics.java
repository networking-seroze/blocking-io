package org.example;

import java.nio.ByteBuffer;

public class ByteBufferBasics {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(2);

        buffer.put((byte)65); // 'A'
        buffer.put((byte)66); // 'B'
        System.out.println("Position after puts: "+ buffer.position());

        // Switch to read mode
        buffer.flip();
        System.out.println("Limit in read mode:" + buffer.limit());

        // Read from buffer
        while (buffer.hasRemaining()) {
            System.out.println("Read byte: "+ (char) buffer.get());
        }

        // Clear for reuse
        buffer.clear();
        System.out.println("Position after clear: "+ buffer.position());

    }
}
