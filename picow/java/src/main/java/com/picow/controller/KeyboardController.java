package com.picow.controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;

import com.picow.model.MotorCommandBus;
import com.picow.model.RobotModel;


public class KeyboardController extends ControllerBase {
    private static final int MOTOR_FULL_SPEED = 100;
    private static final int SPEED_INCREMENT = 10;

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

    private int speed;
    private final Map<Integer, Boolean> keyStates = new ConcurrentHashMap<>();
    private final JFrame mainWindow;
    private long encodedPowers; // use an long integer to avoid a lock, at the cost of precisoin

    public KeyboardController(RobotModel robot, JFrame mainWindow, int frequency) {
        super(robot, "0", MotorCommandBus.KEYBOARD, frequency);
        this.mainWindow = mainWindow;
        this.speed = MOTOR_FULL_SPEED;
        this.encodedPowers = encodeMotorPowerBits(0, 0, 0, 0);
    }

    protected void setupListeners() {
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
                        speed = Math.max(speed - SPEED_INCREMENT, 0);
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

    private void updateMotors() {
        if (!running.get()) return;

        double[] powers = new double[]{0, 0, 0, 0};
        
        if (keyStates.getOrDefault(STOP_ALL, false)) {
            powers = new double[]{0, 0, 0, 0};
        } else {
            if (keyStates.getOrDefault(FORWARD, false)) {
                addPower(powers, new double[]{1, 1, 1, 1});
            }
            if (keyStates.getOrDefault(BACKWARD, false)) {
                addPower(powers, new double[]{-1, -1, -1, -1});
            }
            if (keyStates.getOrDefault(LEFT, false)) {
                addPower(powers, new double[]{-1, 1, 1, -1});
            }
            if (keyStates.getOrDefault(RIGHT, false)) {
                addPower(powers, new double[]{1, -1, -1, 1});
            }
            if (keyStates.getOrDefault(ROTATE_LEFT, false)) {
                addPower(powers, new double[]{-1, 1, -1, 1});
            }
            if (keyStates.getOrDefault(ROTATE_RIGHT, false)) {
                addPower(powers, new double[]{1, -1, 1, -1});
            }
            if (keyStates.getOrDefault(MOTOR_0, false)) {
                addPower(powers, new double[]{1, 0, 0, 0});
            }
            if (keyStates.getOrDefault(MOTOR_1, false)) {
                addPower(powers, new double[]{0, 1, 0, 0});
            }
            if (keyStates.getOrDefault(MOTOR_2, false)) {
                addPower(powers, new double[]{0, 0, 1, 0});
            }
            if (keyStates.getOrDefault(MOTOR_3, false)) {
                addPower(powers, new double[]{0, 0, 0, 1});
            }

            int n = (int)Math.max(Math.abs(powers[0]),
                    Math.max(Math.abs(powers[1]),
                    Math.max(Math.abs(powers[2]), 
                    Math.abs(powers[3]))));

            if (n > 0) {
                evenPower(powers, n);
            }
        }

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
        int[] powers =  decodeMotorPowerBits(encodedPowers);
        robot.setMotorPowers(new double[]{powers[0], powers[1], powers[2], powers[3]}, name);
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