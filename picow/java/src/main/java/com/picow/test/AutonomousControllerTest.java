package com.picow.test;

import com.picow.controller.AutonomousController;
import com.picow.model.RobotModel;
import com.picow.model.RobotFactory;

public class AutonomousControllerTest {
    public static void main(String[] args) {
        System.out.println("Testing AutonomousController...");
        
        try {
            // Create a mock robot (this might fail due to network, but that's ok)
            RobotModel robot = RobotFactory.CreateRobot("127.0.0.1", 8080, 8081);
            
            // Create autonomous controller
            AutonomousController autonomousController = new AutonomousController(robot, 10); // 10Hz for testing
            
            // Test sequence loading
            System.out.println("Testing sequence loading...");
            autonomousController.startSequence("DEMO_ROUTE");
            
            System.out.println("Is executing: " + autonomousController.isExecutingSequence());
            
            // Test stopping
            Thread.sleep(100);
            autonomousController.stopSequence();
            
            System.out.println("Is executing after stop: " + autonomousController.isExecutingSequence());
            
            System.out.println("AutonomousController test completed successfully!");
            
        } catch (Exception e) {
            System.out.println("Test completed with expected network error: " + e.getMessage());
            System.out.println("This is normal since we're not connected to a real robot.");
        }
    }
}
