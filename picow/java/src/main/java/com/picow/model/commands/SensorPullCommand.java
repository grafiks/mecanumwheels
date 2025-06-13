package com.picow.model.commands;

public class SensorPullCommand extends Command{
    public int id;
    public SensorPullCommand(String type, int id, long timestamp) {
        super(type, timestamp);
        this.id = id;
    }
}
