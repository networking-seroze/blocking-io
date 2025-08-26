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
