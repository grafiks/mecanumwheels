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
    private long motorPowerBits;
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
    private static final int SPEED_UP = KeyEvent.VK_UP;
    private static final int SPEED_DOWN = KeyEvent.VK_DOWN;
    private static final int MOTOR_0 = KeyEvent.VK_1;
    private static final int MOTOR_1 = KeyEvent.VK_2;
    private static final int MOTOR_2 = KeyEvent.VK_3;
    private static final int MOTOR_3 = KeyEvent.VK_4;
    private static final int STOP_ALL = KeyEvent.VK_0;

    public KeyboardListener(JFrame jFrame, MotorClient motorClient) {
        this.motorClient = motorClient;
        this.frame= setupFrame(jFrame);
        this.motorPowerBits = encodeMotorPowerBits(0, 0, 0, 0);
        this.speed = MOTOR_FULL_SPEED;
        this.frequency = 50; // Default to 50Hz
    }

    private JFrame setupFrame(JFrame jFrame) {
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
            motorClient.disconnect();
        } catch (IOException e) {
            System.err.println("Error stopping motors: " + e.getMessage());
        }
    }

    private void updateLoop() {
        while (running.get()) {
            try {
                int[] speeds = decodeMotorPowerBits(this.motorPowerBits);
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

        if (keyStates.getOrDefault(SPEED_UP, false)){
            speed += SPEED_INCREMENT;
            if (speed > MOTOR_FULL_SPEED)
                speed = MOTOR_FULL_SPEED;
        }

        if (keyStates.getOrDefault(SPEED_DOWN, false)){
            speed -= SPEED_INCREMENT;
            if (speed < 0)
                speed = 0;
        }

        double[] powers = new double[]{0, 0, 0, 0};
        if (keyStates.getOrDefault(STOP_ALL, false)){
            powers = new double[]{0, 0, 0, 0};
        }
        else{
            if (keyStates.getOrDefault(FORWARD, false)) {
                addPower(powers, new double[]{ 1, 1, 1, 1});
            }
            if (keyStates.getOrDefault(BACKWARD, false)) {
                addPower(powers,  new double[]{ -1, -1, -1, -1});
            }
            if (keyStates.getOrDefault(LEFT, false)) {
                addPower(powers,  new double[]{ -1, 1, 1, -1});
            }
            if  (keyStates.getOrDefault(RIGHT, false)) {
                addPower(powers,  new double[]{ 1, -1, -1, 1});
            }
            if (keyStates.getOrDefault(ROTATE_LEFT, false)) {
                
                addPower(powers,  new double[]{ -1, 1, -1, 1});
            }
            if (keyStates.getOrDefault(ROTATE_RIGHT, false)) {
                
                addPower(powers,  new double[]{ 1, -1, 1, -1});
            }
            if (keyStates.getOrDefault(MOTOR_0, false)) {
                addPower(powers,  new double[]{1, 0, 0, 0});
            }
            if (keyStates.getOrDefault(MOTOR_1, false)) {
                addPower(powers,  new double[]{0, 1, 0, 0});
            }
            if (keyStates.getOrDefault(MOTOR_2, false)) {
                addPower(powers,  new double[]{0, 0, 1, 0});
            }
            if (keyStates.getOrDefault(MOTOR_3, false)) {
                addPower(powers,  new double[]{0, 0, 0, 1});
            }

            int n = (int)Math.max(Math.abs(powers[0]),
                    Math.max(Math.abs(powers[1]),
                    Math.max(Math.abs(powers[2]), 
                    Math.abs(powers[3]))));

            evenPower(powers, n);
        }

        this.motorPowerBits = encodeMotorPowerBits(
            (int)(powers[0] * speed),
            (int)(powers[1] * speed),
            (int)(powers[2] * speed),
            (int)(powers[3] * speed));
    }
    
    private static void addPower(double[] powers1, double[] powers2 ){
        for (int i = 0; i < 4; ++i)
            powers1[i]+= powers2[i];
    }
    
    private static void evenPower(double[] powers1, int n){
        for (int i = 0; i < 4; ++i)
            powers1[i] /= n;
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