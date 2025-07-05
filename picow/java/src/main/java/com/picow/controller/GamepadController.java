package com.picow.controller;

import net.java.games.input.*;
import com.picow.model.MotorCommandBus;
import com.picow.model.RobotModel;

public class GamepadController extends ControllerBase {
    private static final int MOTOR_FULL_SPEED = 100;
    private static final int MOTOR_LOW_SPEED = 40;
    private static final int SPEED_INCREMENT = 5;
    private static final float DEADZONE = 0.1f; // 10% deadzone for analog sticks
    
    private Controller gamepadController;
    private Component leftStickX, leftStickY, rightStickX, rightStickY;
    private Component leftTrigger, rightTrigger;
    private Component leftBumper, rightBumper;
    private Component startButton;
    
    private int speed = MOTOR_FULL_SPEED;
    private long encodedPowers; // use a long integer to avoid a lock, at the cost of precision
    private boolean isConnected = false;
    
    public GamepadController(RobotModel robot, int frequency) {
        super(robot, "0", MotorCommandBus.GAMEPAD, frequency);
        this.encodedPowers = encodeMotorPowerBits(0, 0, 0, 0);
    }
    
    @Override
    protected void init() {
        if (!initializeGamepad()) {
            System.err.println("Failed to initialize gamepad. Controller will not be active.");
            return;
        }
        System.out.println("Gamepad initialized successfully: " + gamepadController.getName());
        isConnected = true;
    }
    
