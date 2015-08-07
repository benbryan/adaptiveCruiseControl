#include <SoftwareSerial.h>  
#include <avr/wdt.h>

#define RxD 2
#define TxD 3
 
#define DEBUG_ENABLED  0
 
class Buff{
  public:
    char *b;
    byte idx;
    byte length;
    Buff(int length){
      this->length = length;
      b = (char*)malloc(length*sizeof(char));
      for (int i = 0; i < length; i++){
        b[i] = 0;
      }
      idx = 0;
    };
};

class DAC{
  public:
    DAC(){
      DDRC |= (1<<4); // CS
      DDRC |= (1<<3); // SCK
      DDRD |= (1<<6); // SDI
      DDRD |= (1<<7); // LDAC
      setCS(1);
      setSCK(0);
      setSDI(1);
      setLDAC(1);
    }
    void setCS(boolean state){
      if (state){
        PORTC |= (1<<4);
      } else {
        PORTC &= ~(1<<4);        
      }
    }
    void setSCK(boolean state){
      if (state){
        PORTC |= (1<<3);
      } else {
        PORTC &= ~(1<<3);        
      }
    }
    void setSDI(boolean state){
      if (state){
        PORTD |= (1<<6);
      } else {
        PORTD &= ~(1<<6);        
      }
    }
    void setLDAC(boolean state){
      if (state){
        PORTD |= (1<<7);
      } else {
        PORTD &= ~(1<<7);        
      }
    }
    
    void set(int value){
      setCS(0);
      delay(1);
      for (int i = 0; i < 16; i++){
        switch (i){
          case 0:
            setSDI(0); //channel A;
            break;
          case 1:
            setSDI(0); //Dont care
            break;
          case 2:
            setSDI(0); //Output gain
            break;
          case 3:
            setSDI(1); //Output enable
            break;
          default:
            int mask = 1<<(15-i);
            setSDI(value&mask);
            break;
        }
        delay(1);
        setSCK(1);
        delay(1);
        setSCK(0);
      }
      setCS(true);
      delay(1);
      setLDAC(false);
      delay(1);
      setLDAC(true);
    }
};

class ADC_Buffer{
  int adcLast;
  int adcIdx;
  boolean initialized;
  public:
    int thresh = -1;
    ADC_Buffer(int adcIdx){
      this->adcIdx = adcIdx;
      this->initialized = false;
    }
    boolean update(){
      if (thresh == -1){
        return false;
      }
      if (initialized){
        int value = analogRead(adcIdx);
        int diff = abs(adcLast - value);
        if (diff > thresh){
          sendADCReading(adcIdx, value);
          return true;
        }
      } else {
        int value = analogRead(adcIdx);
        adcLast = value;
        initialized = true;
        return false;
      }
    }
};

Buff rxBuff(100);
DAC dac;
ADC_Buffer adcBuffer0(0);
SoftwareSerial blueToothSerial(RxD,TxD);

void setup() { 
//  //UART hardware
//  Serial.begin(38400);
  
  dac = DAC();
  dac.set(4000);
  
  //relay0 enable
  DDRC |= (1<<1); 
  PORTC |= (1<<1); 

  pinMode(RxD, INPUT);
  pinMode(TxD, OUTPUT);  

  //Bluetooth is connected pin
  DDRB &= ~(1<<2);
  PORTB &= ~(1<<2);

  // Bluetooth disconnect
  PORTC &= ~(1<<2);
  DDRC |= (1<<2);

  // Bluetooth INQ request
  DDRD &= ~(1<<4);
  PORTD |= (1<<4);

  //Set BluetoothBee BaudRate to default baud rate 38400
  blueToothSerial.begin(38400); 
  delay(1000);    

  sendBlueToothCommand("\r\n+INQ=1\r\n");
  sendBlueToothCommand("\r\n+INQ=0\r\n");

} 

