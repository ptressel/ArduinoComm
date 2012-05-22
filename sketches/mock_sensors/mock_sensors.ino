
// This uses microbridge ADB for connecting to the phone.
// http://code.google.com/p/microbridge/source/browse/trunk/src/arduino/
#include <SPI.h>
#include <Adb.h>

// @Debug: Following includes are for debug messages only.
#include <stdio.h>
#include <string.h>

// ADB connection
#define ADB_CONNECTION_STRING "tcp:6000"
Connection* connection;

// Set true to have debugmsg send messages via Serial.
#define DEBUG 1
// Set true to echo back any incoming messages. Use this to test if your
// messages got to the board.
#define ECHO 1
// For converting bytes to hex.
#define ZERO '0'

// Longest expected transmission on input, and longest message we'll send,
// including any terminating CRLF. This must match MAX_MESSAGE_LEN in
// ArduinoComm.
#define MAX_MESSAGE_LEN 500
uint8_t out_buffer[MAX_MESSAGE_LEN] = {0};

// Incoming transmissions consist of triples of bytes where the first byte will
// be nonzero, the second may be nonzero, and the third will be zero. We expect
// transmissions may contain multiple triples, and that triples may be split
// across transmissions.
#define COMMAND_MESSAGE_LEN 3
// Since we may have commands split across transmissions, we may have up to
// COMMAND_MESSAGE_LEN - 1 leftover bytes. We don't need to save up the trailing
// zero byte, but we do need to know if a command is split before its trailing
// zero so we can clip it off the next transmission. So we only have to remember
// whether there are two or one outstanding bytes, and if there are two (i.e. we
// got the type but not the value), we need to save the type for next time.
uint8_t leftover = 0;
int num_leftovers = 0;

// Each fake sensor message is paired with a pause to follow the message.
// The pauses allow for approximate testing of timestamping on the receiving end.
// The pause time is specified in milliseconds.
// The messages are char* for convenience -- allows using literal strings.
// Do not try to typedef this.
// http://softsolder.com/2009/10/06/arduino-be-careful-with-the-preprocessor/
struct message_and_pause {
  char* message;
  int pause;
};

// Collection of mock sensor messages to send, in rotation. Quoted literals will
// be null-terminated by the compiler.
struct message_and_pause mock_messages[] = {
  {"$PRSO100,1\r\n", 30},
  {"$PRSO200,10,20,30,40,50\r\n", 60},
  {"$PRSO201,11,21,31,41,51*1\r\n", 90},
  {"$PRSO303,254*31\r\n", 30},
  {"$PRSO400,1*37\r\n", 60},
  {"$PRSO401,0*37\r\n", 90},
  {"$GPGGA,170834,4124.8963,N,08151.6838, W,1,05,1.5,280.2,M,-34.0, M,,*75\r\n", 30},
};
int num_mock_messages = sizeof(mock_messages) / sizeof(message_and_pause);
int which_message = 0;

void debugmsg(char* msg, boolean ln) {
  if (DEBUG) {
    if (ln) {
      Serial.println(msg);
    } else {
      Serial.print(msg);
    }
  }
}

void debugmsg(char* msg) {
  debugmsg(msg, true);
}

void debugmsg(int num, boolean ln) {
  if (DEBUG) {
    if (ln) {
      Serial.println(num);
    } else {
      Serial.print(num);
    }
  }
}

void debugmsg(int num) {
  debugmsg(num, true);
}

void adbEventHandler(Connection*, adb_eventType, uint16_t, uint8_t*);

void setup()
{
  // Serial monitor is only useful for debugging, so turn off DEBUG
  // in production.
  Serial.begin(115200);
  debugmsg("\r\nStarting mock_sensors.");
  
  // Open a connection to the phone.
  ADB::init();
  connection = ADB::addConnection(ADB_CONNECTION_STRING, true, adbEventHandler);
}

// Process one incoming command.
void processCommand(uint8_t target, uint8_t value) {
  // @ToDo: Add real command processing.
  debugmsg("Target = ", false);
  debugmsg(target);
  debugmsg("Value = ", false);
  debugmsg(value);
  if (ECHO) {
    // @Debug: send back to the phone.
    int num = sprintf((char*)out_buffer, "$ECHO,%d,%d\r\n", target, value);
    connection->write(num, out_buffer);
  }
}

