package com.example.a5;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static int HR_ARR_LEN = 50;
    public static int MAX_HR = 200;
    public static int SAMPLE_GENERATE_RATE = 350;

    private boolean isRunning = false;
    private float[] hrValues;
    private int hrCurrIdx = 0;
    GraphView graph;
    GraphView graphY;
    GraphView graphZ;
    int offset = 0;
    private Patient currPatient;
    private EditText idEditText;
    private EditText nameEditText;
    private EditText ageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        hrValues = new float[HR_ARR_LEN];
        hrCurrIdx = 0;

        idEditText = (EditText) findViewById(R.id.idTxtView);
        ageEditText = (EditText) findViewById(R.id.ageTxtView);
        nameEditText = (EditText) findViewById(R.id.nameTxtView);


        /*try {
            SQLiteDatabase db;
            //db = SQLiteDatabase.openOrCreateDatabase(getDatabasePath("lalwanidb.sqlite"), null);
            //File dbfile = new File(Environment.getExternalStorageDirectory().getPath()+"/databaseFolder/mydb.sqlite" );
            //db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory().getPath()+"/mydb.sqlite", null);
            //File externalFilesDir = getExternalFilesDir(null);
            //if(externalFilesDir == null) {
             //   return;
            //}

            //File dbFile = new File(externalFilesDir, "mydb.sqlite");

            db = SQLiteDatabase.openOrCreateDatabase(getExternalFilesDir(null).getAbsolutePath() + "/mydb.sqlite", null);
            db.beginTransaction();
            try {
                //perform your database operations here ...
                db.execSQL("create table "+ "table"+" ("
                        + " timestamp int, "
                        + " x int, "
                        + " y int, "
                        + " z int); ");

                db.setTransactionSuccessful(); //commit your changes
            } catch (SQLiteException e) {
                //report problem
            } finally {
                db.endTransaction();
            }
        } catch (SQLException e) {
            Log.e("DB", e.getMessage());
            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }*/

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
        //upload db button listener
        final Button dbUploadButton = (Button) findViewById(R.id.button_upload_to_db);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stopGraph(v);
            }
        });

        //download db button listener
        final Button dbDownloadButton = (Button) findViewById(R.id.button_download_from_db);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stopGraph(v);
            }
        });

        // Set maximum x and y axis values for Graph 1
        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setMaxY(MAX_HR + 50);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxX(HR_ARR_LEN);
        graph.getViewport().setXAxisBoundsManual(true);

        // Set maximum x and y axis values for Graph 2
        graphY = (GraphView) findViewById(R.id.graphY);
        graphY.getViewport().setMaxY(MAX_HR + 50);
        graphY.getViewport().setYAxisBoundsManual(true);
        graphY.getViewport().setMaxX(HR_ARR_LEN);
        graphY.getViewport().setXAxisBoundsManual(true);

        // Set maximum x and y axis values for Graph 3
        graphZ = (GraphView) findViewById(R.id.graphZ);
        graphZ.getViewport().setMaxY(MAX_HR + 50);
        graphZ.getViewport().setYAxisBoundsManual(true);
        graphZ.getViewport().setMaxX(HR_ARR_LEN);
        graphZ.getViewport().setXAxisBoundsManual(true);

        // Set Axis Labels
        graph.getGridLabelRenderer().setHorizontalAxisTitle("\nTime (sec)");
        graph.getGridLabelRenderer().setVerticalAxisTitle("BPM");

        //Axis Lavbels for Graph 2 (Timestamp vs Y-axis)
        graphY.getGridLabelRenderer().setHorizontalAxisTitle("\nTime (sec)");
        graphY.getGridLabelRenderer().setVerticalAxisTitle("BPM");

        //Axis Lavbels for Graph 3 (Timestamp vs Z-axis)
        graphZ.getGridLabelRenderer().setHorizontalAxisTitle("\nTime (sec)");
        graphZ.getGridLabelRenderer().setVerticalAxisTitle("BPM");
    }

    public void startGraph(View view) {
        String name = nameEditText.getText().toString();
        int age = Integer.parseInt(ageEditText.getText().toString());
        int id = Integer.parseInt(idEditText.getText().toString());
        currPatient = new Patient(name, id, age, "M", getExternalFilesDir(null).getAbsolutePath());
        isRunning = true;

    }

    public void stopGraph(View view) {
        int timestamps[] = new int[hrCurrIdx];
        for(int i=0; i< hrCurrIdx; i++)
            timestamps[i] = i;
        currPatient.addSamples(timestamps, hrValues, hrValues, hrValues);

        isRunning = false;
        hrCurrIdx = 0;
        offset = 0;
        graph.removeAllSeries();
        graphY.removeAllSeries();
        graphZ.removeAllSeries();
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
        }
    }; //handler
}
