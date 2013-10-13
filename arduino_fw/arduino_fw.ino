void setup() {
  Serial.begin(9600);
  DDRD = DDRD | B11110000; 
}

void loop() {
}

void serialEvent() {
  while (Serial.available()) {
    byte val = (byte) Serial.read();
    PORTD = val;
    delay(200);
    PORTD = B0;    
  }
}

