package com.picow.model;

import java.io.IOException;
import java.net.InetAddress;

import com.picow.model.sensors.Imu;
import com.picow.network.TcpTransport;
import com.picow.network.UdpTransport;

public class RobotFactory {
    

    public static RobotModel CreateRobot(String serverIp, int tcpPort, int udpPort){
        Imu imu = new Imu();
        System.out.println("Conecting to " + serverIp);
        // Verify we can reach the server
        if (!isServerReachable(serverIp)) {
            System.err.println("Cannot reach server at " + serverIp);
            return null;
        }

        // Create network manager and main window
        TcpTransport tcp = null;
        UdpTransport udp = null;
        try {
            tcp = new TcpTransport(serverIp, tcpPort);
            udp = new UdpTransport(serverIp, udpPort);
            RobotModel robot = new RobotModel(imu, tcp, udp);
            return robot;
        }
        catch (Exception e)
        {
            try {
                if (tcp != null && tcp.isConnected())
                    tcp.disconnect();
                if (udp != null && udp.isConnected())
                    udp.disconnect();
            } catch (IOException ioe){
                ioe.printStackTrace();
            } finally {
                e.printStackTrace();
            }
        }
        return null;
    }

    
    private static boolean isServerReachable(String ipAddress) {
        final int CONNECTION_TIMEOUT = 5000;
        try {
            return InetAddress.getByName(ipAddress).isReachable(CONNECTION_TIMEOUT);
        } catch (IOException e) {
            System.err.println("Error checking server reachability: " + e.getMessage());
            return false;
        }
    }
}
