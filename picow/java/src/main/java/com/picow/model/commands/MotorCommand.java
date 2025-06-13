package com.picow.model.commands;

public class MotorCommand extends Command {
    public int[] pwm;

    public MotorCommand(int[] pwm, long timestamp) {
        super("motor", timestamp);
        this.pwm = pwm;
    }
}
