import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class MultiClientNonBlockingEchoServer {
    // Per-client state
    static class ClientState {
        ByteBuffer readBuffer = ByteBuffer.allocate(256);
        StringBuilder messageBuilder = new StringBuilder();
        Queue<ByteBuffer> pendingWrites = new LinkedList<>();
    }

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(9090));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server listening on port 9090");

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (key.isValid() && key.isAcceptable()) {
                    handleAccept(selector, key);
                } else if (key.isValid() && key.isReadable()) {
                    handleRead(key);
                } else if (key.isValid() && key.isWritable()) {
                    handleWrite(key);
                }
            }
        }
    }

    private static void handleAccept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();
        clientChannel.configureBlocking(false);

        ClientState state = new ClientState();
        clientChannel.register(selector, SelectionKey.OP_READ, state);

        System.out.println("Accepted new client: " + clientChannel.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();

        int bytesRead = channel.read(state.readBuffer);
        if (bytesRead == -1) {
            System.out.println("Client disconnected: " + channel.getRemoteAddress());
            channel.close();
            return;
        }

        if (bytesRead > 0) {
            state.readBuffer.flip();
            while (state.readBuffer.hasRemaining()) {
                char c = (char) state.readBuffer.get();
                state.messageBuilder.append(c);
                if (c == '\n') {
                    String message = state.messageBuilder.toString().trim();
                    System.out.println("Received: " + message);

                    // Echo message back
                    byte[] response = (message + "\n").getBytes();
                    state.pendingWrites.add(ByteBuffer.wrap(response));
                    key.interestOps(SelectionKey.OP_WRITE); // switch to write-ready
                    state.messageBuilder.setLength(0); // reset for next message
                }
            }
            state.readBuffer.clear();
        }
    }

    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();

        while (!state.pendingWrites.isEmpty()) {
            ByteBuffer buf = state.pendingWrites.peek();
            channel.write(buf);
            if (buf.hasRemaining()) {
                // Haven't finished writing this buffer, try again later
                return;
            }
            state.pendingWrites.poll(); // done with this buffer
        }
        // All data sent — go back to reading mode
        key.interestOps(SelectionKey.OP_READ);
    }
}
