unsigned long lastCommandTimestamp;

void setup() {
  Serial.begin(9600);
  DDRD = DDRD | B11110000; 
}

void loop() {
  if (millis() - lastCommandTimestamp > 200) {
    PORTD = B0;  
  }
}

void serialEvent() {
  while (Serial.available()) {
    PORTD = PORTD | (byte) Serial.read();
    lastCommandTimestamp = millis();    
  }
}

