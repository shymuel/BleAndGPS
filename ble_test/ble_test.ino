/*
 * code from https://www.instructables.com/id/ESP32-BLE-Android-App-Arduino-IDE-AWESOME/
 * 2020.2.19
 * shymuel
 */
#include <TTGO.h>

TTGOClass *ttgo;
TFT_eSPI *tft ;
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
 
BLECharacteristic *pCharacteristic;
bool deviceConnected = false;
char BLEbuf[32] = {0};
uint32_t cnt = 0;
 
#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
 
class MyServerCallbacks: public BLEServerCallbacks {  //connection status
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };
 
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};
 
class MyCallbacks: public BLECharacteristicCallbacks {  //handles receiving data being sent from the client (phone)
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue(); 
 
      if (rxValue.length() > 0) {
        Serial.print("------>Received Value: ");
 
        for (int i = 0; i < rxValue.length(); i++) {
          Serial.print(rxValue[i]);
        }
        Serial.println();
       
        if (rxValue.find("A") != -1) {   //接收到A？？
          Serial.print("Rx A!");
        }
        else if (rxValue.find("B") != -1) {
          Serial.print("Rx B!");
        }
        Serial.println();
      }
    }
};
void setup() {
  Serial.begin(115200);

  //打开LCD
  ttgo = TTGOClass::getWatch();
  ttgo->begin();
  ttgo->openBL();
  //! Create a new pointer to save the display object
  tft = ttgo->eTFT;

    tft->fillScreen(TFT_BLACK);
    tft->setTextFont(2);
    tft->println("Begin ublox M8M/GPS Module...");

  tft->setCursor(0, 0);
    tft->fillScreen(TFT_BLACK);
    tft->println("Running...");

    
  // Create the BLE Device
  BLEDevice::init("ESP32 BLE Test");  //设备名称
  
  // Create the BLE Server
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  
  // Create the BLE Service using the service UUID
  BLEService *pService = pServer->createService(SERVICE_UUID);
  
  // Create a BLE Characteristic 
  pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
                   
  pCharacteristic->addDescriptor(new BLE2902());  //the ESP32 won't notify the client unless the client wants to open its ears up to read the values
  
  BLECharacteristic *pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
  
  pCharacteristic->setCallbacks(new MyCallbacks());
  
  // Start the service
  pService->start();
  
  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
}
 
void loop() {
  if (deviceConnected) {
    // Let's convert the value to a char array:
    //char txString[8]; // make sure this is big enuffz
    //dtostrf(txValue, 1, 2, txString); // float_val, min_width, digits_after_decimal, char_buffer
    
//    pCharacteristic->setValue(&txValue, 1); // To send the integer value
//    pCharacteristic->setValue("Hello!"); // Sending a test message

    
    memset(BLEbuf, 0, 32); //填充32个0
    memcpy(BLEbuf, (char*)"Hello BLE APP!", 32);  //max allowable data size per packet is 20 bytes for BLE specification
    pCharacteristic->setValue(BLEbuf);
    
    pCharacteristic->notify(); // Send the value to the app!
    //Serial.print("*** Sent Value: ");
    //Serial.print(BLEbuf);
    //Serial.println(" ***");
  }
  delay(1000);
}
