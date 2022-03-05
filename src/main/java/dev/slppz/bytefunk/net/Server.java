package dev.slppz.bytefunk.net;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Server class, with the core loop and vital actions pre-defined.
 */
public abstract class Server implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final int port;
    private final int capacity;
    private final ExecutorService sessionThreadPool;

    @Getter private final ArrayList<Session> sessions;

    /**
     * Instantiates a new Server using TCP.
     *
     * @param port     the server's TCP port
     * @param capacity the maximum amount of concurrent sessions
     */
    public Server(int port, int capacity) {
        this.port = port;
        this.capacity = capacity;
        sessions = new ArrayList<>(capacity);
        sessionThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void run() {
        logger.info("Server starting on port " + port);

        try {
            // Bind the socket to the port
            serverSocket = new ServerSocket(port);
            running = true;

            // Start listening for connections
            logger.info("Listening for connections");

            while(running) {
                try {
                    // Attempt to accept a connection; if none available, this blocks
                    Socket socket = serverSocket.accept();
                    String socketAddress = socket.getInetAddress().getHostAddress();

                    logger.debug("New connection from " + socketAddress);
                    // Only handle the connection if there is space
                    if(sessions.size() < capacity) {
                        // Schedule the session for further interaction, which will eventually invoke Session's run()
                        logger.debug("Accepted connection from " + socketAddress);

                        sessionThreadPool.submit(new Session(this, socket));
                    } else {
                        // Otherwise, close the connection
                        logger.warn("Denied connection from " + socketAddress + ": Server full");
                        socket.close();
                    }
                } catch (IOException ignored) {} // We don't care if any of these fail
            }
        } catch (IOException e) {
            // An error occurred when starting the server socket
            logger.fatal("Server crashed while starting: " + e.getMessage());
        }

        // The server has finished running; shutdown
        logger.info("Server shutting down");

        shutdown();
    }

    /**
     * Callback for when a Session connects to the server.
     * Call super(session), <b>then</b> run any custom code.
     *
     * @param session the Session
     */
    public void onConnected(Session session) {
        synchronized(sessions) {
            sessions.add(session);
        }
    }

    /**
     * Callback for when the server has received a message from a Session.
     *
     * @param session       the Session
     * @param messageBuffer the message buffer
     */
    public abstract void onMessageReceived(Session session, ByteBuffer messageBuffer);

    /**
     * Callback for when a Session disconnects from the server.
     * Run any custom code, <b>then</b> call super(session).
     *
     * @param session the Session
     */
    public void onDisconnected(Session session) {
        synchronized(sessions) {
            sessions.remove(session);
        }
    }

    /**
     * Shutdown the server
     */
    public void shutdown() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {} // At this point it doesn't matter; let it silently fail

        // Must also shut down the thread pool so that the program can finish executing
        sessionThreadPool.shutdown();
    }
}
