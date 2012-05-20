package org.jigsawrenaissance.ArduinoComm;

/** Holder for data read from and sent to the Arduino. */
public class ArduinoMessage {
    // Sensor messages
    public static final int VISION = 1;
    public static final int COMPASS = 3;
    public static final int SONAR = 3;
    public static final int BUMPER = 4;
    
    // Command messages
    public static final int XXX = 1;
    
    /** Timestamp, milliseconds since phone boot. */
    public long time;
    /** Type of message, i.e. which type of sensor or command. */
    public int type;
    /** Which specific sensor (within type) this comes from, or which
     *  specific system (within type) this command is directed at. */
    public int which;
    
    public int val1;
    public int val2;
    public int val3;
    
    // Temporary field for testing. We don't want to require users of this
    // class to instantiate objects for each message -- defeats the purpose
    // of using a pool.
    public Object obj;
}
