package org.jigsawrenaissance.ArduinoComm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

/**
 * Holds common elements for communication with the Arduino, in particular, the
 * server socket.
 * 
 * @author Pat Tressel
 */
public class ArduinoComm {
    public static final String TAG = "ArduinoComm";
    
    // The input and output workers share one socket, which is owned by the
    // class. We only expose the input and output streams to them.
    // @ToDo: This should be a singleton. Provide a factory method.
    private final Object socketLock = new Object();
    private Socket socket;
    private ServerSocket server;
    private InputStream input;
    private OutputStream output;
    
    private void initSocket() throws IOException {
        synchronized (socketLock) {
            if (server == null) {
                try {
                    Log.d(TAG, "About to open server socket.");
                    server = new ServerSocket(Constants.SERVER_PORT);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to create ServerSocket", e);
                    throw e;
                }
                Log.d(TAG, "About to call accept on server socket.");
                socket = server.accept();
                Log.d(TAG, "After accept.");
                input = socket.getInputStream();
                output = socket.getOutputStream();
            } else {
                Log.d(TAG, "Server socket already open.");
            }
        }
    }
    
    public ArduinoComm() throws IOException {
        initSocket();
    }
    
    // Note after the constructor completes, the input and output streams
    // should be available, thus the ArduinoComm instance methods can
    // assume they exist, and don't need to check for null.
    
    /** Used by ArduinoIn to get the input stream. */
    protected InputStream getInputStream() {
        return input;
    }
    
    /** Used by ArduinoOut to get the output stream. */
    protected OutputStream getOutputStream() {
        return output;
    }
    
    /** Helper that converts two bytes to an int. */
    protected int composeInt(int hi, int lo) {
        int val = hi & 0xff;
        val *= 256;
        val += lo & 0xff;
        return val;
    }
}
