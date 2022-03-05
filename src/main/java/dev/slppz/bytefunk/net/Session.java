package dev.slppz.bytefunk.net;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * An active connection to the server.
 */
public class Session implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    @Getter private final Server server;
    @Getter private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private volatile boolean active = false;

    /**
     * Instantiates a new Session.
     *
     * @param server the server it is connected to
     * @param socket the connection's socket
     * @throws IOException if unable to get input and output streams of socket
     */
    public Session(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;

        // Attempt to obtain socket's input and output streams
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        // Execute the server callback function for a new connection
        server.onConnected(this);

        // Initiate the connection's message parsing loop
        active = true;
        while(active) {
            try {
                // Record the expected message length
                short messageByteLength = in.readShort();
                // Check to see if it is within sane size bounds; if not, terminate the connection
                if(messageByteLength < NetConstants.MIN_MSG_LENGTH || messageByteLength > NetConstants.MAX_MSG_LENGTH) break;
                // The message is within size bounds; attempt to read all of it
                ByteBuffer messageBuffer = ByteBuffer.wrap(in.readNBytes(messageByteLength));
                // Send the message to the server for further processing
                server.onMessageReceived(this, messageBuffer);
            } catch (IOException e) {
                // There was an error gathering the message; terminate the connection
                logger.warn("Error preparing next message: " + e.getMessage());
                break;
            }
        }

        // Session completed, ensure the socket is closed
        terminate(); // We don't care if this fails, here it's a formality

        // Execute the server callback function for a disconnection
        server.onDisconnected(this);
    }

    /**
     * Send the provided bytes to the Session.
     *
     * @param bytes provided bytes (Data)
     */
    public void send(byte[] bytes) {
        // Attempt to write the bytes to the output stream
        try {
            out.write(bytes);
        } catch (IOException e) {
            // If it fails, close the session
            terminate();
        }
    }

    /**
     * Terminate the session.
     */
    public void terminate() {
        // Turn off the looping condition
        active = false;
        // As well as attempting to close the socket
        try {
            socket.close();
        } catch (IOException ignored) {}
        // By this point, we hope that the session loop has been successfully broken
    }
}
