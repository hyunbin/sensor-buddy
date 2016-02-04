package com.hyunbinpark.sensorbuddy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static String TAG = MainActivity.class.getSimpleName();

    private Button mStartButton;
    private Button mStopButton;
    private TextView mAccelText;
    private TextView mGyroText;
    private TextView mMagText;
    private TextView mLightText;

    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mGyro;
    private Sensor mMag;
    private Sensor mLight;

    private long mStartTime;

    private File mFile;
    private CSVWriter mCSVWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Initialize button logic
        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginCollection();
            }
        });
        mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopCollection();
            }
        });

        // Text logic
        mAccelText = (TextView) findViewById(R.id.accel_text);
        mGyroText = (TextView) findViewById(R.id.gyro_text);
        mMagText = (TextView) findViewById(R.id.mag_text);
        mLightText = (TextView) findViewById(R.id.light_text);

        // Initialize sensor-related objects
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    private void beginCollection(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request write permissions because it hasn't been granted yet
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else{
            // Set start time of data collection
            mStartTime = SystemClock.uptimeMillis();

            // Initialize file IO because permissions have been granted
            String fileName = Calendar.getInstance().getTime().toString();
            if(isExternalStorageWritable()){
                mFile = new File(Environment.getExternalStorageDirectory(), fileName + ".csv");
                // mFile.mkdirs();
                initializeCsvReader();
            }
            // Start sensor data listening
            mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mMag, SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void initializeCsvReader(){
        try {
            mCSVWriter = new CSVWriter(new FileWriter(mFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String [] columnNames = "Time#Accel_x#Accel_y#Accel_z#Gyro_x#Gyro_y#Gyro_z#Mag_x#Mag_y#Mag_z#Light".split("#");
        mCSVWriter.writeNext(columnNames);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        beginCollection(); // This is a hacky way to always grant file IO access, can't ever deny
    }

    private void stopCollection() {
        mSensorManager.unregisterListener(this);
        try {
            mCSVWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    public void onSensorChanged(SensorEvent event){
        long curTime = TimeUnit.NANOSECONDS.toMillis(event.timestamp) - mStartTime;
        if(event.sensor == mAccel){
            mAccelText.setText(Arrays.toString(event.values));
            writeAccel(curTime, event.values);
        } else if(event.sensor == mGyro){
            mGyroText.setText(Arrays.toString(event.values));
            writeGyro(curTime, event.values);
        } else if(event.sensor == mMag) {
            mMagText.setText(Arrays.toString(event.values));
            writeMag(curTime, event.values);
        } else if(event.sensor == mLight){
            mLightText.setText(Arrays.toString(event.values));
            writeLight(curTime, event.values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "Accuracy changed: " + sensor.getName() + " , " + i);
    }

    private void writeAccel(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[1] = Float.toString(value[0]);
        array[2] = Float.toString(value[1]);
        array[3] = Float.toString(value[2]);
        mCSVWriter.writeNext(array);
    }

    private void writeGyro(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[4] = Float.toString(value[0]);
        array[5] = Float.toString(value[1]);
        array[6] = Float.toString(value[2]);
        mCSVWriter.writeNext(array);
    }

    private void writeMag(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[7] = Float.toString(value[0]);
        array[8] = Float.toString(value[1]);
        array[9] = Float.toString(value[2]);
        mCSVWriter.writeNext(array);
    }

    private void writeLight(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[10] = Float.toString(value[0]);
        mCSVWriter.writeNext(array);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
}
