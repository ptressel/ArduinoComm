package org.jigsawrenaissance.ArduinoComm;

/**
 * Shared constant values.
 * 
 * @author Pat Tressel
 */
public class Constants {
    // Port we're using for communication with the Arduino.
    // @ToDo: Could there be a conflict with this choice of port?
    public static final int SERVER_PORT = 6000;
    
    /** Max expected length of a single transmission to or from the Arduino.
     *  If messages get buffered into large collection, that may mean we're not
     *  getting them in a timely manner. */
    public static final int MAX_MESSAGE_LEN = 500;
    
    /** Length of one command message. */
    public static final int COMMAND_MESSAGE_LEN = 3;
    
    /** Max number of values in one message. This must match the number of
     *  valX fields in ArduinoMessage. */
    public static final int MAX_MESSAGE_VALUES = 20;
    
    // Input messages follow an NMEA-like format. For the GPS sensor, this is
    // the real NMEA format:
    //   $GPkkk,<arg1>,<arg2>,...,<argN>[*<cksum>]<cr><lf>
    // where details of the format are given here:
    // http://www.gpsinformation.org/dale/nmea.htm
    //
    // Format for other Arduino-connected sensors is:
    //   $PRSOxnn,<arg1>,<arg2>,...,<argN>[*<cksum>]<cr><lf>
    // where x indicates the type of sensor and nn which specific sensor within
    // that type. (x could be any byte, but currently is interpreted as a hex
    // digit. nn is interpreted as a two-digit decimal number.)
    // 
    // Most arguments for the non-GPS sensors are numeric. They may be sent as
    // raw bytes, in the form &I<hi><lo>. Otherwise they're interpreted as
    // decimal digits.
    //
    // The optional <cksum> is the XOR of the bytes between $ and *, exclusive,
    // encoded as two hex digits.
    //
    // See description of input format at:
    // ...
    // @ToDo: Push sensor and motor message format to Github.
    
    // Chars with meaning in our messages. These are 8-bit ASCII bytes.
    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;
    public static final byte DOLLAR = '$';
    public static final byte AMP = '&';
    public static final byte STAR = '*';
    public static final byte UPPER_I = 'I';
    public static final byte LOWER_I = 'i';
    public static final byte COMMA = ',';
    
    // @ToDo: Add constants for NMEA parsing and for identification of
    // specific messages. For preference, supply message formats so the
    // parser can be driven off a table of message prefixes and formats.
    
    // These chars end tokens.
    public static final byte[] BREAK_SET = {CR, COMMA, STAR};
    
    // Sensor type codes for sensors connected to the Arduino.
    public static final int SENSOR_LASER_RANGE_FINDER = 1;
    public static final int SENSOR_OPTICAL_FLOW = 2;
    public static final int SENSOR_SONAR = 3;
    public static final int SENSOR_BUMPER = 4;
    public static final int SENSOR_GPS = 5;
    
    // Sensor type codes for phone sensors.
    public static final int SENSOR_PHONE_GPS = 10;
    public static final int SENSOR_COMPASS = 11;
    public static final int SENSOR_VISION = 12;
    
    // Command message types
    public static final int COMMAND_STEERING = 1;
    public static final int COMMAND_THROTTLE = 2;
    public static final int COMMAND_TURRET_PAN = 3;
    public static final int COMMAND_TURRET_TILT = 4;
    
    // Suggested message transfer queue limits.
    // @ToDo: Provide a way to alert the control module if (e.g.) the out queue
    // is backing up.
    public static final int IN_QUEUE_MAX = 100;
    public static final int OUT_QUEUE_MAX = 100;
}
