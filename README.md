# Mecanum Wheel Drivetrain
## Software needed
* Download Thonny from https://thonny.org/ and install.
* Search latest micropython firmware for Raspberry Pi Pico 2W at https://www.raspberrypi.com/documentation/microcontrollers/micropython.html#drag-and-drop-micropython, or directly download at https://downloads.raspberrypi.com/micropython/mp_firmware_unofficial_latest.uf2
* Install latest JDK and your favorite Java IDE
## Basic Hardware connections
* A set of Mecanum wheels with 4 TT motors
* 1 Raspberry Pi Pico 2W
* 2 L298N
* ~9v Lipo/NiMh battery or a battery box for 6 AA/AAA batteries (Do not use 9v battery)
* ~5v Lipo/NiMh battery or a battery box for 3 AA/AAA batteries
* 1 Breadboard
* Many jumper lines and connectors
## Optional Hardware
* 220 ohm resistors
* LEDs
* Push button
* Switches

# How to Use
## Build the robot
* Assemble the frame, wheel and motors.
* Connect the Pico 2W, L298N and motors following the diagram of the svg files in [docs/](picow/docs/).
* Remember to test each component before assembly.
## Flash the firmware and upload main.py
* Edit the [main.py](picow/micropython/main.py) in Thonny or your favorite python editor, adjust the WiFi SSID and password.
* Adjust the pin definitions in main.py
* Press and hold the white button on the Pico 2W board while connecting it to your computer with a micro-USB cable.
* Your Pico will appear as a USB drive. Copy the firmware and main.py to it.
* Restart your Pico, or run main.py directly from Thonny.
## Run Java control programs
* Use your IDE, open the [pom.xml](picow/java/pom.xml)
* Compile and run
* **Keyboard Control**: Use the combination of WASD for sliding, left/right arrows for rotations, and up/down for speed controls.
* **Xbox Controller** (only supported on Windows-x64 for now): 
  - Left stick: Translation (forward/backward + strafe left/right)
  - Right stick: Rotation  
  - Triggers: Speed control
  - See [GAMEPAD_README.md](GAMEPAD_README.md) for detailed setup and usage
* Install more sensors and program your own controllers!

# Main Idea
## Pico as the nerves
* Pico 2W runs 2 threads, one to handle TCP connections and the other for UDP via WiFi.
* TCP simulates the sensors telemetry data bus, which reliably can be pulled and provides sensor data.
* UDP simulates the fire-and-forget command bus, which is fast but not reliable.
## Java app as the brain
* Now supports multiple control methods:
  - [KeyboardController](picow/java/src/main/java/com/picow/controller/KeyboardController.java) - Full mecanum wheel movement support
  - [GamepadController](picow/java/src/main/java/com/picow/controller/GamepadController.java) - Xbox controller support with analog precision *(NEW!)*
* Write your own controller from the [base class](picow/java/src/main/java/com/picow/controller/ControllerBase.java), e.g. 
  - An autonomous driving controller reads IMU data and navigates the robot.
  - An anti-collision controller reads light sensor or sonar data to brake the robot at emergency.
* Set the priority of your controller in [MotorCommandBus.java](picow/java/src/main/java/com/picow/model/MotorCommandBus.java)
* Logs available for analysis.


