package com.picow.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpTransport {
    private final String host;
    private final int port;
    private int maxRetries = 5;
    private final int retryDelayMillis = 200;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final StringBuilder receiveBuffer = new StringBuilder();

    public TcpTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        if (isConnected()) {
            return;
        }

        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running.set(true);
    }

    public void disconnect() throws IOException {
        running.set(false);
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    public void reconnect() throws IOException {
        disconnect();
        connect();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized void send(String data) throws IOException, InterruptedException  {
        boolean connected = false;
        for (int attempt = 0; attempt < maxRetries; ++attempt){
            if (isConnected()) {
                connected = true;
                break;
            }

            if (attempt < maxRetries){
                reconnect();
                Thread.sleep(attempt * retryDelayMillis);
            }
        }

        if (!connected) {
            throw new IOException("TCP Not connected");
        }
        
        out.println(data);
    }

    public String read() throws IOException, InterruptedException {
        boolean connected = false;
        for (int attempt = 0; attempt < maxRetries; ++attempt){
            if (isConnected()) {
                connected = true;
                break;
            }

            if (attempt < maxRetries){
                reconnect();
                Thread.sleep(attempt * retryDelayMillis);
            }
        }

        if (!connected) {
            throw new IOException("TCP Not connected");
        }

        // Check if there's data available without blocking
        if (in == null || !in.ready()) {
            return null;
        }

        // Read available data
        char[] buffer = new char[1024];
        int bytesRead = in.read(buffer, 0, buffer.length);
        if (bytesRead == -1) {
            throw new IOException("Connection closed by server");
        }

        // Add to buffer and check for complete messages
        receiveBuffer.append(buffer, 0, bytesRead);
        int newlineIndex = receiveBuffer.indexOf("\n");
        if (newlineIndex == -1) {
            return null; // No complete message yet
        }

        // Extract and remove the complete message
        String message = receiveBuffer.substring(0, newlineIndex);
        receiveBuffer.delete(0, newlineIndex + 1);
        return message;
    }
} 