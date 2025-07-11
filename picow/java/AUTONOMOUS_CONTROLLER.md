# Autonomous Controller Design

## Overview
The Autonomous Controller enables the mecanum wheel robot to execute predefined movement sequences automatically while maintaining safety through manual override and emergency stop functionality.

## Architecture

### Controller Hierarchy & Priority System
The system uses a priority-based command bus where higher-priority controllers can override lower-priority ones:

```
Priority 40: GAMEPAD     (highest - gamepad input)
Priority 30: KEYBOARD    (manual keyboard control)  
Priority 20: ANTI_COLLISION (emergency safety)
Priority 10: AUTONOMOUS  (lowest - autonomous sequences)
```

### State Machine Design
The `KeyboardController` implements a state machine that manages interaction between manual and autonomous control:

```
┌─────────┐    key press    ┌─────────────┐
│  IDLE   │ ───────────────▶│ KEYPRESSED  │
│         │                 │             │
└─────────┘                 └─────────────┘
     ▲                              │
     │ transition                   │ key release
     │ to IDLE                      ▼
┌─────────────┐               ┌─────────────┐
│ KEYRELEASED │◀──────────────│ STOPPRESSED │
│             │               │    ('0')    │
└─────────────┘               └─────────────┘
```

**State Behaviors:**
- **IDLE**: No manual input, autonomous can run freely
- **KEYPRESSED**: Manual override active, sends movement commands  
- **STOPPRESSED**: Emergency stop ('0' key), permanently terminates autonomous
- **KEYRELEASED**: Transition state, sends stop command then returns to IDLE

### Autonomous Controller Implementation

#### Core Components
1. **SequenceStep**: Defines individual movement commands
   ```java
   SequenceStep(String action, int durationMs, double[] powers)
   ```

2. **Sequence Queue**: FIFO queue of steps to execute
3. **Execution State**: Tracks whether autonomous mode is active

#### Key Methods
- `startSequence(String sequenceName)`: Loads and begins sequence execution
- `stopSequence()`: Immediately halts autonomous mode (permanent until manually restarted)
- `isExecutingSequence()`: Returns current execution status

#### Demo Route Example
```java
// DEMO_ROUTE sequence:
1. FORWARD:    [100, 100, 100, 100] for 600ms
2. PAUSE:      [0, 0, 0, 0] for 2000ms  
3. TURN_LEFT:  [-50, 50, -50, 50] for 400ms
4. FORWARD:    [100, 100, 100, 100] for 1000ms
5. STOP:       [0, 0, 0, 0] (final)
```

## Safety Features

### Manual Override
- **Temporary Override**: During autonomous execution, pressing movement keys (W/A/S/D) temporarily overrides autonomous commands
- **Resumption**: Releasing manual keys allows autonomous sequence to continue from where it left off
- **No Interference**: Manual override doesn't disrupt the autonomous sequence timeline

### Emergency Stop
- **Trigger**: Press '0' key at any time
- **Effect**: Immediately calls `autonomousController.stopSequence()`
- **Permanence**: Autonomous mode remains disabled until manually restarted with 'R' key
- **Safety Priority**: Works regardless of current controller state

### Autonomous Start Protection
- **Idle Requirement**: 'R' key only starts autonomous when keyboard is in IDLE state
- **User Feedback**: Clear console messages when start conditions aren't met
- **Prevents Conflicts**: Ensures no manual input is active when autonomous begins

## Control Flow

### Normal Operation
1. User presses 'R' → `autonomousController.startSequence("DEMO_ROUTE")`
2. Sequence loads into queue, execution begins
3. Each step runs for specified duration
4. Commands sent at controller frequency (50Hz)
5. Sequence completes → automatic stop

### Manual Override Flow
1. During autonomous execution, user presses 'W'
2. KeyboardController transitions to KEYPRESSED state
3. Manual movement commands override autonomous commands
4. User releases 'W' → KeyboardController transitions to KEYRELEASED → IDLE
5. Autonomous controller resumes from current sequence position

### Emergency Stop Flow
1. User presses '0' at any time
2. KeyboardController calls `autonomousController.stopSequence()`
3. Autonomous sequence cleared and execution flag set to false
4. All subsequent autonomous commands are [0, 0, 0, 0] until restart

## Motor Command Flow

```
AutonomousController
       │
       ▼ setMotorPowers(powers, "Autonomous")
MotorCommandBus
       │
       ▼ getHighestPriorityCommand()
   [Selects highest priority command]
       │
       ▼
   RobotModel
       │
       ▼ JSON over UDP
   Robot Hardware
```

## Integration Points

### With KeyboardController
- Shared reference allows emergency stop functionality
- State machine prevents conflicts during startup
- Manual override implemented through priority system

