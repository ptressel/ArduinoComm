package org.jigsawrenaissance.ArduinoComm;

import java.io.BufferedInputStream;
import java.io.IOException;

import android.os.SystemClock;
import android.util.Log;

public class ArduinoIn extends ArduinoComm {
    public static final String TAG = "ArduinoIn";
    
    /** Max expected length of a single message from the Arduino. */
    public static final int MAX_MESSAGE_LEN = 1024;
    // Chars with meaning in our messages.  These are 8-bit ASCII bytes.
    // See description of input format at:
    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;
    public static final byte AMP = '&';
    public static final byte UPPER_I = 'I';
    public static final byte LOWER_I = 'i';
    public static final byte COMMA = ',';
    
    private BufferedInputStream input;
    
    /**
     * For debugging the queuing, parsing, threading, mock out the board I/O.
     * Repeat a canned script of "messages". The message texts are stored as
     * String, which, being Unicode, is not necessarily representative of the
     * character encoding that we'll receive from the board -- that depends on
     * how InputStream transforms the packet data.
     */
    public class MockInputStream {
        public final String[] messages = {
            "x",
            "y"
        };
        
    }
    
    public ArduinoIn(int max) throws IOException {
        super(max);
        // Wrap ArduinoComm's input stream.
        input = new BufferedInputStream(getInputStream());
    }
    
    private int readInput() {
        int b = 0;
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
        // This is a stand-in for the real parser output.  For testing, we are
        // just going to display and log the text messages.  We'll assemble the
        // text here.
        StringBuilder text = new StringBuilder(MAX_MESSAGE_LEN);
        
        while (true) {
            // Here, we're at the start of a new message.
            msgDone = false;
            // Get an empty message.
            m = obtain();
            // Empty our text storage.
            text.setLength(0);
            // Get a timestamp -- we'll use elapsed time since boot, not wall
            // clock, as the latter can get changed arbitrarily, and we want
            // accurate intervals.
            m.time = SystemClock.elapsedRealtime();
            text.append(m.time).append((char)COMMA);
            
            // @ToDo: Refactor this? Read a whole message up to the CRLF
            // without parsing, then tokenize & split on commas. Can't blindly
            // split on commas as the bytes following &I might equal a comma.
            
            // Read until we see a CRLF, which ends one message.
            while (!msgDone) {
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
                case CR:
                    b = readInput();
                    if (b == LF) {
                        // At end of message -- send it off.
                        // This is Linux -- don't need the CR.
                        text.append((char)LF);
                        m.obj = new String(text);
                        Log.d(TAG, "End of message, sending.");
                        send(m);
                        msgDone = true;
                    } else {
                        // CR in the middle of a message -- probably a mistake.
                        text.append((char)CR);
                        peeked = true;
                    }
                    break;
                    
                case AMP:
                    // &I prefixes a two-byte analog value, high byte first.
                    b = readInput();
                    if ((b == UPPER_I) || (b == LOWER_I)) {
                        Log.d(TAG, "Found &I");
                        b = readInput();
                        b2 = readInput();
                        int val = composeInt(b, b2);
                        text.append(val);
                    } else {
                        // It's just an &...
                        text.append((char)AMP);
                    }
                    break;
                
                default:
                    text.append((char)b);
                    break;
                }
            }
        }
    }

}
