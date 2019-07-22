package com.example.a5;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static int HR_ARR_LEN = 50;
    public static int MAX_HR = 200;
    public static int SAMPLE_GENERATE_RATE = 350;

    public TextView debugTextView;

    private boolean isRunning = false;
    private float[] hrValues;
    private int hrCurrIdx = 0;
    GraphView graph;
    int offset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        hrValues = new float[HR_ARR_LEN];
        hrCurrIdx = 0;

        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setMaxY(MAX_HR + 50);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxX(HR_ARR_LEN);
        graph.getViewport().setXAxisBoundsManual(true);
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
                        Thread.sleep(SAMPLE_GENERATE_RATE); //one second at a time
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
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

            graph.removeAllSeries();
            for(int i=0; i<hrCurrIdx; i++)
                series.appendData(new DataPoint(i+offset, hrValues[i]), true, HR_ARR_LEN);
            if(hrCurrIdx == HR_ARR_LEN)
                offset++;
            graph.getViewport().setMinX(offset);
            graph.getViewport().setMaxX(offset + HR_ARR_LEN);

            graph.addSeries(series);


            String text = new String();
            for(int i = 0; i< hrValues.length; i++)
                text += String.format("%02d", (int)hrValues[i]) + " ";
            debugTextView.setText(text);
        }
    }; //handler
}
