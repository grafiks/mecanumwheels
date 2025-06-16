# Mecanum Wheel Drivetrain
## Software needed
* Download Thonny from https://thonny.org/ and install.
* Search latest micropython firmware for Raspberry Pi Pico 2W at https://www.raspberrypi.com/documentation/microcontrollers/micropython.html#drag-and-drop-micropython, or directly download at https://downloads.raspberrypi.com/micropython/mp_firmware_unofficial_latest.uf2
* Install latest JDK and your favorite Java IDE
## Basic Hardware connections
* A set of Mecanum wheels with 4 tt motors
* 1 Raspberry Pi Pico 2 W
* 2 L298N
* ~9v Lipo/NiMh battery or a battery box for 6 AA/AAA batteries
* ~5v Lipo/NiMh battery or a battery box for 3 AA/AAA batteries
* 1 Breadboard
* Many jumper lines and connector
## Optional Hardware
* 220 ohm resistors
* LEDs
* Push button
* Switches

# How to Use
## Build the robot
* Assemble the frame, wheel and motors.
* Connect the Pico 2W, L298N and motors following the diagram of the svg files in [docs/](pcio/docs/).
* Rember to test each component before assembly.
## Brush the firmware and main.py
* Edit the [main.py](picow/micropython/main.py), adjust the WiFi SSID and password.
* Adjust the pin defintions in main.py
* Press and hold the white button on Pico 2 W board until you connect it to your computer with a mini-USB cabel.
* Now your Pico board becomes a thumbdrive. Copy the firmware and main.py from to your Pico.
* Restart you Pico or run main.py from Thonny.
## Run Java controlling programs
* Use your IDE, open the [pom.xml](picow/java/pom.xml)
* Compile and run
* Use the combination of WASD for sliding, left/right arrows for rotations, and up/down for speed controls.
* Install more sensors and program your own controllers!

# Main Idea
# Pico as the nerves
* Pico 2 W run 2 threads, one to handle TCP connections and the other for UDP via WiFi.
* TCP simulate the sensors' telemetry data bus, which reliably can be pulled and provides sensor data.
* UDP simulate the fire-and-forget command bus, which is fast but not reliable.
# Java app as the brain
* Now the [KeyboardController](picow/java/src/main/java/com/picow/controller/KeyboardController.java) can do all the moves described at https://en.wikipedia.org/wiki/Mecanum_wheel.
* Write your own controller from the [base class](picow/java/src/main/java/com/picow/controller/ControllerBase.java) c to do more add logics, e.g. 
** A game pad controller
** An autonomous driving controller reads IMU data and navigates the robot.
** a anti-collision controller reads light sensor or sonar data to break the robot at emergency.
* Set the priority of your controller at MotorComamndBus.java
* Logs avaiable for analysis.


