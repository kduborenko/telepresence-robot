unsigned long lastCommandTimestamp;
unsigned long lastTurnCommandTimestamp;

int steeringWheelBalance = 330;
int steeringWheelBalanceAccuracy = 2;

int FORWARD = 5;
int BACKWARD = 6;
int RIGHT = 10;
int LEFT = 11;

void setup() {
  Serial.begin(9600);
  pinMode(FORWARD, OUTPUT);
  pinMode(BACKWARD, OUTPUT);
  pinMode(RIGHT, OUTPUT);
  pinMode(LEFT, OUTPUT); 
}

void loop() {
  if (millis() - lastCommandTimestamp > 1000) {
    analogWrite(FORWARD, 0);
    analogWrite(BACKWARD, 0);
    analogWrite(RIGHT, 0);
    analogWrite(LEFT, 0);
  }
  if (millis() - lastTurnCommandTimestamp > 200) {
    int sensorValue = analogRead(A0);
    if (sensorValue < steeringWheelBalance - steeringWheelBalanceAccuracy) {
      analogWrite(RIGHT, 160); 
    } else if (sensorValue > steeringWheelBalance + steeringWheelBalanceAccuracy) {
      analogWrite(LEFT, 160);
    }
  }
  delay(10);
}

void serialEvent() {
  while (Serial.available()) {
    byte command = (byte) Serial.read();
    analogWrite(FORWARD, command & 1 ? 160 : 0);
    analogWrite(BACKWARD, command & 2 ? 160 : 0);
    analogWrite(LEFT, command & 4 ? 200 : 0);
    analogWrite(RIGHT, command & 8 ? 200 : 0);
    lastTurnCommandTimestamp = (command & 12) ? millis() : 0;
    lastCommandTimestamp = millis();    
  }
}

