package com.example.a5;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int EXTERNAL_STORAGE_CODE = 100;
    public static int HR_ARR_LEN = 10;
    public static int MAX_HR = 20;
    public static int SAMPLE_GENERATE_RATE = 1000000;
    long lastSampleTime = 0;

    String dbFilePath;
    final String dbFileName = "lalwani.sqlite";
    final String serverIP = "192.168.43.35";
    final String serverURL = "http://"+serverIP + "/UploadToServer.php";
    final String serverDownladURL = "http://"+serverIP + "/uploads/";

    private boolean isRunning = false;
    GraphView graphX;
    GraphView graphY;
    GraphView graphZ;
    int offset = 0;
    private Patient currPatient;
    private EditText idEditText;
    private EditText nameEditText;
    private EditText ageEditText;
    private RadioGroup sexRdoGrp;
    
    private SensorManager accelManage;
    private Sensor senseAccel;
    float accelValuesX[] = new float[HR_ARR_LEN];
    float accelValuesY[] = new float[HR_ARR_LEN];
    float accelValuesZ[] = new float[HR_ARR_LEN];
    long timestamps[] = new long[HR_ARR_LEN];
    int index = 0;
    int k=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbFilePath = getExternalFilesDir(null).getAbsolutePath()+"/";

        idEditText = (EditText) findViewById(R.id.idTxtView);
        ageEditText = (EditText) findViewById(R.id.ageTxtView);
        nameEditText = (EditText) findViewById(R.id.nameTxtView);
        sexRdoGrp = (RadioGroup) findViewById(R.id.sexRdoGrp);

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
        dbUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadDB(v);
            }
        });

        //download db button listener
        final Button dbDownloadButton = (Button) findViewById(R.id.button_download_from_db);
        dbDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    //permission is denied, request it.
                    String [] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    requestPermissions(permissions, EXTERNAL_STORAGE_CODE);
                }
                else {
                    //permission granted
                    doDownload();
                }
            }
            else{
                doDownload();
            }
            loadPatient();
            }
        });


        // Set maximum x and y axis values for Graph 1

        graphX = (GraphView) findViewById(R.id.graphX);
        graphX.getViewport().setMaxY(MAX_HR);
        graphX.getViewport().setMinY(-1*(MAX_HR));
        graphX.getViewport().setYAxisBoundsManual(true);
        graphX.getViewport().setMaxX(HR_ARR_LEN);
        graphX.getViewport().setXAxisBoundsManual(true);

        // Set maximum x and y axis values for Graph 2
        graphY = (GraphView) findViewById(R.id.graphY);
        graphY.getViewport().setMaxY(MAX_HR);
        graphY.getViewport().setMinY(-1*(MAX_HR));
        graphY.getViewport().setYAxisBoundsManual(true);
        graphY.getViewport().setMaxX(HR_ARR_LEN);
        graphY.getViewport().setXAxisBoundsManual(true);

        // Set maximum x and y axis values for Graph 3
        graphZ = (GraphView) findViewById(R.id.graphZ);
        graphZ.getViewport().setMaxY(MAX_HR);
        graphZ.getViewport().setMinY(-1*(MAX_HR));
        graphZ.getViewport().setYAxisBoundsManual(true);
        graphZ.getViewport().setMaxX(HR_ARR_LEN);
        graphZ.getViewport().setXAxisBoundsManual(true);

        // Set Axis Labels
        graphX.getGridLabelRenderer().setHorizontalAxisTitle("\nTime (sec)");
        graphX.getGridLabelRenderer().setVerticalAxisTitle("X-values");

        //Axis Lavbels for Graph 2 (Timestamp vs Y-axis)
        graphY.getGridLabelRenderer().setHorizontalAxisTitle("\nTime (sec)");
        graphY.getGridLabelRenderer().setVerticalAxisTitle("Y-values");

        //Axis Lavbels for Graph 3 (Timestamp vs Z-axis)
        graphZ.getGridLabelRenderer().setHorizontalAxisTitle("\nTime (sec)");
        graphZ.getGridLabelRenderer().setVerticalAxisTitle("Z-values");
        
        accelManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        senseAccel = accelManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void doDownload() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/"+dbFileName);
        boolean deleted = file.delete();

