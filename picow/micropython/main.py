import json
import socket
import struct
import time
import _thread # Async io not supported very well in micro python
import network
from machine import Pin, PWM, I2C

# WiFi credentials
WIFI_SSID = "irene-robot-wifi"
WIFI_PASSWORD = '12345678'
SHARED_WIFI_PASSWORD = ""
SHARED_WIFI_SSID = ""

# Server configuration
TCP_PORT = 8080
UDP_PORT = 8081
MAX_CONNECTIONS = 10

MODE_PIN = Pin(15, Pin.IN, Pin.PULL_DOWN)  # or PULL_UP, depending on your jumper
USE_AP_MODE = MODE_PIN.value() == 1  # HIGH = AP mode; LOW = STA mode

main_led=Pin("LED", Pin.OUT)
main_led.on()
tcp_error_led = Pin(16, Pin.OUT)
tcp_error_led.off()
udp_error_led = Pin(17, Pin.OUT)
udp_error_led.off()

def blink_led(led, interval=0.1, repeat=3):
    state = led.value()
    for i in range(repeat):
        led.value(1-state)
        time.sleep(interval)
        led.value(state)
        time.sleep(interval)
    
# IMU0: at I2C_SCL_PIN = 1, I2C_SDA_PIN = 0, address = 0x68
i2c = I2C(0, scl=Pin(1), sda=Pin(0), freq =400_000)

class IMU:
    def read_scaled(self):
        raise NotImplementedError("Subclasses must implement this method")

    def read_raw(self):
        raise NotImplementedError("Subclasses must implement this method")

class MPU6050(IMU):
    def __init__(self, i2c: I2C, address: int = 0x68):
        self.i2c = i2c
        self.address = address
        self.init_sensor()

    def init_sensor(self):
        # Wake up MPU6050 (register 0x6B = PWR_MGMT_1)
        self.i2c.writeto_mem(self.address, 0x6B, b'\x00')
        time.sleep(0.1)

    def read_raw(self):
        # Read 14 bytes starting from 0x3B
        raw = self.i2c.readfrom_mem(self.address, 0x3B, 14)
        vals = struct.unpack(">hhhhhhh", raw)
        ax, ay, az, temp_raw, gx, gy, gz = vals
        return {
            "accel": (ax, ay, az),
            "gyro": (gx, gy, gz),
            "temp_raw": temp_raw
        }

    def read_scaled(self):
        """
        16384.0	LSB per g for ±2g sensitivity
        131.0	LSB per °/s for ±250°/s sensitivity
        340.0	LSB/°C for temperature
        36.53	Offset for temp conversio
        """
        data = self.read_raw()
        ax, ay, az = [v / 16384.0 for v in data["accel"]]
        gx, gy, gz = [v / 131.0 for v in data["gyro"]]
        temp = data["temp_raw"] / 340.0 + 36.53
        return {
            "accel_g": (ax, ay, az),
            "gyro_dps": (gx, gy, gz),
            "temp_c": temp
        }

#imu0 = MPU6050(i2c, 0x68) # or 0x69, no other values for MPU6050. If AD0 Pin is GND, then 0x68, 0x69 else if AD0 Pin is VCC;

class Motor:
    def __init__(self, pwm_pin, in1_pin, in2_pin):
        self.in1 = Pin(in1_pin, Pin.OUT)
        self.in2 = Pin(in2_pin, Pin.OUT)
        self.pwm = PWM(Pin(pwm_pin))
        self.pwm.freq(1000)
        self.pwm.duty_u16(0)

    def set_power(self, power:int):
        def clamp(value):
            return max(-65535, min(value, 65535))
        power = clamp(power)
        duty = abs(power)
        
        if power > 0:
            self.in1.high()
            self.in2.low()
        elif power < 0:
            self.in1.low()
            self.in2.high()
        else:
            self.in1.low()
            self.in2.low()  # Brake or coast depending on driver

        self.pwm.duty_u16(duty)

    def stop(self):
        self.set_power(0)

# Setup for 4 motors
#  EN(PWM)	IN1	    IN2
#0	GP2	    GP3	    GP4
#1	GP6	    GP7	    GP8
#2	GP10	GP11	GP12
#3	GP18	GP19	GP20
motor0 = Motor(2, 3, 4)
motor1 = Motor(6, 7, 8)
motor2 = Motor(10, 11, 12)
motor3 = Motor(18, 19, 20)

class MotorController:
    def __init__(self, motors:list[Motor]):
        self.motors:list[Motor] = motors  # List of Motor instances
    
    def set_power(self, motor_id:int, power:int): 
        if 0 <= motor_id <= len(self.motors):
            self.motors[motor_id].set_power(power)
    
    def set_powers(self, powers):
        for idx, power in enumerate(powers):
            self.set_power(idx, power)

    def stop_all(self):
        for motor in self.motors:
            motor.stop()

def setup_wifi_ap():
    """Setup WiFi in AP mode"""
    ap = network.WLAN(network.AP_IF)
    ap.active(True)
    ap.config(essid=WIFI_SSID, password=WIFI_PASSWORD) # 0 for network.AUTH_OPEN, no password
    print(f'WiFi AP started with SSID: {WIFI_SSID}')
    while not ap.active():
        time.sleep(1)
    return ap

