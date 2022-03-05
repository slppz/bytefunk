package dev.slppz.bytefunk.sample;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SampleClient {
    public static void main(String[] args) throws IOException {
        // Initialize the connection
        Socket socket = new Socket("127.0.0.1", 6767);

        // Acquire the output stream as a DataOutputStream for easy writing of data-types
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeShort(6); // Length of message

        // This number doesn't matter at the moment, just adding 2 bytes to align the parser
        out.writeShort(-1); // Would usually be an op-code (message type identifier)

        socket.getOutputStream().write("blah".getBytes(StandardCharsets.US_ASCII)); // Message content

        // Force it to send the whole thing
        socket.getOutputStream().flush();

        // Wait for a response string (including a terminating newline)
        System.out.println(new Scanner(socket.getInputStream()).nextLine());

        // Once received, close the socket and end the client program
        socket.close();
    }
}
