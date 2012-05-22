package org.jigsawrenaissance.ArduinoComm;

import android.util.Log;

/**
 * This is a stand-in for the real controller. This consumes incoming
 * sensor (or mock sensor) messages and emits fake commands.
 * 
 * @author Pat Tressel
 */
public class MockController implements Runnable {
    public static final String TAG = "MockController";

    /** Our end of the ArduinoIn sensor queue. */
    private PoolQueue sensorQueue;
    /** Our end of the ArduinoOut command queue. */
    private PoolQueue commandQueue;
    
    // A set of fake commands to send.
    private int which_command = 0;
    private byte[][] mock_commands = {
            {(byte)0x01, (byte)0x80},
            {(byte)0x02, (byte)0xFF},
            {(byte)0x03, (byte)0x10},
            {(byte)0x04, (byte)0xEF},
            {(byte)0x05, (byte)0x01}
    };
    
    /** During setup, after ArduinoIn and ArduinoOut are instantiated, get
     *  their queues and hand them over to us. Start the controller first,
     *  then ArduinoOut, then ArduinoIn. The controller should initially
     *  block on the sensor queue, so everything will be ready before it
     *  starts operating. */
    public MockController(PoolQueue sensorQueue, PoolQueue commandQueue) {
        this.sensorQueue = sensorQueue;
        this.commandQueue = commandQueue;
    }
    
    public void run() {
        ArduinoMessage m = null;
        while (true) {
            // Read one sensor message.
            m = sensorQueue.read();
            Log.d(TAG, "Picked from sensor queue: " + m.obj);
            sensorQueue.giveback(m);
            
            // Write one fake command.
            m = commandQueue.obtain();
            m.type = mock_commands[which_command][0];
            m.val1 = mock_commands[which_command][1];
            commandQueue.send(m);
        }
    }
}
