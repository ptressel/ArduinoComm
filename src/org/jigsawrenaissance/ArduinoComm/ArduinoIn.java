package org.jigsawrenaissance.ArduinoComm;

import java.io.BufferedInputStream;
import java.io.IOException;

import android.os.SystemClock;
import android.util.Log;

/**
 * Read from the Arduino over USB using ADB as the transmission protocol.
 * Parse sensor messages, put their contents in ArduinoMessage objects, and
 * queue them up for the control module.
 * 
 * Format of the messages is based on NMEA:
 * http://www.gpsinformation.org/dale/nmea.htm
 * http://aprs.gids.nl/nmea/
 * 
 * @ToDo: The incoming messages have checksums, but there is an uncertainty
 * in their definition. The checksum calculator here:
 * http://www.hhhh.org/wiml/proj/nmeaxor.html
 * does not seem to include spaces in the count. The reason this is important
 * is that we don't construct the messages from the external GPS, and the
 * description of NMEA GGA message here:
 * http://aprs.gids.nl/nmea/#gga
 * shows blanks in the example of message data. If the GPS will be computing
 * its own checksum, then we need to abide by whatever its computation is.
 * 
 * @author Pat Tressel
 */
public class ArduinoIn implements Runnable {
    public static final String TAG = "ArduinoIn";
    
    /** Our message transfer queue -- we queue up the ArduinoMessages we
     *  extract from the data here. This will be shared with other modules
     *  that also read sensor data from other sources. */
    private PoolQueue sensorQueue;
    
    /** Our communication helper. */
    private ArduinoComm comm;
    
    /** Buffer ArduinoComm's input stream. */
    private BufferedInputStream input;
    // @Debug:
    //private MockInputStream input;
    
    /**
     * @Debug:
     * For debugging the queuing, parsing, threading, mock out the board I/O.
     * Repeat a canned script of "messages". The message texts are stored as
     * String, which, being Unicode, is not necessarily representative of the
     * character encoding that we'll receive from the board -- that depends on
     * how InputStream transforms the packet data.
     * 
     * This is not a true mock of InputStream, i.e. does not subclass it. One
     * must comment out the use of ArduinoComm and substitute this. It provides
     * only the one InputStream method that is used here -- read().
     */
    /*
    public class MockInputStream {
        public final byte[][] messages = {
            "$PRSO100,1\r\n".getBytes(),
            "$PRSO200,10,20,30,40,50\r\n".getBytes(),
            "$PRSO201,11,21,31,41,51*1\r\n".getBytes(),
            "$PRSO303,254*31\r\n".getBytes(),
            "$PRSO400,1*37\r\n".getBytes(),
            "$PRSO401,0*37\r\n".getBytes(),
            "$GPGGA,170834,4124.8963,N,08151.6838, W,1,05,1.5,280.2,M,-34.0, M,,*75".getBytes(),
        };
        
        // These point to the next byte to return.
        private int i = 0;  // index into messages array
        private int j = 0;  // index within the current message
        
        public int read() {
            // This doesn't handle empty messages.
            byte b = messages[i][j];
            if (j == (messages[i].length - 1)) {
                // End of that message.
                j = 0;
                i = (i + 1) % messages.length;
            } else {
                ++j;
            }
            return b;
        }
    }
    */
    
    /** This is the real constructor. */
    public ArduinoIn(ArduinoComm comm) {
        this.comm = comm;
        input = new BufferedInputStream(comm.getInputStream());
        sensorQueue = new PoolQueue(Constants.IN_QUEUE_MAX);
    }
    
    /** @Debug:
     *  This constructor uses the mock. Requires changing the type of input
     *  and changing the instantiation in ArduinoCommActivity. */
    /*
    public ArduinoIn() {
        input = new MockInputStream();
        sensorQueue = new PoolQueue(Constants.IN_QUEUE_MAX);
    }
    */
    
    /** Provide our queue for the control module. */
    public PoolQueue getQueue() {
        return sensorQueue;
    }
    
