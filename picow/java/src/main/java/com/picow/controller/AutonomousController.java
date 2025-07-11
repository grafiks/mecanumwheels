package com.picow.controller;

import com.picow.model.MotorCommandBus;
import com.picow.model.RobotModel;
import java.util.*;

public class AutonomousController extends ControllerBase {
    private Queue<SequenceStep> currentSequence = new LinkedList<>();
    private SequenceStep currentStep = null;
    private long stepStartTime = 0;
    private boolean isExecuting = false;
    private long encodedPowers; // Same pattern as other controllers
    
    public AutonomousController(RobotModel robot, int frequency) {
        super(robot, "0", MotorCommandBus.AUTONOMOUS, frequency);
        this.encodedPowers = encodeMotorPowerBits(0, 0, 0, 0);
    }
    
    @Override
    protected void init() {
        System.out.println("AutonomousController initialized");
    }
    
    @Override
    protected void readSensors() {
        // No sensors needed for basic predefined sequences
        // Future enhancement: could read IMU for position tracking
    }
    
    @Override
    protected void takeActions() {
        if (!isExecuting) {
            // When not executing, send stop command
            encodedPowers = encodeMotorPowerBits(0, 0, 0, 0);
            int[] powers = decodeMotorPowerBits(encodedPowers);
            robot.setMotorPowers(new double[]{powers[0], powers[1], powers[2], powers[3]}, name);
            return;
        }
        
        executeCurrentSequence();
    }
    
    private void executeCurrentSequence() {
        long currentTime = System.currentTimeMillis();
        
        // Check if we need to start the next step
        if (currentStep == null && !currentSequence.isEmpty()) {
            currentStep = currentSequence.poll();
            stepStartTime = currentTime;
            System.out.println("Starting step: " + currentStep.action + " for " + currentStep.durationMs + "ms");
        }
        
        // If no current step and no more steps, sequence is complete
        if (currentStep == null) {
            System.out.println("Sequence completed");
            isExecuting = false;
            return;
        }
        
        // Check if current step is complete
        long elapsed = currentTime - stepStartTime;
        if (elapsed >= currentStep.durationMs) {
            System.out.println("Step '" + currentStep.action + "' completed after " + elapsed + "ms");
            currentStep = null; // Will get next step on next cycle
            return;
        }
        
        // Execute current step - convert powers to encoded format
        int[] speeds = new int[]{
            (int)(currentStep.powers[0]),
            (int)(currentStep.powers[1]),
            (int)(currentStep.powers[2]),
            (int)(currentStep.powers[3])
        };
        
        encodedPowers = encodeMotorPowerBits(speeds[0], speeds[1], speeds[2], speeds[3]);
        int[] powers = decodeMotorPowerBits(encodedPowers);
        
        robot.setMotorPowers(new double[]{powers[0], powers[1], powers[2], powers[3]}, name);
    }
    
    // Public method to start a predefined sequence
    public void startSequence(String sequenceName) {
        System.out.println("Starting sequence: " + sequenceName);
        
        // Clear any existing sequence
        currentSequence.clear();
        currentStep = null;
        
        // Load the requested sequence
        if ("DEMO_ROUTE".equals(sequenceName)) {
            loadDemoRoute();
        } else {
            System.err.println("Unknown sequence: " + sequenceName);
            return;
        }
        
        // Start execution
        isExecuting = true;
        stepStartTime = System.currentTimeMillis();
    }
    
    private void loadDemoRoute() {
        // Add sequence steps: FORWARD 600ms, PAUSE 1000ms, LEFT 1000ms, FORWARD 1000ms, STOP
        currentSequence.add(new SequenceStep("FORWARD", 600, new double[]{100, 100, 100, 100}));
        currentSequence.add(new SequenceStep("PAUSE", 1000, new double[]{0, 0, 0, 0}));
        currentSequence.add(new SequenceStep("LEFT", 1000, new double[]{-100, 100, 100, -100}));
        currentSequence.add(new SequenceStep("FORWARD", 1000, new double[]{100, 100, 100, 100}));
        currentSequence.add(new SequenceStep("STOP", 0, new double[]{0, 0, 0, 0}));
        
        System.out.println("Loaded DEMO_ROUTE with " + currentSequence.size() + " steps");
    }
    
    // Check if currently executing a sequence
    public boolean isExecutingSequence() {
        return isExecuting;
    }
    
    // Stop current sequence
    public void stopSequence() {
        System.out.println("Stopping current sequence");
        isExecuting = false;
        currentSequence.clear();
        currentStep = null;
    }
    
    // Inner class to represent a sequence step
    private static class SequenceStep {
        String action;
        int durationMs;
        double[] powers;
        
        SequenceStep(String action, int durationMs, double[] powers) {
            this.action = action;
            this.durationMs = durationMs;
            this.powers = powers.clone(); // Defensive copy
        }
    }
    
    // Utility methods (same as other controllers)
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
