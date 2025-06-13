package com.picow.model;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.annotations.SerializedName;

public class Imu {
    
    public static class Data {
        @SerializedName(value="accel", alternate={"a", "accel_g"})
        private final double[] accel; // acceleration in g (x, y, z)
        @SerializedName(value="gyro", alternate={"g", "gyro_dps"})
        private final double[] gyro;  // angular velocity in degrees/s (x, y, z)
        @SerializedName(value="temp", alternate={"t", "temp_raw", "temp_c"})
        private final double temp;    // temperature in Celsius

        public Data(double[] accel, double[] gyro, double temp) {
            this.accel = accel.clone();
            this.gyro = gyro.clone(); 
            this.temp = temp;
        }

        public double[] getAccel() { return accel.clone(); }
        public double[] getGyro() { return gyro.clone(); }
        public double getTemp() { return temp; }
    }

    private final ReentrantLock lock = new ReentrantLock();
    private Data data;

    public Imu() {
        data = new Data(new double[]{0, 0, 0}, new double[]{0, 0, 0}, 0);
    }

    public Data read() {
        lock.lock();
        try {
            return data;
        } finally {
            lock.unlock();
        }
    }

    public void set(Data newData) {
        try {
            lock.lock();
            try {
                data = newData;
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            System.err.println("Error updating IMU data: " + e.getMessage());
        }
    }
}