    private boolean initializeGamepad() {
        try {
            // Get all available controllers
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
            
            System.out.println("JInput found " + controllers.length + " input devices:");
            
            // Find the first gamepad/joystick controller
            for (Controller controller : controllers) {
                System.out.println("  - " + controller.getName() + " (" + controller.getType() + ")");
                
                if (controller.getType() == Controller.Type.GAMEPAD || 
                    controller.getType() == Controller.Type.STICK) {
                    
                    gamepadController = controller;
                    System.out.println("Selected controller: " + controller.getName() + " (" + controller.getType() + ")");
                    
                    // Map components
                    mapComponents();
                    return true;
                }
            }
            
            System.err.println("No gamepad found. Available controllers:");
            for (Controller controller : controllers) {
                System.err.println("  - " + controller.getName() + " (" + controller.getType() + ")");
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Error initializing JInput: " + e.getMessage());
            System.err.println("Common solutions:");
            System.err.println("  1. Ensure Xbox controller is connected and recognized by Windows");
            System.err.println("  2. Try running as administrator");
            System.err.println("  3. Check if controller appears in Windows Game Controllers");
            e.printStackTrace();
            return false;
        }
    }
    
    private void mapComponents() {
        Component[] components = gamepadController.getComponents();
        
        for (Component component : components) {
            String componentName = component.getIdentifier().getName();
            Component.Identifier identifier = component.getIdentifier();
            
            // Map analog sticks - try both string names and identifier types
            if (componentName.equals("x") || identifier == Component.Identifier.Axis.X) {
                leftStickX = component;
            } else if (componentName.equals("y") || identifier == Component.Identifier.Axis.Y) {
                leftStickY = component;
            } else if (componentName.equals("rx") || identifier == Component.Identifier.Axis.RX) {
                rightStickX = component;
            } else if (componentName.equals("ry") || identifier == Component.Identifier.Axis.RY) {
                rightStickY = component;
            }
            
            // Map triggers
            else if (componentName.equals("z") || identifier == Component.Identifier.Axis.Z) {
                leftTrigger = component;
            } else if (componentName.equals("rz") || identifier == Component.Identifier.Axis.RZ) {
                rightTrigger = component;
            }
            
            // Map bumpers (shoulders)
            else if (componentName.equals("4") || identifier == Component.Identifier.Button._4) {
                leftBumper = component;
            } else if (componentName.equals("5") || identifier == Component.Identifier.Button._5) {
                rightBumper = component;
            } else if (componentName.equals("7") || identifier == Component.Identifier.Button._7) {
                startButton = component;
            }
        }
        
        // Debug: Print mapped components
        System.out.println("Mapped components:");
        System.out.println("  Left stick X: " + (leftStickX != null ? "Found" : "Not found"));
        System.out.println("  Left stick Y: " + (leftStickY != null ? "Found" : "Not found"));
        System.out.println("  Right stick X: " + (rightStickX != null ? "Found" : "Not found"));
        System.out.println("  Right stick Y: " + (rightStickY != null ? "Found" : "Not found"));
        System.out.println("  Left trigger: " + (leftTrigger != null ? "Found" : "Not found"));
        System.out.println("  Right trigger: " + (rightTrigger != null ? "Found" : "Not found"));
        
        // Debug: Print all available components
        System.out.println("Available components:");
        for (Component component : components) {
            System.out.println("  - " + component.getIdentifier().getName() + " (" + component.getIdentifier() + ")");
        }
    }
    
    @Override
    protected void readSensors() {
        if (!isConnected || gamepadController == null) {
            return;
        }
        
        // Poll the controller for new input
        if (!gamepadController.poll()) {
            System.err.println("Controller disconnected");
            isConnected = false;
            return;
        }
        
        // Read analog inputs and update motor powers
        updateMotorPowers();
        
        // Read digital inputs for speed control and special functions
        updateSpeedAndSpecialFunctions();
    }
    
    private void updateMotorPowers() {
        // Get stick values
        float leftX = getComponentValue(leftStickX);
        float leftY = getComponentValue(leftStickY);
        float rightX = getComponentValue(rightStickX);
        
        // Apply deadzone
        leftX = applyDeadzone(leftX);
        leftY = applyDeadzone(leftY);
        rightX = applyDeadzone(rightX);
        
        // Calculate mecanum wheel powers
        // Left stick controls translation (forward/backward and strafe left/right)
        // Right stick X controls rotation
        double[] powers = calculateMecanumPowers(leftX, -leftY, rightX); // Invert Y for natural control
        
        // Scale by current speed
        int[] speeds = new int[]{
            (int)(powers[0] * speed),
            (int)(powers[1] * speed),
            (int)(powers[2] * speed),
            (int)(powers[3] * speed)
        };
        
        encodedPowers = encodeMotorPowerBits(speeds[0], speeds[1], speeds[2], speeds[3]);
    }
    
    private void updateSpeedAndSpecialFunctions() {
        // Speed control with triggers
        float leftTriggerValue = getComponentValue(leftTrigger);
        float rightTriggerValue = getComponentValue(rightTrigger);
        
        // Right trigger increases speed, left trigger decreases speed
        if (rightTriggerValue > 0.1f) {
            speed = Math.min(speed + (int)(rightTriggerValue * SPEED_INCREMENT), MOTOR_FULL_SPEED);
        }
        if (leftTriggerValue > 0.1f) {
            speed = Math.max(speed - (int)(leftTriggerValue * SPEED_INCREMENT), MOTOR_LOW_SPEED);
        }
        
        // Emergency stop with button combination (e.g., both bumpers)
        boolean leftBumperPressed = getComponentValue(leftBumper) > 0.5f;
        boolean rightBumperPressed = getComponentValue(rightBumper) > 0.5f;
        
        if (leftBumperPressed && rightBumperPressed) {
            // Emergency stop
            encodedPowers = encodeMotorPowerBits(0, 0, 0, 0);
            speed = 0;
        }
        
        // Reset speed with start button
        if (getComponentValue(startButton) > 0.5f) {
            speed = MOTOR_FULL_SPEED;
        }
    }
    
    private double[] calculateMecanumPowers(float strafe, float forward, float rotate) {
        // Mecanum wheel calculation
        // Motor arrangement (looking from top):
        // 0: Front-left  1: Front-right
        // 2: Back-left   3: Back-right
        
        double frontLeft = forward + strafe + rotate;
        double frontRight = forward - strafe - rotate;
        double backLeft = forward - strafe + rotate;
        double backRight = forward + strafe - rotate;
        
        // Normalize to prevent values > 1.0
        double maxPower = Math.max(Math.max(Math.abs(frontLeft), Math.abs(frontRight)),
                                  Math.max(Math.abs(backLeft), Math.abs(backRight)));
        
        if (maxPower > 1.0) {
            frontLeft /= maxPower;
            frontRight /= maxPower;
            backLeft /= maxPower;
            backRight /= maxPower;
        }
        
        return new double[]{frontLeft, frontRight, backLeft, backRight};
    }
    
    private float getComponentValue(Component component) {
        if (component == null) return 0.0f;
        return component.getPollData();
    }
    
    private float applyDeadzone(float value) {
        if (Math.abs(value) < DEADZONE) {
            return 0.0f;
        }
        // Scale the remaining range to 0-1
        return (Math.abs(value) - DEADZONE) / (1.0f - DEADZONE) * Math.signum(value);
    }
    
    @Override
    protected void takeActions() {
        if (!isConnected) {
            return;
        }
        
        int[] powers = decodeMotorPowerBits(encodedPowers);
        robot.setMotorPowers(new double[]{powers[0], powers[1], powers[2], powers[3]}, name);
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public String getControllerName() {
        return gamepadController != null ? gamepadController.getName() : "No controller";
    }
    
    // Utility methods (same as KeyboardController)
    private static long encodeMotorPowerBits(int p0, int p1, int p2, int p3) {
        return ((long)(p0 + 100) << 24) |
               ((long)(p1 + 100) << 16) |
               ((long)(p2 + 100) << 8)  |
               ((long)(p3 + 100));
    }
    
    private static int[] decodeMotorPowerBits(long power) {
        int p0 = (int)((power >> 24) & 0xFF) - 100;
        int p1 = (int)((power >> 16) & 0xFF) - 100;
        int p2 = (int)((power >> 8)  & 0xFF) - 100;
        int p3 = (int)(power & 0xFF) - 100;
        return new int[]{p0, p1, p2, p3};
    }
}
