# My takeaways 

## Threading model 

### Server
- You can create ServerSocket by specifying port
- it will wait for client connection 
- it will handle each client connection in the same thread, so another client
  cannot connect while he is being served, this is not scalable because we need 
  one thread per client, you will be limited by some 2k native threads/process per user determined by ulimit -u 
  (at-least in java-17 because virtual threads start at java-21)

### Client 
- Client waits to connect to server, so you should start server beforehand
- We use another class Socket that will bind to target port on the host. 

## IO-details 
- socket objects return inputStream() and outputStream(), we should wrap
  them in InputStreamReader/OutputStreamReader and that needs to wrapped
  again with BufferedReader (for batching and limit io syscalls)
- we use PrintWriter to write to other process, but for Character like syntax like println
- if we don't close the sockets there will be a resource leak, Each socket consumes a file descriptor-OS's have
  hard limit (ulimit -n on unix, often a few thousand). You will eventually get 'java.net.SocketException: too many open files'
  - closing a socket will send FIN packet to the client, it will clear send,receive buffers at our end, releases file
  descriptor in the OS so other processes can use it, cancels any blocked IO operations, marks Socket object as closed
  so future invocations throw `SocketException`


### Understanding java i/o library 

1. Streams (java.io) - for reading/writing bytes or characters sequentially
   2. Byte-oriented: InputStream/ OutputStream and subclasses
   3. Examples: 
      4. FileInputStream, FileOutputStream 
      5. BufferedInputStream, BufferedOutputStream 
      6. DataInputStream, DataOutputStream 
7. Character-oriented: Reader/Writer and subclasses 
   8. Examples:
      9. FileReader, FileWriter 
      10. BufferedReader, BufferedWriter 
      11. PrintWriter 
12. Buffers/Wrappers - wrap streams to improve performance or add features 
    13. Buffering: BufferedReader, BufferedWriter, BufferedInputStream, BufferedOutputStream 
    14. Data conversion: InputStreamReader (bytes -> chars), OutputStreamWriter (chars -> bytes)
    15. Formatting/printing: PrintWriter 

    
    
### What happens when you try to 1000 connections sequentially
- In mac you will see failure at 51st connection because mac only allows 50 connections in the listen backloc queue
- On server side i only saw that one client is connected.
- I thought server only accepts a connection if i call ServerSocket.accept() but it's not true
  the OS stores the connection in the backlog till the application is ready to operate on them, but there's a limit
  to this backlog, the OS TCP stack on server side queued those incoming TCP SYN packets (a kernel level queue)
- Once server does `accpet()` it returns a socket and the tcp handshake is complete.
- In my testing with a multithreaded server, i was able to open around 4K connections on mac M2 with 16G ram

- Client connect() ---> TCP SYN ---> [Kernel accept backlog] ---> Java Thread accept()
  (not called yet in single-threaded server)


### Testing if I understand ByteBuffer these are some of sample questions 

Eg1: 
    ByteBuffer buf = ByteBuffer.allocate(5); // initially in write mode 
    buf.put((byte) 1);
    buf.put((byte) 2);
    System.out.println(buf.capacity()); // 5 
    System.out.println(buf.position()); // 2 
    System.out.println(buf.limit());    // 5

Eg2:
    buf.flip(); // now in read mode 
    System.out.println(buf.capacity()); // 10 
    System.out.println(buf.position()); // 0 starts from 0 because we just converted it to read mode 
    System.out.println(buf.limit());    // 2 since we can only read till i=2 

Eg3: 

    ByteBuffer b = ByteBuffer.allocate(4); 
    b.put((byte) 10); 
    b.put((byte) 20);
    b.get(); // without flip, what happens 

    What happens with get() here:
    
    By default, the ByteBuffer is in write mode after allocate().
    You wrote two bytes (10, 20), so position is now 2.
    When you call b.get() without flipping to read mode, it still operates in the current mode — so it reads from the current position (which is 2 right now), not from the start.
    Since you haven’t put anything at position 2 yet, it will read the default 0 that was in that byte slot.
    After get(), position becomes 3.

Eg4: 

    if we write two bytes and then flip and read 1 byte we will end up here 
    [ D E _ _ _ ]  // '_' means empty space
    pos=1, limit=2

    Question: If you call compact(), What does the buffer now look like, 
              and what are position and limit?

    When you call compact() on a ByteBuffer, it’s typically done in read mode to switch back to write mode while preserving any unread data.

    What compact() does:
        Copies any remaining unread bytes (from position() to limit()) to the start of the buffer (index 0 onward).
        Sets position to the number of bytes copied (so the next write appends after them).
        Sets limit to capacity (full buffer size, since you can now write freely).
        Old data before the new position is considered discarded.
    
    Example
    
    java
    
    Copy
    
    ByteBuffer b = ByteBuffer.allocate(5);
    b.put((byte) 1);
    b.put((byte) 2);
    b.put((byte) 3);
    
    b.flip(); // read mode: position=0, limit=3
    
    b.get(); // read one byte (1)
    // position=1, limit=3 notice that limit also gets rest so that we know till when to read 
    b.compact();

    Compact() will work in read mode or write mode, but it's only meaningful if you apply it in read mode 

    Why? 
        - The whole point of compact() is to keep unread data and make room for more writes without losing
          more data
        - In read mode (flip() was called), position() marks where you are in the unread section, and limit() 
          marks the end of valid data. compact() moves the bytes from position() to limit() to the start 
          of the buffer, resets pointers for writing.
        - If you’re in write mode already (limit = capacity), then “remaining data” is basically capacity 
          - position, and compact() just ends up shifting whatever’s past the current position 
          - which is usually unused space — so it has almost no practical effect.

    After compact():

    Copy
    
    Original unread data: [2][3]
    Copied to start:      [2][3][_][_][_]
    position = 2   // after the copied bytes
    limit    = 5   // can write up to capacity again 
    
    Quick Summary Table
    Before compact()	            After compact()
    position = first unread byte	position = number of remaining bytes
    limit = last readable index + 1	limit = capacity
    Mode = read	Mode = write


Eg5: 

    Networking read cycle 

    int bytesRead = channel.read(buf);
    buf.flip();
    process(buf);
    buf.clear();

    Question:
    
    Why do we flip() after reading from the channel and then clear() after processing? 
    What would happen if we skipped these steps?

    Answer: 

    when we pass the buff to channel it internally switches the buf to write mode and then gives it back 
    to us, now since we have to read, we need to flip back again and after processing the buf we need to 
    clear() it so it can be reused again 

    If you skip:
    
    - flip() → your read loop will think there’s nothing to process (remaining() == 0).
    - clear() → you’ll append new data after the old position instead of overwriting, leading to data corruption.

Eg6: 

    ByteBuffer bb = ByteBuffer.allocate(2);
    bb.putChar('A');

    limit = 2 
    position = 2 since it needs two bytes to represent A 


![Screenshot 2025-08-27 at 3.21.23 AM.png](../../../../../../var/folders/nx/p0_6rh9j1x3_bnq03snm7j7r0000gn/T/TemporaryItems/NSIRD_screencaptureui_PDlgzp/Screenshot%202025-08-27%20at%203.21.23%E2%80%AFAM.png)

Key mental shift : 


```

loop {
    read();
    process();
    write();
}

```

``` 
loop {
    selector.select();
    for (each ready key) {
        if (ready to read) read into buffer, switch to write mode;
        if (ready to write) write from buffer, switch to read mode;
    }
}

```