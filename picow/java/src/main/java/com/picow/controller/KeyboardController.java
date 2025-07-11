package com.picow.controller;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import com.picow.model.MotorCommandBus;
import com.picow.model.RobotModel;


public class KeyboardController extends ControllerBase {
    
    // State machine for keyboard input
    private enum KeyboardState {
        IDLE,        // No keys pressed, no commands in bus
        KEYPRESSED,  // Movement keys are pressed
        STOPPRESSED, // Stop key (0) is pressed
        KEYRELEASED  // Keys were just released, need to send stop command
    }
    
    private static final int MOTOR_FULL_SPEED = 100;
    private static final int MOTOR_LOW_SPEED=40;
    private static final int SPEED_INCREMENT = 5;

    // Key mappings
    private static final int FORWARD = KeyEvent.VK_W;
    private static final int BACKWARD = KeyEvent.VK_S;
    private static final int LEFT = KeyEvent.VK_A;
    private static final int RIGHT = KeyEvent.VK_D;
    private static final int ROTATE_LEFT = KeyEvent.VK_LEFT;
    private static final int ROTATE_RIGHT = KeyEvent.VK_RIGHT;
    private static final int SPEED_UP = KeyEvent.VK_UP;
    private static final int SPEED_DOWN = KeyEvent.VK_DOWN;
    private static final int MOTOR_0 = KeyEvent.VK_1;
    private static final int MOTOR_1 = KeyEvent.VK_2;
    private static final int MOTOR_2 = KeyEvent.VK_3;
    private static final int MOTOR_3 = KeyEvent.VK_4;
    private static final int STOP_ALL = KeyEvent.VK_0;
    private static final int START_SEQUENCE = KeyEvent.VK_R; // 'R' for Route

    private int speed;
    private final Map<Integer, Boolean> keyStates = new ConcurrentHashMap<>();
    private final JFrame mainWindow;
    private final AutonomousController autonomousController; // Reference to autonomous controller
    private long encodedPowers; // use an long integer to avoid a lock, at the cost of precisoin
    private KeyboardState currentState = KeyboardState.IDLE; // Current state of the keyboard controller

    public KeyboardController(RobotModel robot, JFrame mainWindow, int frequency, AutonomousController autonomousController) {
        super(robot, "0", MotorCommandBus.KEYBOARD, frequency);
        this.mainWindow = mainWindow;
        this.autonomousController = autonomousController;
        this.speed = MOTOR_FULL_SPEED;
        this.encodedPowers = encodeMotorPowerBits(0, 0, 0, 0);
    }

    protected void setupListenersOld() {
        // Add key listener to main window
        mainWindow.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // Not used
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (!running.get()) return;
                
                int keyCode = e.getKeyCode();
                keyStates.put(keyCode, true);
                
                switch (keyCode) {
                    case SPEED_UP:
                        speed = Math.min(speed + SPEED_INCREMENT, MOTOR_FULL_SPEED);
                        break;
                    case SPEED_DOWN:
                        speed = Math.max(speed - SPEED_INCREMENT, MOTOR_LOW_SPEED);
                        break;
                    case STOP_ALL:
                        speed = 0;
                        break;
                }
                
                updateMotors();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!running.get()) return;
                
