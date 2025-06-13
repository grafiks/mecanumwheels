package com.picow.model.commands;

import com.google.gson.JsonElement;

public class SensorPullCommandJsonResponse extends SensorPullCommand {
    public JsonElement data;
    public SensorPullCommandJsonResponse(String type, int id, long timestamp, JsonElement data){
        super(type, id, timestamp);
        this.data = data;
    }
}
