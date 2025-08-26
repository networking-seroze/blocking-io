package org.example;

import java.nio.ByteBuffer;

/***
 *
 *
 * Key methods:
 *
 * allocate(n) → create heap buffer
 * allocateDirect(n) → off-heap for faster I/O
 * put() → write data into buffer
 * get() → read data out of buffer
 * flip() → prepare buffer for reading after writing
 * clear() → prepare buffer for writing again (does not erase data)
 * rewind() → reset position for re-reading
 *
 *
 * A ByteBuffer internally has:
 *
 * position → index where next write/read will happen
 * limit → one past the last byte you can read/write
 * capacity → total storage size (fixed)
 *
 * Why flip() is “bread and butter” in networking
 * Networking I/O in NIO is almost always:
 *
 * channel.read(buffer)
 * → Fills buffer in write mode (position moves forward).
 * flip()
 * → Switch to read mode.
 * Process the data (parse, handle message, etc.).
 * clear() (or compact() if partially processed)
 * → Switch back to write mode to get more data from the channel.
 */
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

        // prepare the buffer for writing again, it doesn't erase the data
        buffer.clear();
        // buffer.rewind(); // resets the position for re-reading
        System.out.println("Position after clear: "+ buffer.position());

    }
}
