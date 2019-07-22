package com.example.a5;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static int HR_ARR_LEN = 10;
    public static int MAX_HR = 100;

    public TextView debugTextView;

    private boolean isRunning = false;
    private float[] hrValues;
    private int hrCurrIdx = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        hrValues = new float[HR_ARR_LEN];
        hrCurrIdx = 0;
        debugTextView = (TextView)findViewById(R.id.debugText);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        Thread background = new Thread(new Runnable() {
            public void run() {
                try {
                    while(isRunning) {
                        float newHR;
                        Thread.sleep(1000); //one second at a time
                        Random rnd = new Random();

                        newHR = (float)rnd.nextInt(MAX_HR);
                        if(hrCurrIdx < HR_ARR_LEN)
                            hrValues[hrCurrIdx++] = newHR;
                        else {
                            // Array is full. Shift it
                            for(int i=0; i<HR_ARR_LEN-1; i++)
                                hrValues[i] = hrValues[i+1];
                            hrValues[HR_ARR_LEN - 1] = newHR;
                        }

                        Message msg = handler.obtainMessage(1, null);

                        if (isRunning) {
                            handler.sendMessage(msg);
                        }
                    }
                } catch (Throwable t) {
                }
            }//run
        });//background
        isRunning = true;
        background.start();
    }//onStart

    @Override
    public void onStop() {
        super.onStop();
        isRunning = false;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String text = new String();
            for(int i = 0; i< hrValues.length; i++)
                text += String.format("%02d", (int)hrValues[i]) + " ";
            debugTextView.setText(text);
        }
    }; //handler
}
