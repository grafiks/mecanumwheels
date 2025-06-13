package com.picow.controller;
import com.picow.model.RobotModel;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ControllerBase implements Runnable {
    protected final String id;
    protected final String name;
    protected final RobotModel robot;
    protected final int frequency;
    protected final double interval;
    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected Thread controlThread;
    

    protected ControllerBase(RobotModel robot, String id, String name, int frequency) {
        this.id = id;
        this.name = name;
        this.robot  = robot;
        this.frequency = frequency;
        this.interval = 1.0/frequency;
        this.controlThread = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    protected abstract void readSensors();
    protected abstract void takeActions();
    protected abstract void init();

    public void start(){
        init();

        if (running.get())
            return;

        running.set(true);
        controlThread = new Thread(this, getClass().getName() + "-" + getName() + "-" +getId());
        controlThread.start();
    }

    @Override
    public void run()
    {
        long intervalMillis = (long)(this.interval * 1000);
        while (running.get()){
            long loopStart = System.currentTimeMillis();
            readSensors();

            // Step 2: Run control logic
            takeActions();

            // Step 3: Sleep to maintain frequency
            long loopDuration = System.currentTimeMillis() - loopStart;
            long sleepTime = intervalMillis - loopDuration;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                System.out.println("Warning: Loop overran by " + (-sleepTime) + " ms");
            }
        }
    }

    public void stop(){
        running.set(false);
        try {
            controlThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 