//        String url = "https://images.all-free-download.com/images/graphiclarge/hd_picture_of_the_beautiful_natural_scenery_03_166249.jpg";
        String url = serverDownladURL + dbFileName;
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE |
                DownloadManager.Request.NETWORK_WIFI);
        request.allowScanningByMediaScanner()   ;
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, dbFileName);

        //request.setDestinationInExternalPublicDir(dbFilePath, dbFileName);

        DownloadManager manager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startGraph(View view) {
        if(nameEditText.getText().length() == 0 ||
                ageEditText.getText().length() == 0 ||
                idEditText.getText().length() == 0
        ) {
            Toast.makeText(MainActivity.this, "Please fill patient data",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String name = nameEditText.getText().toString();
        int age = Integer.parseInt(ageEditText.getText().toString());
        int id = Integer.parseInt(idEditText.getText().toString());
        String sex = ((RadioButton)findViewById(sexRdoGrp.getCheckedRadioButtonId()))
                        .getText().toString();

        currPatient = new Patient(name, id, age, sex, dbFilePath);
        //isRunning = true;
        
        accelManage.registerListener(MainActivity.this, senseAccel, /*accelManage.SENSOR_DELAY_NORMAL*/SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void stopGraph(View view) {
        accelManage.unregisterListener(this);
        currPatient.addSamples(timestamps, accelValuesX, accelValuesY, accelValuesZ);

        isRunning = false;
        index = 0;
        offset = 0;

        graphX.removeAllSeries();
        graphY.removeAllSeries();
        graphZ.removeAllSeries();
    }

    private void uploadDB(View view){
        /* dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);*/

        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        //  messageText.setText("uploading started.....");
                    }
                });

                ServerConnection.Upload(dbFileName, dbFilePath, serverURL);

            }
        }).start();
    }

    public void loadPatient() {
        if(nameEditText.getText().length() == 0 ||
                ageEditText.getText().length() == 0 ||
                idEditText.getText().length() == 0
        ) {
            Toast.makeText(MainActivity.this, "Please fill patient data", Toast.LENGTH_LONG).show();
            return;
        }

        String name = nameEditText.getText().toString();
        int age = Integer.parseInt(ageEditText.getText().toString());
        int id = Integer.parseInt(idEditText.getText().toString());
        String sex = ((RadioButton)findViewById(sexRdoGrp.getCheckedRadioButtonId()))
                .getText().toString();
        currPatient = new Patient(name, id, age, sex, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/");

        //Show the patient Data from DB on Graph.
        // Clear the graph, first...
        graphX.removeAllSeries();
        graphY.removeAllSeries();
        graphZ.removeAllSeries();

        Patient.PatientData patientData[] = currPatient.loadPatientData();
        int patientDataLength = patientData.length;
        DataPoint[] dataPointsX = new DataPoint[patientDataLength];
        DataPoint[] dataPointsY = new DataPoint[patientDataLength];
        DataPoint[] dataPointsZ = new DataPoint[patientDataLength];

        for (int i = 0; i < patientDataLength; i++) {
            dataPointsX[i] = new DataPoint(i,patientData[i].x);
            dataPointsY[i] = new DataPoint(i,patientData[i].y);
            dataPointsZ[i] = new DataPoint(i,patientData[i].z);
        }
        LineGraphSeries<DataPoint> seriesTempX = new LineGraphSeries<DataPoint>(dataPointsX);
        LineGraphSeries<DataPoint> seriesTempY = new LineGraphSeries<DataPoint>(dataPointsY);
        LineGraphSeries<DataPoint> seriesTempZ = new LineGraphSeries<DataPoint>(dataPointsZ);

        graphX.addSeries(seriesTempX);
        graphY.addSeries(seriesTempY);
        graphZ.addSeries(seriesTempZ);
    }

    @Override
    public void onStop() {
        super.onStop();
        isRunning = false;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            LineGraphSeries<DataPoint> seriesX = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesY = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesZ = new LineGraphSeries<>();

            // Clear the graph
            graphX.removeAllSeries();
            graphY.removeAllSeries();
            graphZ.removeAllSeries();
            
            // Build new series for the updated hrValues
            for(int i=0; i<index; i++) {
                seriesX.appendData(new DataPoint(i + offset, accelValuesX[i]), true, HR_ARR_LEN);
                seriesY.appendData(new DataPoint(i + offset, accelValuesY[i]), true, HR_ARR_LEN);
                seriesZ.appendData(new DataPoint(i + offset, accelValuesZ[i]), true, HR_ARR_LEN);
            }

            // Update the X axis range
            graphX.getViewport().setMinX(offset);
            graphX.getViewport().setMaxX(offset + HR_ARR_LEN);
            // Add the new series to the graph
            graphX.addSeries(seriesX);
            
            graphY.getViewport().setMinX(offset);
            graphY.getViewport().setMaxX(offset + HR_ARR_LEN);
            // Add the new series to the graph
            graphY.addSeries(seriesY);
            
            graphZ.getViewport().setMinX(offset);
            graphZ.getViewport().setMaxX(offset + HR_ARR_LEN);
            // Add the new series to the graph
            graphZ.addSeries(seriesZ);

            // If the array is full, shift the x-axis start offset
            if(index >= HR_ARR_LEN)
                offset++;
        }
    }; //handler
    
    public void onSensorChanged(SensorEvent sensorEvent) {
        // TODO Auto-generated method stub
        Sensor mySensor = sensorEvent.sensor;
        long currTimestampsys = System.currentTimeMillis();
        long currTimestamp = sensorEvent.timestamp;

        if (currTimestampsys - lastSampleTime < 1000)
            return;

        lastSampleTime = currTimestampsys;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if(index < HR_ARR_LEN) {
                accelValuesX[index] = sensorEvent.values[0];
                accelValuesY[index] = sensorEvent.values[1];
                accelValuesZ[index] = sensorEvent.values[2];
                timestamps[index] = currTimestamp;
                index++;
            }
            else{
                for (int i = 0; i < HR_ARR_LEN - 1; i++){
                    accelValuesX[i] = accelValuesX[i+1];
                    accelValuesY[i] = accelValuesY[i+1];
                    accelValuesZ[i] = accelValuesZ[i+1];
                    timestamps[i] = timestamps[i+1];
                }
                accelValuesX[index-1] = sensorEvent.values[0];
                accelValuesY[index-1] = sensorEvent.values[1];
                accelValuesZ[index-1] = sensorEvent.values[2];
                timestamps[index-1] = currTimestamp;
            }
        }

        Message msg = handler.obtainMessage(1, null);
        handler.sendMessage(msg);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case EXTERNAL_STORAGE_CODE:{
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    doDownload();
                }
                else{
                    Toast.makeText(this,"Permission Denied..!!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

