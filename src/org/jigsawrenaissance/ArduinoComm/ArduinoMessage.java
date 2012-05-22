package org.jigsawrenaissance.ArduinoComm;

/** 
 * Holder for data read from and sent to the Arduino. 
 * Yes, all the fields are public, for performance.
 * We don't use Android's Message class because it only has a couple of
 * primitive fields, so we'd have to create objects to hold our data --
 * exactly what we're trying to avoid by having a pool of message objects.
 * 
 * @ToDo: Change the following so that some values can be floats, e.g.
 * lat and lon. That spoils ordering the values as they are in the NMEA
 * strings, so provide accessors for each type of data.
 * 
 * All argument values (val1 through val20) are encoded as integers.
 * For incoming messages, the order of order of values is the same as they
 * appear in the NMEA string. For outgoing commands, only val1 is used --
 * the target of the command is specified in type, and the terminating zero
 * byte will be added by ArduinoOut when it constructs the outgoing text.
 * 
 * @author Pat Tressel
 */
public class ArduinoMessage {
    /** Timestamp, milliseconds since phone boot. */
    public long time;
    /** Type of message, i.e. which type of sensor or command. */
    public int type;
    /** Which specific sensor (within type) this comes from, or which
     *  specific system (within type) this command is directed at. */
    public int which;
    /** Number of argument values supplied in this message. First value is in
     *  val1, and there are no gaps, i.e if nvals is some number N, then the
     *  valid arguments are val1 through valN. */
    public int nvals;
    // All arguments get encoded as integers. For incoming messages the
    // order of arguments is the same as they appear in the NMEA string.
    // For outgoing commands, only the first two are used -- a third
    // zero byte will be added by ArduinoOut.
    public int val1;
    public int val2;
    public int val3;
    public int val4;
    public int val5;
    public int val6;
    public int val7;
    public int val8;
    public int val9;
    public int val10;
    public int val11;
    public int val12;
    public int val13;
    public int val14;
    public int val15;
    public int val16;
    public int val17;
    public int val18;
    public int val19;
    public int val20;
    
    // Temporary field for testing. We don't want to require users of this
    // class to instantiate objects for each message -- defeats the purpose
    // of using a pool.
    public Object obj;
    
    // @ToDo: If we want to ditch LinkedBlockingDeque and LinkedBlockingQueue
    // in favor of just linking ArduinoMessage objects directly, add a next
    // field. For justification see the Android Message and MessageQueue
    // classes.
}