    private int readInput() {
        int b = 0;
        // @Debug: Comment out the try catch.
        try {
            Log.d(TAG, "About to call read()");
            b = input.read();
            Log.d(TAG, "read() returned " + b);
        } catch (IOException e) {
            Log.d(TAG, "read() threw IOException", e);
            return -1;
        }
        if (b < 0) {
            Log.d(TAG, "read() returned -1");
            return -1;
        }
        return b;
    }
    
    /** 
     * Read from USB off the main thread.  Our messages are CRLF-terminated
     * 8-bit text with comma-separated fields.
     * 
     * @ToDo: The message semantics are embedded. Might be cleaner to move the
     * interpretation out. Just tokenize here and hand off the interpretation.
     */
    public void run() {
        Log.d(TAG, "Worker thread started.");
        
        // Byte read from Arduino. It's an int because read returns -1 if
        // there's no more data, i.e. if the Arduino closes its socket.
        int b = 0;
        int b2 = 0;
        // The message we're currently assembling.
        ArduinoMessage m = null;
        // Flag for whether we peeked ahead at the next byte.
        boolean peeked = false;
        // Flag for end of message.
        boolean msgDone = false;
        // @Debug:
        // This is a stand-in for the real parser output.  For testing, we are
        // just going to display and log the text messages.  We'll assemble the
        // text here.
        StringBuilder text = new StringBuilder(Constants.MAX_MESSAGE_LEN);
        
        while (true) {
            // Here, we're at the start of a new message.
            msgDone = false;
            // Get an empty message.
            m = sensorQueue.obtain();
            // @Debug: Empty our text storage.
            text.setLength(0);
            // Get a timestamp -- we'll use elapsed time since boot, not wall
            // clock, as the latter can get changed arbitrarily, and we want
            // accurate intervals.
            m.time = SystemClock.elapsedRealtime();
            // @Debug:
            text.append(m.time).append((char)Constants.COMMA);
            
            // @ToDo: Refactor this. Read a whole message up to the CRLF
            // without parsing, then tokenize & split on break chars. Can't
            // blindly split on commas as the bytes following &I might equal a
            // comma. Better, use a parser generator, or an NMEA package.
            // @ToDo: Regardless of method, finish the parser for the actual
            // sensor formats.
            
            // Read until we see a CRLF, which ends one message.
            while (!msgDone) {
                // Some formats require peeking ahead one char -- if we did
                // that, the char is already in b.
                if (!peeked) {
                    b = readInput();
                }
                peeked = false;
                if (b < 0) {
                    // @ToDo: Is there a way to recover if we get an exception
                    // or end of stream? Can we close and re-open the socket?
                    return;
                }
                
                switch (b) {
                case Constants.CR:
                    b = readInput();
                    if (b == Constants.LF) {
                        // At end of message -- send it off.
                        // @Debug: This is Linux -- don't need the CR.
                        text.append((char)Constants.LF);
                        // @Debug:
                        m.obj = new String(text);
                        Log.d(TAG, "End of message, sending.");
                        sensorQueue.send(m);
                        // Don't hold a reference to the message.
                        m = null;
                        msgDone = true;
                    } else {
                        // CR in the middle of a message -- probably a mistake.
                        text.append((char)Constants.CR);
                        peeked = true;
                    }
                    break;
                    
                case Constants.AMP:
                    // &I prefixes a two-byte analog value, high byte first.
                    b = readInput();
                    if ((b == Constants.UPPER_I) || (b == Constants.LOWER_I)) {
                        Log.d(TAG, "Found &I");
                        b = readInput();
                        b2 = readInput();
                        int val = comm.composeInt(b, b2);
                        // @Debug:
                        text.append(val);
                    } else {
                        // It's just an &...
                        // @Debug:
                        text.append((char)Constants.AMP);
                    }
                    break;
                
                default:
                    // @Debug:
                    text.append((char)b);
                    break;
                }
            }
        }
    }

}
