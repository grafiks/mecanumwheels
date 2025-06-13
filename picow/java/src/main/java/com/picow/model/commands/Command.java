package com.picow.model.commands;

public abstract class Command{
    public String type;
    public long timestamp;
    public Command(String type, long timestamp){
        this.type = type;
        this.timestamp = timestamp;
    }
}