// Pick off triples of bytes as commands and send the first byte (target)
// and second byte (value) off for processing. Processors should not take a
// lot of time for their work, and should *not block*.
//
// In case incoming transmissions split in the middle of commands, we save
// the tail of the previous transmission and prepend it to the next
// transmission.
//
// @ToDo: Find out if this actually happens. Find out if transmissions can
// drop chars. Should we send a resync command periodically, with a distinct
// pattern?
//
// The real code will call the appropriate processor -- the mock optionally
// logs to the serial monitor and sends the message back to the phone.
void bufferIncomingData(uint16_t length, uint8_t* data) {
  int start = 0;
  
  // Sanity check.
  if (length <= 0) {
    debugmsg("Got empty incoming transmission.");
    return;
  }
  
  // Assemble the first command out of our leftover plus bytes from the new
  // transmission.
  if (num_leftovers == 1) {
    // It's just the trailing zero left -- we already processed the command.
    start = 1;
  } else if (num_leftovers == 2) {
    processCommand(leftover, data[0]);
    if (length == 1) {
      // We got enough to process the command, but we still have a leftover
      // trailing zero, yet to come.
      num_leftovers = 1;
      return;
    }
    // We got both the value and trailing zero -- skip over them.
    start = 2;
  }
  
  // Subsequent commands are from the new transmission.
  int i = start;
  for (; i < length; i = i + COMMAND_MESSAGE_LEN) {
    processCommand(data[i], data[i+1]);
  }
  
  // Put any leftover in our stash. If we have no leftover, i is exactly
  // length.
  if (i == length) {
    num_leftovers = 0;
    return;
  }
  // i got incremented before the loop test, so if we subtract the increment
  // from i, then the difference between that and length is the number of
  // leftovers.
  num_leftovers = length - (i - COMMAND_MESSAGE_LEN);
  if (num_leftovers == 1) {
    // If we have one leftover, it's just zero. We can go ahead and process
    // the command.
    processCommand(data[length-2], data[length-1]);
    debugmsg("Had one leftover");
    return;
  } else if (num_leftovers == 2) {
    // If we have two leftovers, we need to save the last data byte.
    leftover = data[length-1];
    debugmsg("Had two leftovers");
    return;
  }
  // If we had any other number of leftovers, something is wrong.
  debugmsg("Unexpected number of leftovers: ", false);
  debugmsg(num_leftovers);
}

// This is called when ADB::poll() finds incoming data on USB.  See calls to
// ADB::fireEvent in Adb.ccp for the circumstances under which this is called.
// The events we get are:
// ADB_CONNECT -- got a connect request from the other end.
// ADB_CONNECTION_OPEN -- other side said okay to our connect request.
// ADB_CONNECTION_FAILED -- other side said no to our connect request.
// ADB_CONNECTION_CLOSE -- other side shut down the connection.
// ADB_CONNECTION_RECEIVE -- have incoming data.
// The mock reports the events, but responds only to receive.
// 
// Note: this is not an interrupt routine. This runs synchronously, inline
// in the run loop.
void adbEventHandler(Connection* connection,
                     adb_eventType event,
                     uint16_t length,
                     uint8_t* data) {
  if (event == ADB_CONNECTION_RECEIVE) {
    bufferIncomingData(length, data);
  } else if (event == ADB_CONNECT) {
    debugmsg("Got connect event");
  } else if (event == ADB_CONNECTION_FAILED) {
    debugmsg("Got connection failed event");
  } else if (event == ADB_CONNECTION_OPEN) {
    debugmsg("Got connection open event");
  } else if (event == ADB_CONNECTION_CLOSE) {
    debugmsg("Got connection close event");
  }
}

// The main purpose of the run loop is to:
// 1) Send sensor data.
// 2) Poll the incoming ADB connection.
// If polling finds data, the adbEventHandler is called. Thus incoming
// commands are serviced in adbEventHandler, or staged for servicing,
// while sensor data is sent within the run loop. In neither case should
// the code spend a lot of time working on one thing, nor should it block.
//
// @ToDo: Can we get interrupts from the sensors, or some of them?
// If we're going to poll, can we use timer interrupts rather than pausing
// with delay()?
//
// For mock sensors (on the other hand), we send the next predefined message,
// then wait for the predefined interval paired with that message. Incoming
// mock commands are reflected back within adbEventHandler.
void loop()
{
  // Poll ADB for incoming commands and other events.
  ADB::poll();
  
  // Send the next mock sensor message.
  char* message = mock_messages[which_message].message;
  debugmsg("About to send: ", false);
  debugmsg(message);
  connection->writeString(message);
  
  // Pause for the specified time.
  delay(mock_messages[which_message].pause);
  
  // Next!
  which_message = (which_message + 1) % num_mock_messages;
}

