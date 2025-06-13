package com.picow.model;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.picow.model.commands.MotorCommand;
import com.picow.model.commands.SensorPullCommand;
import com.picow.model.commands.SensorPullCommandJsonResponse;
import com.picow.network.TcpTransport;
import com.picow.network.UdpTransport;

public class RobotModel {
    // Sensors
    private final Imu imu;
    private final MotorCommandBus commandBus;
    private final int numberOfMotors = 4;
    
    // Network transports
    private final TcpTransport tcp;
    private final UdpTransport udp;
    
    // Thread management
    private final ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread sensorThread;
    private final Gson gson = new Gson();

    public RobotModel(Imu imu, TcpTransport tcpTransport, UdpTransport udpTransport) {
        this.imu = imu;
        this.commandBus = new MotorCommandBus();
        this.tcp = tcpTransport;
        this.udp = udpTransport;
        this.executor = Executors.newScheduledThreadPool(2);
    }

    public void start() {
        if (running.get()) return;
        running.set(true);
        
        // Start TCP polling at 20Hz
        executor.scheduleAtFixedRate(this::pollSensors, 0, 50, TimeUnit.MILLISECONDS);
        
        // Start UDP command sending at 100Hz
        executor.scheduleAtFixedRate(this::sendMotorCommands, 0, 10, TimeUnit.MILLISECONDS);
        
        // Start sensor data reading thread
        startSensorThread();
    }

    public void stop() {
        if (!running.get()) return;
        running.set(false);
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (sensorThread != null) {
            sensorThread.interrupt();
            try {
                sensorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try{
            tcp.disconnect();
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
        try{
            udp.disconnect();
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void pollSensors() {
        if (!running.get()) return;
        
        try {
            long ts = System.currentTimeMillis();
            SensorPullCommand command = new SensorPullCommand("imu", 0, ts);
            String cmd = gson.toJson(command);
            tcp.send(cmd);
            System.out.println(cmd);
        } catch (Exception e) {
            System.err.println("Error polling sensors: " + e.getMessage());
        }
    }

    private void sendMotorCommands() {
        if (!running.get()) return;
        
        try {
            long ts = System.currentTimeMillis();
            MotorCommand command = commandBus.getHighestPriorityCommand(ts);
            String cmd = gson.toJson(command);
            udp.send(cmd);
            System.out.println(cmd);
        } catch (Exception e) {
            System.err.println("Error sending motor commands: " + e.getMessage());
        }
    }

    private void startSensorThread() {
        sensorThread = new Thread(() -> {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    String data = tcp.read();
                    if (data != null) {
                        processSensorData(data);
                    }
                    Thread.sleep(1); // Small sleep to prevent busy waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error reading sensor data: " + e.getMessage());
                }
            }
        });
        sensorThread.setDaemon(true);
        sensorThread.start();
    }

    private void processSensorData(String data) {
        try {
            SensorPullCommandJsonResponse response = gson.fromJson(data, SensorPullCommandJsonResponse.class);
            if (response.type.equals("imu")) {
                // Process sensor data and update IMU
                imu.set(gson.fromJson(response.data, Imu.Data.class));
                String imuDataStr = gson.toJson(response.data);
                System.out.println(imuDataStr);
            }
        } catch (Exception e) {
            System.err.println("Error processing sensor data: " + e.getMessage());
        }
    }

    public Imu.Data getImuData() {
        return imu.read();
    }

    // powers range from -100 to 100
    public void setMotorPowers(double[] powers, String setter) {
        if (powers == null || powers.length != numberOfMotors) {
            throw new IllegalArgumentException("powers setting is null or mismatching motor numbers.");
        }

        // Clamp and copy all motor powers
        int[] pwm = new int[4];
        for (int i = 0; i < numberOfMotors; i++) {
            pwm[i] = Math.min(65535, Math.max(-65535, (int)(655.35 * powers[i])));
        }

        MotorCommand command = new MotorCommand(pwm, System.currentTimeMillis() / 1000);
        commandBus.updateCommand(setter, command);
    }
}