                keyStates.put(e.getKeyCode(), false);
                updateMotors();
            }
        });
    }

    protected void setupListeners() {
        JRootPane rootPane = mainWindow.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();
    
        // Bindings for key pressed
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed W"), FORWARD, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed S"), BACKWARD, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed A"), LEFT, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed D"), RIGHT, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed LEFT"), ROTATE_LEFT, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed RIGHT"), ROTATE_RIGHT, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed UP"), SPEED_UP, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed DOWN"), SPEED_DOWN, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed 0"), STOP_ALL, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed 1"), MOTOR_0, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed 2"), MOTOR_1, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed 3"), MOTOR_2, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed 4"), MOTOR_3, true);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("pressed R"), START_SEQUENCE, true);
    
        // Bindings for key released
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released W"), FORWARD, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released S"), BACKWARD, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released A"), LEFT, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released D"), RIGHT, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released LEFT"), ROTATE_LEFT, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released RIGHT"), ROTATE_RIGHT, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released 0"), STOP_ALL, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released 1"), MOTOR_0, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released 2"), MOTOR_1, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released 3"), MOTOR_2, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released 4"), MOTOR_3, false);
        bindKey(inputMap, actionMap, KeyStroke.getKeyStroke("released R"), START_SEQUENCE, false);
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, KeyStroke keyStroke, int keyCode, boolean isPressed) {
        String actionKey = (isPressed ? "pressed_" : "released_") + keyCode;
    
        inputMap.put(keyStroke, actionKey);
        actionMap.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!running.get()) return;
    
                keyStates.put(keyCode, isPressed);
    
                if (isPressed) {
                    switch (keyCode) {
                        case SPEED_UP:
                            speed = Math.min(speed + SPEED_INCREMENT, MOTOR_FULL_SPEED);
                            break;
                        case SPEED_DOWN:
                            speed = Math.max(speed - SPEED_INCREMENT, MOTOR_LOW_SPEED);
                            break;
                        case STOP_ALL:
                            speed = 0;
                            // EMERGENCY STOP: Permanently terminate autonomous mode
                            if (autonomousController != null) {
                                autonomousController.stopSequence();
                                System.out.println("KeyboardController: EMERGENCY STOP - autonomous sequence terminated");
                            }
                            break;
                        case START_SEQUENCE:
                            // Start the autonomous sequence only if keyboard is idle
                            if (currentState == KeyboardState.IDLE && autonomousController != null) {
                                System.out.println("KeyboardController: Starting autonomous sequence via 'R' key");
                                autonomousController.startSequence("DEMO_ROUTE");
                            } else if (currentState != KeyboardState.IDLE) {
                                System.out.println("KeyboardController: Cannot start autonomous - release all keys first (current state: " + currentState + ")");
                            }
                            break;
                    }
                }
    
                updateMotors();
            }
        });
    }
    
    
    private void updateMotors() {
        if (!running.get()) return;

        double[] powers = new double[]{0, 0, 0, 0};
        boolean anyKeyPressed = false;
        
        if (keyStates.getOrDefault(STOP_ALL, false)) {
            powers = new double[]{0, 0, 0, 0};
            anyKeyPressed = true; // STOP is an intentional command
        } else {
            if (keyStates.getOrDefault(FORWARD, false)) {
                addPower(powers, new double[]{1, 1, 1, 1});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(BACKWARD, false)) {
                addPower(powers, new double[]{-1, -1, -1, -1});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(LEFT, false)) {
                addPower(powers, new double[]{-1, 1, 1, -1});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(RIGHT, false)) {
                addPower(powers, new double[]{1, -1, -1, 1});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(ROTATE_LEFT, false)) {
                addPower(powers, new double[]{-1, 1, -1, 1});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(ROTATE_RIGHT, false)) {
                addPower(powers, new double[]{1, -1, 1, -1});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(MOTOR_0, false)) {
                addPower(powers, new double[]{1, 0, 0, 0});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(MOTOR_1, false)) {
                addPower(powers, new double[]{0, 1, 0, 0});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(MOTOR_2, false)) {
                addPower(powers, new double[]{0, 0, 1, 0});
                anyKeyPressed = true;
            }
            if (keyStates.getOrDefault(MOTOR_3, false)) {
                addPower(powers, new double[]{0, 0, 0, 1});
                anyKeyPressed = true;
            }

            int n = (int)Math.max(Math.abs(powers[0]),
                    Math.max(Math.abs(powers[1]),
                    Math.max(Math.abs(powers[2]), 
                    Math.abs(powers[3]))));

            if (n > 0) {
                evenPower(powers, n);
            }
        }

        // Update the state based on keyboard input
        if (keyStates.getOrDefault(STOP_ALL, false)) {
            currentState = KeyboardState.STOPPRESSED;
        } else if (anyKeyPressed) {
            currentState = KeyboardState.KEYPRESSED;
        } else if (currentState == KeyboardState.KEYPRESSED || currentState == KeyboardState.STOPPRESSED) {
            // Keys were just released
            currentState = KeyboardState.KEYRELEASED;
        }
        // If currentState is already IDLE or KEYRELEASED, it stays as is
        
        int[] speeds = new int[]{
            (int)(powers[0] * speed),
            (int)(powers[1] * speed),
            (int)(powers[2] * speed),
            (int)(powers[3] * speed)
        };

        encodedPowers = encodeMotorPowerBits(speeds[0], speeds[1], speeds[2], speeds[3]);
    }

    private static void addPower(double[] powers1, double[] powers2) {
        for (int i = 0; i < 4; ++i) {
            powers1[i] += powers2[i];
        }
    }

    private static void evenPower(double[] powers, int n) {
        for (int i = 0; i < 4; ++i) {
            powers[i] /= n;
        }
    }

    @Override
    protected void readSensors() {
    }

    @Override
    protected void takeActions() {
        switch (currentState) {
            case IDLE:
                // Send clear command to remove this controller from the command bus
                robot.clearMotorCommands(name);
                // Stay in IDLE state
                break;
                
            case KEYPRESSED:
                // Send the current movement command
                int[] powers = decodeMotorPowerBits(encodedPowers);
                robot.setMotorPowers(new double[]{powers[0], powers[1], powers[2], powers[3]}, name);
                // Stay in KEYPRESSED state (until updateMotors changes it)
                break;
                
            case STOPPRESSED:
                // Send stop command
                robot.setMotorPowers(new double[]{0, 0, 0, 0}, name);
                // Transition to IDLE
                currentState = KeyboardState.IDLE;
                break;
                
            case KEYRELEASED:
                // Send stop command to stop the robot immediately
                robot.setMotorPowers(new double[]{0, 0, 0, 0}, name);
                // Transition to IDLE
                currentState = KeyboardState.IDLE;
                break;
        }
    }

    @Override
    protected void init() {
        setupListeners();
    }
    
    private static long encodeMotorPowerBits(int p0, int p1, int p2, int p3){
        return ((long)(p0 + 100) << 24) |
               ((long)(p1 + 100) << 16) |
               ((long)(p2 + 100) << 8)  |
               ((long)(p3 + 100));
    }

    private static int[] decodeMotorPowerBits(long power){
        int p0 = (int)((power >> 24) & 0xFF) - 100;
        int p1 = (int)((power >> 16) & 0xFF) - 100;
        int p2 = (int)((power >> 8)  & 0xFF) - 100;
        int p3 = (int)(power & 0xFF) - 100;
        return new int[] { p0, p1, p2, p3 };
    }
} 