package com.example.a5;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
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

        // Add Start button onClick listener
        final Button startButton = (Button) findViewById(R.id.startButtonID);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGraph(v);
            }
        });

        // Add Stop button onClick listener
        final Button stopButton = (Button) findViewById(R.id.stopButtonID);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopGraph(v);
            }
        });

        // Set maximum x and y axis values
        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setMaxY(MAX_HR + 50);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxX(HR_ARR_LEN);
        graph.getViewport().setXAxisBoundsManual(true);

        // Set Axis Labels
        graph.getGridLabelRenderer().setHorizontalAxisTitle("\nTime (sec)");
        graph.getGridLabelRenderer().setVerticalAxisTitle("BPM");

        debugTextView = (TextView)findViewById(R.id.debugText);
    }

    public void startGraph(View view) {
        isRunning = true;
    }

    public void stopGraph(View view) {
        isRunning = false;
        hrCurrIdx = 0;
        offset = 0;
        graph.removeAllSeries();
    }

    @Override
    public void onStart() {
        super.onStart();
        Thread background = new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        if(isRunning) {
                            float newHR;
                            Thread.sleep(SAMPLE_GENERATE_RATE);
                            // Generate new sample
                            Random rnd = new Random();

                            // Add new sample to hrValues array
                            newHR = (float) rnd.nextInt(MAX_HR);
                            if (hrCurrIdx < HR_ARR_LEN)
                                hrValues[hrCurrIdx++] = newHR;
                            else {
                                // Array is full. Shift it
                                for (int i = 0; i < HR_ARR_LEN - 1; i++)
                                    hrValues[i] = hrValues[i + 1];
                                hrValues[HR_ARR_LEN - 1] = newHR;
                            }

                            // Send message to the UI activity
                            Message msg = handler.obtainMessage(1, null);
                            handler.sendMessage(msg);
                        }
                    }
                } catch (Throwable t) {
                }
            }//run
        });//background
        //isRunning = true;
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

            // Clear the graph
            graph.removeAllSeries();
            // Build new series for the updated hrValues
            for(int i=0; i<hrCurrIdx; i++)
                series.appendData(new DataPoint(i+offset, hrValues[i]), true, HR_ARR_LEN);
            // If the array is full, shift the x-axis start offset
            if(hrCurrIdx == HR_ARR_LEN)
                offset++;
            // Update the X axis range
            graph.getViewport().setMinX(offset);
            graph.getViewport().setMaxX(offset + HR_ARR_LEN);
            // Add the new series to the graph
            graph.addSeries(series);


            String text = new String();
            for(int i = 0; i< hrValues.length; i++)
                text += String.format("%02d", (int)hrValues[i]) + " ";
            debugTextView.setText(text);
        }
    }; //handler
}
