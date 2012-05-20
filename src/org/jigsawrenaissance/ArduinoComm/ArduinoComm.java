package org.jigsawrenaissance.ArduinoComm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

/**
 * Holds a FIFO for receiving or sending reusable message objects, and a pool
 * of messages to draw from, to avoid overhead of creating and garbage
 * collecting messages. If the pool runs dry, new messages are created, up to
 * a specified limit. It is the responsibility of the the recipient of messages
 * to give them back.
 * 
 * To read messages, call get() to wait if no messages are ready, or poll() to
 * return null in that case. To send, call obtain() to get a message object, up
 * to the specified limit, and put() to queue it up -- obtain() blocks if the
 * pool is exhausted; put() does not block.
 * 
 * @author Pat Tressel
 */
public abstract class ArduinoComm implements Runnable {
    public static final String TAG = "ArduinoComm";
    
    // Suggested queue limits.
    // @ToDo: Provide a way to alert the control module if (e.g.) the out queue
    // is backing up.
    public static final int IN_QUEUE_MAX = 100;
    public static final int OUT_QUEUE_MAX = 100;
    
    // The input and output workers share one socket, which is owned by the
    // class. We only expose the input and output streams to them.
    // @ToDo: Ugh. Rework this.
    private static final Object socketLock = new Object();
    private static Socket socket;
    private static ServerSocket server;
    private static InputStream input;
    private static OutputStream output;
    public static final int SERVER_PORT = 6000;
    
    private static void initSocket() throws IOException {
        synchronized (socketLock) {
            if (server == null) {
                try {
                    Log.d(TAG, "About to open server socket.");
                    server = new ServerSocket(SERVER_PORT);
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
    
    /** FIFO for passing messages to another thread. */
    protected final LinkedBlockingDeque<ArduinoMessage> queue = new LinkedBlockingDeque<ArduinoMessage>();
    /** Pool of messages available for use.
     *  @ToDo: Does LinkedBlockingQueue allocate a new node when an element
     *  is added, or does it keep a pool? If it's creating objects, switch to
     *  an ad hoc linked list with a next field, as does Android's Message. */
    protected final LinkedBlockingQueue<ArduinoMessage> pool = new LinkedBlockingQueue<ArduinoMessage>();
    /** Count of message objects created. */
    protected int count = 0;
    /** Max allowed message objects for this queue. */
    protected int max = 0;
    
    public ArduinoComm(int max) throws IOException {
        this.max = max;
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
    protected OutputStream getOutputstream() {
        return output;
    }
    
    /** Get an empty ArduinoMessage. */
    public ArduinoMessage obtain() {
        Log.d(TAG, "In obtain.");
        ArduinoMessage message = null;
        synchronized (pool) {
            message = pool.poll();
            if (message == null && count < max) {
                message = new ArduinoMessage();
                ++count;
                Log.d(TAG, "Made a new message, count is " + count);
            }
            while (message == null) {
                // We're run out of messages and hit the max -- wait for one to
                // be returned.
                try {
                    Log.d(TAG, "At max count of messages, waiting for a return.");
                    message = pool.take();
                } catch (InterruptedException e) { }
            }
            Log.d(TAG, "Have an available message.");
        }
        return message;
    }
    
    /** Return a spent ArduinoMessage. */
    public void giveback(ArduinoMessage empty) {
        Log.d(TAG, "In giveback.");
        while (true) {
            try {
                pool.put(empty);
            } catch (InterruptedException e) { }
        }
    }
    
    /** Read the next message from the queue. If none is available, this
     *  blocks until a message arrives. */
    public ArduinoMessage read() {
        Log.d(TAG, "In read.");
        while (true) {
            try {
                return queue.takeFirst();
            } catch (InterruptedException e) { }
        }
    }
    
    /** Read the next message from the queue. If none is currently available,
     *  return null. */ 
    public ArduinoMessage poll() {
        Log.d(TAG, "In poll.");
        return queue.pollFirst();
    }
    
    /** Send a message. */
    public void send(ArduinoMessage m) {
        Log.d(TAG, "In send.");
        
        // Just for testing, strip off and give back anything found in the
        // queue so it doesn't grow.
        ArduinoMessage x = null;
        do {
            x = poll();
            if (x != null) {
                String xo = (x.obj != null) ? x.obj.toString() : "null";
                Log.d(TAG, "Removed queued message with obj: " + xo);
                giveback(x);
            }
        } while (x != null);
        
        while (true) {
            try {
                queue.putLast(m);
            } catch (InterruptedException e) { }
        }
    }
    
    /** Helper that converts two bytes to an int. */
    protected int composeInt(int hi, int lo) {
        int val = hi & 0xff;
        val *= 256;
        val += lo & 0xff;
        return val;
    }
}
