package dev.slppz.bytefunk.sample;

import dev.slppz.bytefunk.net.Server;
import dev.slppz.bytefunk.net.Session;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SampleServer extends Server {
    public static void main(String[] args) {
        new Thread(new SampleServer(6767, 100)).start();
    }

    public SampleServer(int port, int capacity) {
        super(port, capacity);
    }

    @Override
    public void onConnected(Session session) {
        super.onConnected(session);
        // Sample code, ensure super is called first
        System.out.println("new connection");
    }

    @Override
    public void onMessageReceived(Session session, ByteBuffer messageBuffer) {
        System.out.println("message (" + messageBuffer.capacity() + " bytes) received");

        messageBuffer.getShort(); // Skip the op-code

        // Parse the message from bytes (ASCII text)
        StringBuilder s = new StringBuilder();
        while(messageBuffer.hasRemaining()) {
            s.append((char) messageBuffer.get());
        }

        // Print the received message
        System.out.println("Incoming message: " + s);

        // Send back a message
        session.send("hello client\n".getBytes(StandardCharsets.US_ASCII));

        if(s.toString().equals("blah")) {
            shutdown();
        }
    }

    @Override
    public void onDisconnected(Session session) {
        System.out.println("end of connection");
        // Ensure super is called last
        super.onDisconnected(session);
    }
}
