import Adafruit_DHT
import time
import glob
import os
import pyrebase
from gpiozero import OutputDevice
from imutils.video import VideoStream
import imutils
import cv2
import datetime
import threading

class Relay(OutputDevice):
    def __init__(self, pin, active_high):
        super(Relay, self).__init__(pin, active_high)

DHT_SENSOR = Adafruit_DHT.DHT22
DHT_PIN = 22
humidity_check_interval = 1800
min_humidity = 50

base_dir = '/sys/bus/w1/devices/'
device_folder = glob.glob(base_dir + '28*')[0]
device_file = device_folder + '/w1_slave'

SECONDS_TO_WATER = 10
RELAY = Relay(18, False)

T = 50
min_area = 1000

video_path = None
if video_path is None:
    vs = VideoStream().start()
    time.sleep(2)
else:
    vs = cv2.VideoCapture(video_path)

os.system('modprobe w1-gpio')
os.system('modprobe w1-therm')

def water(relay, seconds):
    relay.on()
    print("water on")
    time.sleep(seconds)
    print("water off")
    relay.off()

def read_temp_raw():
    f = open(device_file, 'r')
    lines = f.readlines()
    f.close()
    return lines
def read_temp():
    lines = read_temp_raw()
    while lines[0].strip()[-3:] != 'YES':
        time.sleep(0.2)
        lines = read_temp_raw()
    equals_pos = lines[1].find('t=')
    if equals_pos != -1:
        temp_string = lines[1][equals_pos+2:]
        temp_c = float(temp_string) / 1000.0
        temp_f = temp_c * 9.0 / 5.0 + 32.0
        return temp_c, temp_f
    
  
config = {     
        "apiKey": "AIzaSyDSWDNvvm515sHNNsBsSvB709jmGhA9L9Q",
        "authDomain": "okos-terrarium-rendszer.firebaseapp.com",
        "databaseURL": "https://okos-terrarium-rendszer-default-rtdb.europe-west1.firebasedatabase.app/",
        "storageBucket": "okos-terrarium-rendszer.appspot.com"
}

firebase = pyrebase.initialize_app(config)
db = firebase.database()
storage = firebase.storage()

images = []

def upload_ten_images(image_name):
    global images
    
    images = db.child("images").get().val()
    if not images:
        images = []

    if len(images) >= 10:
        oldest_image = min(images, key=lambda x: datetime.datetime.strptime(x['timestamp'], '%Y-%m-%d_%H-%M-%S'))
        images.remove(oldest_image)
    
    new_image = {
        "fileName": image_name,
        "timestamp": datetime.datetime.now().strftime('%Y-%m-%d_%H-%M-%S')
    }
    images.append(new_image)

    db.child("images").set(images)
    
def sensor_thread():
    
    last_humidity_check_time = 0
    current_time = time.time()
     
    while True:
        t = 10
        humidity, temperature = Adafruit_DHT.read_retry(DHT_SENSOR, DHT_PIN)
        celsius, fahrenit = read_temp()

        if humidity is not None and temperature is not None and \
           celsius is not None and celsius > 0 and fahrenit is not None:
            data = {"Temp": round(celsius,2), "Hum": round(humidity,2)}
            db.child("TempHum").set(data)
            print("Temp={0:0.2f}C Humidity={1:0.2f}%".format(temperature, humidity))
            print("Celsius={0:0.2f}C°  Fahrenit={1:0.2f}F°".format(celsius, fahrenit))
            if current_time - last_humidity_check_time >= humidity_check_interval:
                if humidity < min_humidity:
                    water(RELAY, SECONDS_TO_WATER)
                    
                last_humidity_check_time = current_time
        else:
            print("Sensor failure. Check wiring.")
            t = 2
        time.sleep(t)

def motion_thread():
    
    background = None
    
    while True: 
        frame = vs.read()
        frame = frame if video_path is None else frame[1]
        
        state = "No change"
        
        if frame is None:
            break
        
        frame = cv2.resize(frame, (300, 250))
        
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        gray = cv2.GaussianBlur(gray, (21,21), 0)
        
        if background is None:
            background = gray
            continue
        
        delta_frame = cv2.absdiff(background, gray)
        threshold = cv2.threshold(delta_frame, T, 255,
        cv2.THRESH_BINARY)[1]
        threshold = cv2.dilate(threshold, None, iterations=2)
        
        cnts = cv2.findContours(threshold.copy(), cv2.RETR_EXTERNAL,
        cv2.CHAIN_APPROX_SIMPLE)
        cnts = imutils.grab_contours(cnts)
        
        for c in cnts:
            if cv2.contourArea(c) < min_area:
                continue
        
            (x,y,w,h) = cv2.boundingRect(c)
            filename = "img.jpg"
            cv2.imwrite(filename, frame)
            print("photo")
            current_time = datetime.datetime.now()
            img_filename = current_time.strftime('%Y-%m-%d_%H-%M-%S') + '.jpg'
            storage.child('images/'+ img_filename).put(filename)
            
            upload_ten_images(img_filename)
            
            time.sleep(10)
            break
            
        cv2.putText(frame, "Room Status: {}".format(state), (10, 20),
        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
        cv2.putText(frame, datetime.datetime.now().strftime("%A %d %B %Y %I:%M:%S%p"), (10, frame.shape[0] - 10),
        cv2.FONT_HERSHEY_SIMPLEX, 0.35, (0, 0, 255), 1)
        cv2.imshow("Camera image", frame)
        cv2.imshow("Threshold", threshold)
        cv2.imshow("Delta frame", delta_frame)
        
        key = cv2.waitKey(1) & 0xFF
        if key == ord("q"):
            break
           
    vs.stop() if video_path is None else vs.release()

motion_thread = threading.Thread(target=motion_thread)
sensor_thread = threading.Thread(target=sensor_thread)

motion_thread.start()
sensor_thread.start()

motion_thread.join()
sensor_thread.join()

camera.release()
cv2.destroyAllWindows()
