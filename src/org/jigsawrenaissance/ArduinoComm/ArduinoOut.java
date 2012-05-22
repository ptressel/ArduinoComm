package org.jigsawrenaissance.ArduinoComm;

import java.io.BufferedOutputStream;
import java.io.IOException;

import android.util.Log;

/**
 * We receive ArduinoMessages representing commands from the control module,
 * format them as per:
 * ...
 * and send them to the Arduino.
 * 
 * @author Pat Tressel
 */
public class ArduinoOut implements Runnable{
    public static final String TAG = "ArduinoOut";
    
    /** Our message transfer queue. */
    private PoolQueue commandQueue;
    
    /** Buffer ArduinoComm's output stream. */
    private BufferedOutputStream output;
    
    /** Scratch buffer for formatting command messages.
     *  BufferedOutputStream.write() copies the contents of the buffer it's
     *  passed before it returns, so we don't need to allocate a new buffer
     *  for each write. */
    private byte[] buffer = new byte[Constants.COMMAND_MESSAGE_LEN];
    
    public ArduinoOut(ArduinoComm comm) {
        output = new BufferedOutputStream(comm.getOutputStream());
        commandQueue = new PoolQueue(Constants.OUT_QUEUE_MAX);
    }
    
    // @Debug: For testing, comment out comm and write the formatted messages
    // to the log.
    //public ArduinoOut() {
    //commandQueue = new PoolQueue(Constants.OUT_QUEUE_MAX);
    //}
    
    /** Provide our queue for the control module. */
    public PoolQueue getQueue() {
        return commandQueue;
    }
    
    /** 
     * Format the command as per:
     * ...
     * There are only two values per command -- the command target (steering,
     * throttle, turret pan, turret tilt), which is supplied in the message
     * type, and the value, supplied in val1. The command is sent to the board
     * as three bytes: type, value, zero.
     * 
     * @ToDo: Push the command format to Github and reference it here.
     */
    public void formatCommand(ArduinoMessage m) {
        Log.d(TAG, "Got command, type = " + m.type + ", value =" + m.val1);
        buffer[0] = (byte)m.type;
        buffer[1] = (byte)m.val1;
        buffer[2] = 0;
    }
    
    public void run() {
        ArduinoMessage m = null;
        while (true) {
            // Block until there's a command.
            m = commandQueue.read();
            formatCommand(m);            
            // @Debug: Comment out the write.
            try {
                output.write(buffer, 0, buffer.length);
                // Flush after each command -- a bit wasteful of system calls,
                // but we want the board to get commands without delay.
                output.flush();
            } catch (IOException e) {
                Log.d(TAG, "write() threw IOException", e);
            }
            commandQueue.giveback(m);
        }
    }
}
