package org.jigsawrenaissance.ArduinoComm;

import java.io.BufferedInputStream;
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
    
    /** Our message transfer queue --  */
    private PoolQueue commandQueue;
    
    /** Our communication helper. */
    private ArduinoComm comm;
    
    /** Buffer ArduinoComm's output stream. */
    private BufferedOutputStream output;
    
    public ArduinoOut(ArduinoComm comm) {
        this.comm = comm;
        output = new BufferedOutputStream(comm.getOutputStream());
        commandQueue = new PoolQueue(Constants.OUT_QUEUE_MAX);
    }
    
    // @Debug: For testing, comment out comm and write the formatted messages
    // to the log.
    //public ArduinoOut() {
    //commandQueue = new PoolQueue(Constants.OUT_QUEUE_MAX);
    //}
    
    /** 
     * Format the command as per:
     * ...
     * @ToDo: Push the command format to Github and reference it here.
     */
    public void formatCommand(ArduinoMessage m, byte[] buffer) {
        // @ToDo
    }
    
    public void run() {
        byte[] buffer = new byte[Constants.MAX_MESSAGE_LEN];
        ArduinoMessage m = null;
        while (true) {
            // Block until there's a command.
            m = commandQueue.read();
            formatCommand(m, buffer);
            // @Debug: When formatCommand does something, print its result.
            Log.d(TAG, "Got command, type = " + m.type + ", which =" + m.which);
            // @Debug: Comment out the write.
            try {
                output.write(buffer, 0, buffer.length);
            } catch (IOException e) {
                Log.d(TAG, "write() threw IOException", e);
            }
            commandQueue.giveback(m);
        }
    }
}
