package com.picow;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

import javax.swing.JFrame;

public class MotorControl {
    private static final int SERVER_PORT = 8080;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final String SERVER_IP = "192.168.4.1"; // Default IP for Pico W in Access Point mode
    //private static final String SERVER_IP = "192.168.1.66";  // Change this to your server's IP address in your wifi network
    public static void main(String[] args) {

        try {
            // Verify we can reach the server
            if (!isServerReachable(SERVER_IP)) {
                System.err.println("Cannot reach server at " + SERVER_IP);
                return;
            }

            // Create motor client and input manager
            try (MotorClient motorClient = new MotorClient(SERVER_IP, SERVER_PORT)) {
                motorClient.connect();

                JFrame jFrame = new JFrame("Motor Control");
                jFrame.setSize(640, 480);
                jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                jFrame.setLocationRelativeTo(null);

                // Create and start input controller manager
                InputControllerManager inputManager = new InputControllerManager(jFrame, motorClient);
                inputManager.start();

                // Keep the main thread alive
                System.out.println("Press Enter to exit...");
                new Scanner(System.in).nextLine();
                
                // Cleanup
                inputManager.stop();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isServerReachable(String serverIp) {
        try {
            InetAddress address = InetAddress.getByName(serverIp);
            return address.isReachable(CONNECTION_TIMEOUT);
        } catch (IOException e) {
            System.err.println("Error checking server reachability: " + e.getMessage());
            return false;
        }
    }
} 