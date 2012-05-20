package org.jigsawrenaissance.ArduinoComm;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class ArduinoCommActivity extends Activity {
    public static final String TAG = "ArduinoCommActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "In onCreate");
        setContentView(R.layout.main);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "In onResume");
        
        // @ToDo: Move this to a Service.
        try {
            ArduinoIn in = new ArduinoIn(ArduinoIn.IN_QUEUE_MAX);
            Log.d(TAG, "ArduinoIn create succeeded");
            Thread inThread = new Thread(null, in, ArduinoIn.TAG);
            Log.d(TAG, "About to start input worker thread");
            inThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Could not set up input worker");
        }
        
        /*
        try {
            ArduinoOut out = new ArduinoOut(ArduinoOut.OUT_QUEUE_MAX);
            Log.d(TAG, "ArduinoOut create succeeded");
            Thread outThread = new Thread(null, out, ArduinoOut.TAG);
            Log.d(TAG, "About to start output worker thread");
            outThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Could not set up output worker");
        }
        */
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "In onPause");
        // @ToDo: Should we close the socket?
    }
}