def connect_wifi():
    """Connect to WiFi network"""
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    
    if not wlan.isconnected():
        print('Connecting to WiFi...')
        wlan.connect(SHARED_WIFI_SSID, SHARED_WIFI_PASSWORD)
        
        # Wait for connection with timeout
        max_wait = 10
        while max_wait > 0:
            if wlan.isconnected():
                break
            max_wait -= 1
            print('Waiting for connection...')
            time.sleep(1)
        if wlan.isconnected():
            print('Connected to WiFi')
            print('Network config:', wlan.ifconfig())
            return wlan
        else:
            print('WiFi connection failed')
            return None
        
    return wlan


class TCPServer:
    def __init__(self, ip_address, port):
        self.ip_address = ip_address
        self.port = port
        self.socket = None
        self.client_socket = None
        self.client_address = None
        self.running = False

    def start(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.bind((self.ip_address, self.port))
        self.socket.listen(1)  # Only one client
        print(f'TCP server started on {self.ip_address}:{self.port}')
        self.running = True
        blink_led(main_led)

    def stop(self):
        self.running = False
        if self.client_socket:
            self.client_socket.close()
        if self.socket:
            self.socket.close()

    def handle_client(self, imu, pwm_motors):
        buffer = ""
        while self.running:
            try:
                # If no client, wait for one
                if not self.client_socket:
                    print("Waiting for TCP client...")
                    pwm_motors.stop_all()
                    self.client_socket, self.client_address = self.socket.accept()
                    print(f'TCP client connected from {self.client_address}')
                    buffer = ""  # Clear buffer for new client

                # Handle client
                data = self.client_socket.recv(1024)
                if not data:  # Client disconnected
                    print('TCP client disconnected')
                    self.client_socket.close()
                    self.client_socket = None
                    self.client_address = None
                    continue

                buffer += data.decode()
                while '\n' in buffer:
                    line, buffer = buffer.split('\n', 1)
                    if not line.strip():
                        continue

                    try:
                        command = json.loads(line)
                        if command.get('type') == 'imu':
                            imu_data = imu.read_scaled() if imu else {
                                "accel_g": (0, 0, 0),
                                "gyro_dps": (0, 0, 0),
                                "temp_c": 0
                            }
                            command['data'] = imu_data
                            command['error'] = None
                        else:
                            command['error'] = "Unknown command type"
                            command['data'] = None

                        self.client_socket.sendall((json.dumps(command) + '\n').encode())

                    except Exception as e:
                        error_response = {'type': 'error', 'data': str(e)}
                        self.client_socket.sendall((json.dumps(error_response) + '\n').encode())
                        blink_led(tcp_error_led)

            except Exception as e:
                print(f'TCP connection error: {e}')
                if self.client_socket:
                    self.client_socket.close()
                self.client_socket = None
                self.client_address = None
                blink_led(tcp_error_led)

class UDPServer:
    def __init__(self, ip_address, port):
        self.ip_address = ip_address
        self.port = port
        self.socket = None
        self.client_address = None
        self.running = False

    def start(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.bind((self.ip_address, self.port))
        print(f'UDP server started on {self.ip_address}:{self.port}')
        self.running = True
        blink_led(main_led)

    def stop(self):
        self.running = False
        if self.socket:
            self.socket.close()

    def handle_client(self, pwm_motors):
        while self.running:
            try:
                data, addr = self.socket.recvfrom(1024)
                if not data:
                    continue
                
                # Update client address if it changed
                if self.client_address != addr:
                    if self.client_address:
                        print(f'UDP client changed from {self.client_address} to {addr}')
                    else:
                        print(f'UDP client connected from {addr}')
                    self.client_address = addr

                try:
                    command = json.loads(data)
                    if command.get('type') == 'motor':
                        pwms = command.get('pwm')
                        pwm_motors.set_powers(pwms)
                except Exception as e:
                    print('Invalid UDP command:', e)
                    blink_led(udp_error_led)

            except Exception as e:
                print(f'UDP connection error: {e}')
                self.client_address = None
                blink_led(udp_error_led)

def start_servers(ip_address):
    """Start both TCP and UDP servers"""
    # Initialize controllers
    blink_led(main_led, 1)
    pwm_motors = MotorController([motor0, motor1, motor2, motor3])
    imu = None  # MPU6050()
    
    # Create servers
    tcp_server = TCPServer(ip_address, TCP_PORT)
    udp_server = UDPServer(ip_address, UDP_PORT)
    
    # Start servers
    tcp_server.start()
    udp_server.start()

    try:
        # Start UDP server in a separate thread
        _thread.start_new_thread(udp_server.handle_client, (pwm_motors,))
        # Run TCP server in main thread
        tcp_server.handle_client(imu, pwm_motors)
    except KeyboardInterrupt:
        print("Shutting down servers...")
    finally:
        tcp_server.stop()
        udp_server.stop()
        pwm_motors.stop_all()

def main(use_AP=True):
    if use_AP:
        ap = setup_wifi_ap()
        ip_address = ap.ifconfig()[0]
    else:
        wlan = connect_wifi()
        ip_address = wlan.ifconfig()[0]

    start_servers(ip_address)
    print('Servers stopped')

if __name__ == '__main__':
    main(USE_AP_MODE) 
