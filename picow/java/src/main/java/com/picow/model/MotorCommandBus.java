package com.picow.model;
import java.util.Map;
import java.util.concurrent.*;
import com.picow.model.commands.MotorCommand;

public class MotorCommandBus {
    public static final String ANTI_COLLISION = "AntiCollision";
    public static final String AUTONOMOUS = "Autonomous";
    public static final String GAMEPAD = "GamePad";
    public static final String KEYBOARD = "Keyboard";

    private final Map<String, MotorCommand> latestCommands = new ConcurrentHashMap<>();
    private static final Map<String, Integer> priorities = Map.of(
        GAMEPAD, 40,
        KEYBOARD, 30,
        ANTI_COLLISION, 20,
        AUTONOMOUS, 10
    );

    public MotorCommandBus(){

    }

    public void updateCommand(String source, MotorCommand command) {
        if (priorities.containsKey(source)) {
            latestCommands.put(source, command);
            
            // DEBUG: Show what command was added
            if (command != null) {
                System.out.println("DEBUG: MotorCommandBus.updateCommand() - Added command from " + source + 
                    " with powers [" + command.pwm[0] + ", " + command.pwm[1] + ", " + command.pwm[2] + ", " + command.pwm[3] + "]");
            } else {
                System.out.println("DEBUG: MotorCommandBus.updateCommand() - Cleared command from " + source);
            }
        }
    }
    
    public void clearCommand(String source, MotorCommand command) {
        updateCommand(source, null);
    }

    public MotorCommand getHighestPriorityCommand(long timestamp) {
        // DEBUG: Show all available commands
        System.out.println("DEBUG: MotorCommandBus.getHighestPriorityCommand() - Available commands:");
        for (Map.Entry<String, MotorCommand> entry : latestCommands.entrySet()) {
            if (entry.getValue() != null) {
                MotorCommand cmd = entry.getValue();
                System.out.println("  - " + entry.getKey() + " (priority " + priorities.get(entry.getKey()) + "): [" + 
                    cmd.pwm[0] + ", " + cmd.pwm[1] + ", " + cmd.pwm[2] + ", " + cmd.pwm[3] + "]");
            } else {
                System.out.println("  - " + entry.getKey() + " (priority " + priorities.get(entry.getKey()) + "): NULL");
            }
        }
        
        MotorCommand selectedCommand = latestCommands.entrySet().stream()
            .filter(entry -> entry.getValue() != null) // skip null values
            .sorted((a, b) -> priorities.get(b.getKey()) - priorities.get(a.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(new MotorCommand(new int[]{0, 0, 0, 0}, timestamp));
            
        // DEBUG: Show which command was selected
        System.out.println("DEBUG: MotorCommandBus selected command: [" + 
            selectedCommand.pwm[0] + ", " + selectedCommand.pwm[1] + ", " + selectedCommand.pwm[2] + ", " + selectedCommand.pwm[3] + "]");
            
        return selectedCommand;
    }
}
