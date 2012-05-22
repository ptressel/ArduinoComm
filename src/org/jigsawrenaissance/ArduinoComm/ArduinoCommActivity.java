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
        
        // @Debug: Comment out ArduinoComm.
        ArduinoComm comm = null;
        try {
            comm = new ArduinoComm();
        } catch (IOException e) {
            Log.e(TAG, "Could not set up communication with the Arduino", e);
        }
        
        // @Debug: For testing with MockInputStream, comment out the first
        // line ArduinoIn(comm), and uncomment the following with ArduinoIn().
        ArduinoIn in = new ArduinoIn(comm);
        //ArduinoIn in = new ArduinoIn();
        Log.d(TAG, "ArduinoIn create succeeded");
        Thread inThread = new Thread(null, in, ArduinoIn.TAG);
        Log.d(TAG, "About to start input worker thread");
        inThread.start();
        
        // @Debug: Comment out starting ArduinoOut.
        ArduinoOut out = new ArduinoOut(comm);
        Log.d(TAG, "ArduinoOut create succeeded");
        Thread outThread = new Thread(null, out, ArduinoOut.TAG);
        Log.d(TAG, "About to start output worker thread");
        outThread.start();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "In onPause");
        // @ToDo: onPause is moot when this moves to a service, but in
        // general, should we close the socket if we're exiting?
    }
}