### With MotorCommandBus
- Uses "Autonomous" controller name with priority 10
- Commands automatically prioritized against other controllers
- Graceful degradation when higher-priority controllers active

### With RobotModel
- Standard `setMotorPowers()` interface
- Same command format as manual controllers
- Integrated logging and telemetry

## Usage Examples

### Starting Autonomous Mode
```java
// In application code
AutonomousController autonomous = new AutonomousController(robot, 50);
autonomous.start(); // Start controller thread

// User presses 'R' key
autonomous.startSequence("DEMO_ROUTE");
```

### Emergency Stop
```java
// Triggered by '0' key press
autonomous.stopSequence();
// Autonomous mode now disabled until manual restart
```

### Custom Sequences
```java
// In AutonomousController.loadCustomSequence()
currentSequence.add(new SequenceStep("MOVE_FORWARD", 1000, new double[]{75, 75, 75, 75}));
currentSequence.add(new SequenceStep("SLIDE_RIGHT", 500, new double[]{50, -50, -50, 50}));
currentSequence.add(new SequenceStep("ROTATE_180", 800, new double[]{-100, 100, -100, 100}));
```

## Testing & Debugging

### Debug Output
The system provides extensive logging for troubleshooting:
- State transitions in KeyboardController
- Sequence start/stop events
- Individual step execution
- Command bus priority resolution

### Testing Approach
1. **Manual Control**: Verify W/A/S/D movement works correctly
2. **Autonomous Start**: Test 'R' key starts sequence only when idle
3. **Manual Override**: During autonomous, test movement keys temporarily override
4. **Emergency Stop**: Test '0' key permanently stops autonomous
5. **State Recovery**: Verify system returns to proper states after each operation

## Future Enhancements

### Sensor Integration
- Add IMU feedback for closed-loop control
- Implement position tracking and correction
- Distance-based rather than time-based movements

### Advanced Sequences
- Conditional branching based on sensor data
- Looping and repeating segments
- Dynamic sequence modification

### Safety Improvements
- Collision detection integration
- Automatic obstacle avoidance
- Geofencing and boundary detection

## Configuration

### Controller Frequencies
- **AutonomousController**: 50Hz (precise timing for sequences)
- **KeyboardController**: 20Hz (responsive manual input)
- **MotorCommandBus**: 100Hz (smooth command execution)

### Tunable Parameters
- Sequence step durations
- Motor power levels for each movement type
- Controller priorities in MotorCommandBus
- Override detection sensitivity

---

## Related Documentation
- [Main Project README](../../../README.md) - Overall system setup and usage
- [KeyboardController](src/main/java/com/picow/controller/KeyboardController.java) - Manual control implementation
- [MotorCommandBus](src/main/java/com/picow/model/MotorCommandBus.java) - Priority-based command system
- [ControllerBase](src/main/java/com/picow/controller/ControllerBase.java) - Base class for creating custom controllers

## Summary
The Autonomous Controller provides safe, automated movement capabilities for the mecanum wheel robot through:
- **Predefined sequences** with precise timing control
- **Manual override** without disrupting autonomous execution  
- **Emergency stop** functionality for immediate safety
- **Priority-based integration** with other control systems
- **State machine design** preventing control conflicts

This design enables complex autonomous behaviors while maintaining operator safety and control at all times.

## Known Issues

### Testing & Validation
- **Not tested on real robot**: The autonomous controller has been designed and implemented but not yet validated with actual hardware
- **Network latency effects**: Real-world WiFi delays may affect sequence timing accuracy
- **Motor response variations**: Actual motor behavior may differ from expected power-to-movement mappings

### Implementation Limitations
- **Fixed sequence timing**: Currently uses time-based steps rather than position/sensor feedback
- **No collision detection**: Autonomous sequences run blindly without obstacle awareness
- **Limited error recovery**: System doesn't handle unexpected hardware failures gracefully

### User Interface
- **Console-only feedback**: No visual indicators in GUI for autonomous state
- **Limited sequence variety**: Only DEMO_ROUTE implemented, needs more predefined sequences
- **No runtime sequence editing**: Cannot modify sequences without code changes

### Performance Considerations
- **High CPU usage**: Multiple controller threads running at high frequencies
- **Memory usage**: Sequence steps stored in memory without optimization
- **Network overhead**: Commands sent continuously even when unchanged

### Future Work Required
- **Hardware validation**: Test all features with physical robot
- **Sensor integration**: Add IMU and other sensors for closed-loop control
- **Sequence editor**: GUI for creating/editing autonomous sequences
- **Performance optimization**: Reduce CPU and network overhead
- **Error handling**: Improve robustness for real-world deployment
