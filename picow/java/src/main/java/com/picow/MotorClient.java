package com.picow;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class MotorClient implements AutoCloseable {
    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    public MotorClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.outputStream = null;
        this.inputStream = null;
    }

    public void connect() throws IOException {
        if (connected.get()) {
            return;
        }

        socket = new Socket(serverIp, serverPort);
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        connected.set(true);
        System.out.println("Connected to server at " + serverIp + ":" + serverPort);
    }

    public void setMotors(int[] motorSpeeds) throws IOException {
        if (!connected.get()) {
            throw new IOException("Not connected to server");
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{ \"actions\": [");
            for (int i = 0; i < motorSpeeds.length; i++) {
                String action = String.format("{ \"type\": \"motor\", \"id\": %d, \"speed\": %d }%s", 
                    i, motorSpeeds[i], i < motorSpeeds.length - 1 ? "," : "");
                sb.append(action);
            }
            sb.append("]}");
            String response =  WriteDataAndGetResponse(outputStream, inputStream, sb.toString());
            System.out.println("Received: " + response);
        } catch (IOException e) {
            connected.set(false);
            throw e;
        }
    }

    private static String WriteDataAndGetResponse(OutputStream outputStream, InputStream inputStream, String data) throws IOException
    {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        outputStream.write(dataBytes);
        outputStream.flush();
    
        // Read raw byte response
        byte[] buffer = new byte[1024];
        int length = inputStream.read(buffer);  // blocking read
        if (length == -1) {
            throw new IOException("Connection closed by server");
        }
        
        String response = new String(buffer, 0, length, StandardCharsets.UTF_8);
        return response;
    }

    public void disconnect() throws IOException {
        if (!connected.get()) {
            return;
        }
        try {
            String response = WriteDataAndGetResponse(outputStream, inputStream, "{\"command\":\"quit\"}\n");
            System.out.println("Received: " + response);
        } catch (IOException e) {
            connected.set(false);
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            if (connected.get()) {
                try {
                    setMotors(new int[]{0, 0, 0, 0});
                    disconnect();
                } catch (IOException e) {
                    // Ignore errors during cleanup
                }
                socket.close();
                connected.set(false);
                System.out.println("Disconnected from server");
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
} 