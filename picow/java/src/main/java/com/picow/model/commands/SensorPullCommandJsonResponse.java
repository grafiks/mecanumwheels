package com.picow.model.commands;

import com.google.gson.JsonElement;

public class SensorPullCommandJsonResponse extends SensorPullCommand {
    public JsonElement data;
    public String error;
    public SensorPullCommandJsonResponse(String type, int id, long timestamp, String error, JsonElement data){
        super(type, id, timestamp);
        this.error = error;
        this.data = data;
    }
}
