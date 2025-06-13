package com.picow.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpTransport {
    private final String host;
    private final int port;
    private DatagramSocket socket;
    private InetAddress address;
    private final int maxRetries = 5;
    
    // UDP packet size limits
    public static int MAX_UDP_PACKET_SIZE = 1020; // Safe UDP packet size (1024 - 4 bytes for header)

    public UdpTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        if (isConnected()) {
            return;
        }

        socket = new DatagramSocket();
        socket.setSendBufferSize(MAX_UDP_PACKET_SIZE);
        address = InetAddress.getByName(host);
    }

    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    public void reconnect() throws IOException {
        disconnect();
        connect();
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void send(String data) throws IOException{
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        boolean connected = false;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (isConnected()) {
                connected = true;
                break;
            }
            if (attempt < maxRetries) {
                reconnect();
            }
        }

        if (!connected) {
            throw new IOException("UDP Not connected");
        }

        // Add newline and check size
        String message = data;
        byte[] sendData = message.getBytes();
        
        if (sendData.length > MAX_UDP_PACKET_SIZE) {
            throw new IOException("Data too large for UDP packet. Max size: " + MAX_UDP_PACKET_SIZE + 
                                " bytes, got: " + sendData.length + " bytes");
        }

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        socket.send(sendPacket);
    }
} 