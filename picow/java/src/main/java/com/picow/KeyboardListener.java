package com.picow;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;

public class KeyboardListener {
    private final MotorClient motorClient;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int speed;
    private final double[] motorSpeeds;
    private final int frequency;
    private final JFrame frame;
    private final Map<Integer, Boolean> keyStates = new ConcurrentHashMap<>();

    private static final int MOTOR_FULL_SPEED = 100; // Full speed
    private static final int SPEED_INCREMENT = 10;

    // Key mappings
    private static final int FORWARD = KeyEvent.VK_W;
    private static final int BACKWARD = KeyEvent.VK_S;
    private static final int LEFT = KeyEvent.VK_A;
    private static final int RIGHT = KeyEvent.VK_D;
    private static final int ROTATE_LEFT = KeyEvent.VK_LEFT;
    private static final int ROTATE_RIGHT = KeyEvent.VK_RIGHT;
    private static final int SPEED_UP = KeyEvent.VK_EQUALS;
    private static final int SPEED_DOWN = KeyEvent.VK_UNDERSCORE;
    private static final int MOTOR_0 = KeyEvent.VK_1;
    private static final int MOTOR_1 = KeyEvent.VK_2;
    private static final int MOTOR_2 = KeyEvent.VK_3;
    private static final int MOTOR_3 = KeyEvent.VK_4;
    private static final int STOP_ALL = KeyEvent.VK_0;

    public KeyboardListener(MotorClient motorClient) {
        this.motorClient = motorClient;
        this.frame = setupFrame();
        this.motorSpeeds = new double[]{0, 0, 0, 0};
        this.speed = MOTOR_FULL_SPEED;
        this.frequency = 50; // Default to 50Hz
    }

    private JFrame setupFrame() {
        JFrame jFrame = new JFrame("Motor Control");
        jFrame.setSize(640, 480);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLocationRelativeTo(null);
        
        jFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case STOP_ALL:
                        speed = 0;
                        break;
                    case SPEED_UP:
                        speed += SPEED_INCREMENT;
                        if (speed > MOTOR_FULL_SPEED) {
                            speed = MOTOR_FULL_SPEED;
                        } 
                        break;
                    case SPEED_DOWN:
                        speed -= SPEED_INCREMENT;
                        if (speed < 0) {
                            speed = 0;
                        }
                        break;
                    default:
                        break;
                }
                updateMotors();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                keyStates.put(e.getKeyCode(), true);
                updateMotors();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keyStates.put(e.getKeyCode(), false);
                updateMotors();
            }
        });

        jFrame.setFocusable(true);
        jFrame.setVisible(true);
        jFrame.requestFocusInWindow();

        return jFrame;
    }

    public void start() {
        if (running.get()) {
            return;
        }
        running.set(true);

        // Start motor update thread
        Thread updateThread = new Thread(this::updateLoop);
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public void stop() {
        running.set(false);
        frame.dispose();
        try {
            motorClient.setMotors(new int[] {0, 0, 0, 0});
        } catch (IOException e) {
            System.err.println("Error stopping motors: " + e.getMessage());
        }
    }

    private void updateLoop() {
        while (running.get()) {
            try {
                int[] speeds  = new int[4];
                for (int i = 0; i < 4; ++i)
                    speeds[i] = (int)(motorSpeeds[i] * 100);
                try {
                    motorClient.setMotors(speeds);
                } catch (IOException e) {
                    System.err.println("Failed to update motors: " + e.getMessage());
                    throw e;  // Propagate the error to stop the listener
                }
    
                Thread.sleep(1000/frequency); // 50Hz update rate
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
                stop();  // Stop the listener when connection is lost
                break;
            } catch (Exception e) {
                System.err.println("Error in keyboard update loop: " + e.getMessage());
                // Don't break for other errors, just log them
            }
        }
    }

    private void updateMotors(){
        if (!running.get()) {
            return;
        }

        double[] powers = null;
        if (keyStates.getOrDefault(STOP_ALL, false)){
            powers = new double[]{0,0,0,0};
        }
        if (keyStates.getOrDefault(FORWARD, false)) {
            powers = new double[]{ 1, 1, 1, 1};
        }
        if (keyStates.getOrDefault(BACKWARD, false)) {
            powers = new double[]{ -1, -1, -1, -1};
        }
        if (keyStates.getOrDefault(LEFT, false)) {
            powers = new double[]{ -1, 1, 1, -1};
        }
        if  (keyStates.getOrDefault(RIGHT, false)) {
            
            powers = new double[]{ 1, -1, -1, 1};
        }
        if (keyStates.getOrDefault(ROTATE_LEFT, false)) {
            
            powers = new double[]{ -1, 1, -1, 1};
        }
        if (keyStates.getOrDefault(ROTATE_RIGHT, false)) {
            
            powers = new double[]{ 1, -1, 1, -1};
        }
        if (keyStates.getOrDefault(MOTOR_0, false)) {
            powers = new double[]{1, 0, 0, 0};
        }
        if (keyStates.getOrDefault(MOTOR_1, false)) {
            powers = new double[]{0, 1, 0, 0};
        }
        if (keyStates.getOrDefault(MOTOR_2, false)) {
            powers = new double[]{0, 0, 1, 0};
        }
        if (keyStates.getOrDefault(MOTOR_3, false)) {
            powers = new double[]{0, 0, 0, 1};
        }

        if (powers != null)
            for (int i = 0; i < 4; ++i)
                motorSpeeds[i] = powers[i];
    }
}