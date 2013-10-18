unsigned long lastCommandTimestamp;

void setup() {
  Serial.begin(9600);
  DDRD = DDRD | B11110000; 
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
    analogWrite(command, 160);
    lastCommandTimestamp = millis();    
  }
}

