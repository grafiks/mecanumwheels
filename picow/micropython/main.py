import network
import socket
import struct
import time
from machine import Pin, PWM, I2C
import json

# WiFi credentials
WIFI_SSID = "irene-robot-wifi"
WIFI_PASSWORD = '12345678'
SHARED_WIFI_PASSWORD = "your_wifi_password"
SHARED_WIFI_SSID = "your_wifi"

# Server configuration
SERVER_PORT = 8080
MAX_CONNECTIONS = 10

MODE_PIN = Pin(15, Pin.IN, Pin.PULL_DOWN)  # or PULL_UP, depending on your jumper
USE_AP_MODE = MODE_PIN.value() == 1  # HIGH = AP mode; LOW = STA mode

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

class IMUController:
    def __init__(self, imus: list[IMU]):
        self.imus = imus

    def read_imu_scaled(self, id: int):
        return self.imus[id].read_scaled()

    def read_imu_raw(self, id: int):
        return self.imus[id].read_raw()
    
    def read_imu(self, id: int, data_type: str):
        if data_type == "scaled":
            return self.read_imu_scaled(id)
        elif data_type == "raw":
            return self.read_imu_raw(id)
        else:
            raise ValueError("Invalid data type")

class Motor:
    def __init__(self, in1_pin, in2_pin, pwm_pin):
        self.in1 = Pin(in1_pin, Pin.OUT)
        self.in2 = Pin(in2_pin, Pin.OUT)
        self.pwm = PWM(Pin(pwm_pin))
        self.pwm.freq(1000)
        self.pwm.duty_u16(0)

    def set_speed(self, speed_percent):
        # speed_percent: -100 (full reverse) to +100 (full forward)
        speed = abs(speed_percent)
        duty = int((speed / 100.0) * 65535)
        
        if speed_percent > 0:
            self.in1.high()
            self.in2.low()
        elif speed_percent < 0:
            self.in1.low()
            self.in2.high()
        else:
            self.in1.low()
            self.in2.low()  # Brake or coast depending on driver

        self.pwm.duty_u16(duty)

    def stop(self):
        self.set_speed(0)

# Setup for 4 motors
# Motor 0: IN1 = GP2, IN2 = GP3, EN = GP16
# Motor 1: IN1 = GP4, IN2 = GP5, EN = GP17
# Motor 2: IN1 = GP6, IN2 = GP7, EN = GP18
# Motor 3: IN1 = GP8, IN2 = GP9, EN = GP19
motor0 = Motor(2, 3, 16)
motor1 = Motor(4, 5, 17)
motor2 = Motor(6, 7, 18)
motor3 = Motor(8, 9, 19)

class MotorController:
    def __init__(self, motors):
        self.motors = motors  # List of Motor instances
    
    def set_motor_speed(self, motor_id:int, speed_spercent:float): # -100.0 to 100.0
        if 0 <= motor_id <= len(self.motors):
            self.motors[motor_id].set_speed(speed_spercent)
    
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

def start_server(ip_address):
    """Start TCP server and handle client connections"""
    # Initialize both motor controllers
    pwm_motors = MotorController([motor0, motor1, motor2, motor3])
    imus = None # IMUController([imu0])
        
    # Create server socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    # Bind and listen
    server_socket.bind((ip_address, SERVER_PORT))
    server_socket.listen(MAX_CONNECTIONS)
    print(f'Server started on {ip_address}:{SERVER_PORT}')
    
    def clamp(value, min_value, max_value):
        return max(min_value, min(value, max_value))
    
    def handle_client(client_socket, client_address):
        print(f'Client connected from {client_address}')
        try:
            while True:
                data = client_socket.recv(1024)
                if not data:
                    break

                try:
                    data = data.decode()
                    print(data)
                    command = json.loads(data)
                except ValueError as e:
                    error_response = {'status': 'error', 'message': 'Invalid JSON: ' + str(e)}
                    client_socket.send(json.dumps(error_response).encode())
                    continue

                # Handle quit command
                if isinstance(command, dict) and command.get("command") == "quit":
                    print("Received quit command. Stopping motors and closing connection.")
                    pwm_motors.stop_all()
                    break

                response = {'status': 'succeeded', 'actions': [], 'sensors': []}

                try:
                    if 'actions' in command:
                        for action in command.get('actions', []):
                            if action.get('type') == 'motor':
                                motor_id = action.get('id')
                                speed = action.get('speed')
                                if motor_id in [0, 1, 2, 3]:
                                    speed = clamp(speed, -100, 100)
                                    pwm_motors.set_motor_speed(motor_id, speed)
                                    response['actions'].append(action)

                    if 'sensors' in command:
                        for sensor in command.get('sensors', []):
                            if sensor.get('type') == 'imu':
                                imu_id = sensor.get('id')
                                data_type = sensor.get('data_type')
                                try:
                                    imu_data = imus.read_imu(imu_id, data_type)
                                    sensor['data'] = imu_data
                                    response['sensors'].append(sensor)
                                except Exception as e:
                                    sensor['error'] = f"Failed to read IMU: {e}"
                                    response['sensors'].append(sensor)

                except Exception as e:
                    response = {'status': 'error', 'message': 'Command processing failed: ' + str(e)}

                try:
                    print(response)
                    client_socket.send(json.dumps(response).encode())
                except Exception as e:
                    print(f'Failed to send response: {e}')
                    break

        except Exception as e:
            print(f'Connection error with {client_address}: {e}')
        finally:
            print(f'Client {client_address} disconnected')
            client_socket.close()
            pwm_motors.stop_all()

    while True:
        try:
            client_socket, client_address = server_socket.accept()
            handle_client(client_socket, client_address)
        except Exception as e:
            print('Error accepting client connection:', e)
            pwm_motors.stop_all()
            time.sleep(1)

def main(use_AP=True):
    if use_AP:
    # Get IP address
        ap = setup_wifi_ap()
        ip_address = ap.ifconfig()[0]
    else:
        wlan = connect_wifi()
        ip_address = wlan.ifconfig()[0]
        
    start_server(ip_address)
    print('Start server at IP ' + ip_address)

if __name__ == '__main__':
    main(USE_AP_MODE) 

