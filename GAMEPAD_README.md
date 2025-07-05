# Gamepad Controller Implementation

## Overview
The GamepadController provides Xbox controller support for the mecanum wheel robot using the JInput library.

## Features
- **Left Stick**: Controls robot translation (forward/backward + strafe left/right)
- **Right Stick X**: Controls robot rotation (left/right)
- **Right Trigger**: Increases speed
- **Left Trigger**: Decreases speed
- **Start Button**: Resets speed to maximum (not yet tested)
- **Both Bumpers**: Emergency stop (when pressed together) (not yet tested)

## Mecanum Wheel Control Mapping
The controller implements proper mecanum wheel kinematics:
- Forward/Backward: All wheels move in same direction
- Strafe Left/Right: Wheels move in alternating pattern
- Rotation: Wheels move in diagonal pattern

## Motor Priority
The GamepadController has the highest priority (40) in the MotorCommandBus, meaning it will override keyboard and other control inputs when active.

## Setup
1. Connect Xbox controller to computer via USB or Bluetooth
2. Run the application - the controller will be automatically detected
3. Check console output for connection status

## Technical Details
- Uses JInput library for cross-platform gamepad support
- Runs at 20Hz control loop frequency
- Includes 10% deadzone for analog sticks to prevent drift
- Thread-safe implementation using atomic operations

## Troubleshooting
- If controller not detected, check that it's properly connected
- Ensure JInput native libraries are available for your platform
- Check console output for detailed component mapping information

## Known Issues / Future Improvements

### Cross-Platform Native Library Setup
**Issue**: Currently, only Windows has an automated setup script (`run_with_gamepad.ps1`). Mac and Linux users need to manually extract native libraries.

**Workaround for Mac/Linux users**:
1. Build the project: `mvn clean package -DskipTests`
2. Create a `natives` directory in the project root
3. Extract platform-specific libraries from the JAR:
   - **Linux**: Extract `.so` files to `natives/` folder
   - **macOS**: Extract `.dylib` files to `natives/` folder
4. Run with: `java -Djava.library.path=natives -jar target/motor-control-1.0-SNAPSHOT-jar-with-dependencies.jar`

**Future Enhancement**: Create equivalent setup scripts for Mac (`.sh`) and Linux (`.sh`) similar to the Windows PowerShell script.

### Keyboard/Gamepad Priority
**Issue**: When gamepad is connected, keyboard input is blocked due to priority system.

**Current Behavior**: Gamepad (priority 40) > Keyboard (priority 30)

**Future Enhancement**: Implement smart priority switching - only use gamepad priority when sticks are actively moved beyond deadzone, allowing keyboard control when gamepad is idle.
