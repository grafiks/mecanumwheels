package com.picow;

import com.picow.controller.KeyboardController;
import com.picow.controller.GamepadController;
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
            KeyboardController keyboardController = new KeyboardController(robot, mainWindow, 20);
            GamepadController gamepadController = new GamepadController(robot, 20);
            
            // Set up window closing handler
            mainWindow.setFocusable(true);  // Ensure window can receive focus
            mainWindow.requestFocusInWindow();  // Request focus
            // Show window
            mainWindow.setVisible(true);
               
            mainWindow.setWindowClosingHandler(e -> {
                keyboardController.stop();
                gamepadController.stop();
                try {
                    robot.stop();
                } catch (Exception ex) {
                    System.err.println("Error disconnecting: " + ex.getMessage());
                }
                System.exit(0);
            });

            // Start robot and controllers
            robot.start();
            keyboardController.start();
            
            // Start gamepad controller (may not connect if no controller found)
            gamepadController.start();
            
            // Print gamepad status
            System.out.println("=== Controller Status ===");
            System.out.println("Keyboard controller: Active");
            System.out.println("Gamepad connected: " + gamepadController.isConnected());
            if (gamepadController.isConnected()) {
                System.out.println("Gamepad: " + gamepadController.getControllerName());
                System.out.println("Note: Gamepad has higher priority than keyboard");
            } else {
                System.out.println("Gamepad: Not connected - using keyboard only");
            }
            System.out.println("=========================");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
}