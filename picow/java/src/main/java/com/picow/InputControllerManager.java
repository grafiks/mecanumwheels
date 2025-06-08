package com.picow;

import java.io.IOException;
import javax.swing.JFrame;

public class InputControllerManager {
    private final MotorClient motorClient;
    private final KeyboardListener keyboardListener;
    private final GamepadListener gamepadListener;
    private boolean hasGamepad = false;

    public InputControllerManager(JFrame frame, MotorClient motorClient) {
        this.motorClient = motorClient;
        this.keyboardListener = new KeyboardListener(frame, motorClient);
        this.hasGamepad = false;
        this.gamepadListener = null;
     
        // Check if gamepad is available
        /* 
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller controller : controllers) {
            if (controller.getType() == Controller.Type.GAMEPAD || 
                controller.getType() == Controller.Type.STICK) {
                hasGamepad = true;
                break;
            }
        }
        
        this.gamepadListener = hasGamepad ? new GamepadListener(motorClient) : null;
        */
    }

    public void start() {

        // Always start keyboard listener
        keyboardListener.start();
        System.out.println("Keyboard controls enabled.");

        // Start gamepad listener if available
        if (hasGamepad && gamepadListener != null) {
            gamepadListener.start();
            System.out.println("Gamepad controls enabled");
        } else {
            System.out.println("No gamepad detected - using keyboard controls only");
        }
    }

    public void stop() {
        keyboardListener.stop();
        if (hasGamepad && gamepadListener != null) {
            gamepadListener.stop();
        }
        try {
            motorClient.disconnect();;
        } catch (IOException e) {
            System.err.println("Error stopping motors: " + e.getMessage());
        }
    }
} 