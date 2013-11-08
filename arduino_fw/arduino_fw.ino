unsigned long lastCommandTimestamp;

void setup() {
  Serial.begin(9600);
  pinMode(5, OUTPUT);
  pinMode(6, OUTPUT);
  pinMode(10, OUTPUT);
  pinMode(11, OUTPUT); 
}

void loop() {
  if (millis() - lastCommandTimestamp > 200) {
    analogWrite(5, 0);
    analogWrite(6, 0);
    analogWrite(10, 0);
    analogWrite(11, 0); 
  }
}

void serialEvent() {
  while (Serial.available()) {
    byte command = (byte) Serial.read();
    analogWrite(command, command < 10 ? 160 : 200);
    lastCommandTimestamp = millis();    
  }
}

