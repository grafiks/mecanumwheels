package com.picow;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class GamepadListener {
    private final MotorClient motorClient;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread listenerThread;
    private static final float DEADZONE = 0.1f; // Ignore small joystick movements
    private final int frequency;

    public GamepadListener(MotorClient motorClient) {
        this.motorClient = motorClient;
        this.frequency = 50; // Default to 50Hz
    }

    public void start() {
        if (running.get()) {
            return;
        }

        running.set(true);
        listenerThread = new Thread(this::run);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stop() {
        running.set(false);
        if (listenerThread != null) {
            try {
                listenerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void run() {
        try {
            // Initialize JInput
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
            Controller gamepad = null;

            // Find first gamepad
            for (Controller controller : controllers) {
                if (controller.getType() == Controller.Type.GAMEPAD || 
                    controller.getType() == Controller.Type.STICK) {
                    gamepad = controller;
                    break;
                }
            }

            if (gamepad == null) {
                System.err.println("No gamepad found!");
                return;
            }

            System.out.println("Found gamepad: " + gamepad.getName());

            // Main gamepad reading loop
            while (running.get()) {
                if (!gamepad.poll()) {
                    System.err.println("Failed to poll gamepad");
                    break;
                }

                // Get component values
                float leftX = getComponentValue(gamepad, Component.Identifier.Axis.X);
                float leftY = getComponentValue(gamepad, Component.Identifier.Axis.Y);
                float rightX = getComponentValue(gamepad, Component.Identifier.Axis.RX);
                float rightY = getComponentValue(gamepad, Component.Identifier.Axis.RY);

                // Apply deadzone
                leftX = applyDeadzone(leftX);
                leftY = applyDeadzone(leftY);
                rightX = applyDeadzone(rightX);
                rightY = applyDeadzone(rightY);

                // Convert to motor commands
                // Left stick controls motors 1 and 2
                // Right stick controls motors 3 and 4
                updateMotors(leftX, leftY, rightX, rightY);

                // Sleep to prevent too frequent updates
                Thread.sleep(1000 / frequency); // 50Hz update rate
            }

        } catch (Exception e) {
            System.err.println("Error in gamepad listener: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                motorClient.disconnect();;
            } catch (IOException e) {
                System.err.println("Error stopping motors: " + e.getMessage());
            }
        }
    }

    private float getComponentValue(Controller controller, Component.Identifier.Axis axis) {
        Component component = controller.getComponent(axis);
        return component != null ? component.getPollData() : 0.0f;
    }

    private float applyDeadzone(float value) {
        if (Math.abs(value) < DEADZONE) {
            return 0.0f;
        }
        // Normalize the value after deadzone
        return (value - Math.signum(value) * DEADZONE) / (1.0f - DEADZONE);
    }

    private void updateMotors(float leftX, float leftY, float rightX, float rightY) throws IOException {
    }
} 