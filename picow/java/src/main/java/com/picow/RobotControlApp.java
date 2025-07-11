package com.picow;

import com.picow.controller.AutonomousController;
import com.picow.controller.KeyboardController;
import com.picow.model.RobotFactory;
import com.picow.model.RobotModel;
import com.picow.ui.MainWindow;

public class RobotControlApp {
    private static final int TCP_PORT = 8080;
    private static final int UDP_PORT = 8081;
    private static final String SERVER_IP = "192.168.4.1"; // Default IP for Pico W in Access Point mode
    //private static final String SERVER_IP = "192.168.1.66";  // Change this to your server's IP address in your wifi network

    public static void main(String[] args) {
        try {

            // Create a view - logging is initialized in MainWindow constructor
            MainWindow mainWindow = new MainWindow();
            
            // Create a model
            RobotModel robot = RobotFactory.CreateRobot(SERVER_IP, TCP_PORT, UDP_PORT);
            if (robot == null) {
                System.err.println("Failed to initialize robot");
                System.exit(-1);
            }

            // Create controllers
            AutonomousController autonomousController = new AutonomousController(robot, 50); // 50Hz for precise timing
            KeyboardController keyboardController = new KeyboardController(robot, mainWindow, 20, autonomousController);
            
            // Set up window closing handler
            mainWindow.setFocusable(true);  // Ensure window can receive focus
            mainWindow.requestFocusInWindow();  // Request focus
            // Show window
            mainWindow.setVisible(true);
               
            mainWindow.setWindowClosingHandler(e -> {
                System.out.println("Shutting down controllers...");
                keyboardController.stop();
                autonomousController.stop();
                try {
                    robot.stop();
                } catch (Exception ex) {
                    System.err.println("Error disconnecting: " + ex.getMessage());
                }
                System.exit(0);
            });

            // Start robot and controllers
            System.out.println("Starting robot and controllers...");
            robot.start();
            keyboardController.start();
            autonomousController.start();
            
            System.out.println("System ready! Press 'R' to start autonomous sequence.");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
}