void parseRX(char *buff, int length){
    byte eqIdx = 0;
    int i = 0;
    while (true){
      if (buff[i] == '='){
        buff[i] = 0;
        eqIdx = i;
        break;
      }
      if ((buff[i] == 0) || (i == length)){
        return;
      }
      i++;
    }
    char *name = buff;    
    long int value = strtol(&buff[eqIdx+1], NULL, 10);
    if (strcmp("adc",name)==0){
      if (value > 7){
        blueToothSerial.write("\rerror:adc idx out of bounds\r");
        return;
      }
      int reading = analogRead(value);
      sendADCReading(value, reading);
      return;
    } else if (strcmp("dac",name)==0){
      dac.set(value);
      blueToothSerial.print("\r\ndac=set\r\n");
      return;
    } else if (strcmp("relay",name)==0){
      if (value){
        PORTC |= (1<<1);
        blueToothSerial.print("\r\nrelay=on\r\n");
      } else {
        PORTC &= ~(1<<1);
        blueToothSerial.print("\r\nrelay=off\r\n");
      }
      return;
    } else if (strcmp("adcThresh0",name)==0){
      adcBuffer0.thresh = value;
      return;
    }

//    Serial.write(name, eqIdx);
//    Serial.write('\r'); 
}

void sendADCReading(int value, int reading){
  blueToothSerial.print("\r\n");
  blueToothSerial.print("adc");
  blueToothSerial.print(value);
  blueToothSerial.print("=");
  blueToothSerial.print(reading);
  blueToothSerial.print("\r\n");
}

boolean isBluetoothConnected(){
  return PINB & (1<<2);
}

unsigned int adcUpdateTimer = 0;
unsigned int bluetoothINQ_Delay = 0;
void loop() { 
//  while(true){
//    if(blueToothSerial.available()){
//      char c = blueToothSerial.read();
//      Serial.write(c);
//    }
//    if (Serial.available()){
//      char c = Serial.read();
//      blueToothSerial.write(c);
//    }
//  }
  
 // wdt_enable(WDTO_4S);
  while (true){
   // wdt_reset();
    if (isBluetoothConnected()){
      if(blueToothSerial.available()){
        char c = blueToothSerial.read();
        if ((c == '\n') || (c=='\r')){
          rxBuff.b[rxBuff.idx] = 0;
          rxBuff.idx = 0; 
          parseRX(rxBuff.b, rxBuff.length);         
        } else if (rxBuff.idx == (rxBuff.length-3)){
          rxBuff.b[0] = c;
          rxBuff.idx = 1;
        } else {
          rxBuff.b[rxBuff.idx++] = c;
        }
      }
      if (adcUpdateTimer == 0 ){
        if (adcBuffer0.update()){
          adcUpdateTimer = 7000;        
        }
      } else {
        adcUpdateTimer--;
      }
    } else {
      if (adcUpdateTimer++ == 0){
        dac.set(4000);
        PORTC |= (1<<1);
      }
    }
    if (bluetoothINQ_Delay == 0){
      if (PIND & (1<<4)){ 
      } else { 
        setupBlueToothConnection();
        setBluetoothINQ();
        bluetoothINQ_Delay = 30000;
      }
    } else if (bluetoothINQ_Delay > 0) {
      bluetoothINQ_Delay--;
    }
  }
} 

void disconnectBluetooth(){
  PORTC |= (1<<2);
  delay(1000);
  PORTC &= ~(1<<2);
}

void setBluetoothINQ(){
  sendBlueToothCommand("\r\n+STPIN=0000\r\n");
  delay(2000); // This delay is required.
  sendBlueToothCommand("\r\n+INQ=1\r\n");
  delay(2000); // This delay is required.
}
 
void setupBlueToothConnection() {
    sendBlueToothCommand("\r\n+STWMOD=0\r\n");
    sendBlueToothCommand("\r\n+STNA=Cruise Control\r\n");
    sendBlueToothCommand("\r\n+STAUTO=0\r\n");
    sendBlueToothCommand("\r\n+STOAUT=1\r\n");
    delay(2000); // This delay is required.
}
 
void CheckOK() {
  char a,b;
  while(1) {
    if(blueToothSerial.available()) {
      a = blueToothSerial.read();
 
    if('O' == a) {
      // Wait for next character K. available() is required in some cases, as K is not immediately available.
      while(blueToothSerial.available()) {
         b = blueToothSerial.read();
         break;
      }
      if('K' == b) {
        break;
      }
    }
   }
  }
 
  while( (a = blueToothSerial.read()) != -1) {
    //Wait until all other response chars are received
  }
}
 
void sendBlueToothCommand(char command[]) {
    blueToothSerial.print(command);
    CheckOK();   
}


