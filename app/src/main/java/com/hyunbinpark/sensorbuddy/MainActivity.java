package com.hyunbinpark.sensorbuddy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Debug;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static java.lang.Math.toDegrees;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static String TAG = MainActivity.class.getSimpleName();

    private Button mStartButton;
    private Button mStopButton;
    private Button mYoloButton;
    private TextView mAccelText;
    private TextView mGyroText;
    private TextView mMagText;
    private TextView mLightText;
    private TextView mStepText;
    private TextView mTotalRotationText;
    private TextView mTotalDistanceText;

    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mGyro;
    private Sensor mMag;
    private Sensor mLight;

    private long mStartTime;

    private File mFile;
    private CSVWriter mCsvWriter;

    private ArrayList<Float> mAccelYData;
    private ArrayList<Long> mAccelTimeData;
    private boolean mFirstTimeWriting = true;

    private float[] smoothedData = new float[3];
    private float[] previousData = new float[3];
    private float ALPHA = 0.038f; // 0.04f
    private int mZeroCrossing = 0;
    private float mGravityCompensation = 9.82f; // 9.81f

    private float timestamp = 0; // Used for integration of angle
    private static final float NS2S = 1.0f / 1000000000.0f;

    private double mTotalRotation = 0;
    private double mMaxVal;
    private boolean mMoving = true;

    private double mStride = 0.415 * 1.75; // in meters

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
        mYoloButton = (Button) findViewById(R.id.rotate_button);
        mYoloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMoving = !mMoving;
            }
        });

        // Text logic
        mAccelText = (TextView) findViewById(R.id.accel_text);
        mGyroText = (TextView) findViewById(R.id.gyro_text);
        mMagText = (TextView) findViewById(R.id.mag_text);
        mLightText = (TextView) findViewById(R.id.light_text);
        mStepText = (TextView) findViewById(R.id.step_text);
        mTotalRotationText = (TextView) findViewById(R.id.rotation_text);
        mTotalDistanceText = (TextView) findViewById(R.id.distance_text);

        // Initialize sensor-related objects
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        mAccelYData = new ArrayList<>();
        mAccelTimeData = new ArrayList<>();
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
                Log.d(TAG, mFile.toString());
                // mFile.mkdirs();
                initializeCsvReader();
            }
            mZeroCrossing = 0; // set step counting to 0
            mTotalRotation = 0; // reset rotation
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
            mCsvWriter = new CSVWriter(new FileWriter(mFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String [] columnNames = "Time#Accel_x#Accel_y#Accel_z#Gyro_x#Gyro_y#Gyro_z#Mag_x#Mag_y#Mag_z#Light".split("#");
        mCsvWriter.writeNext(columnNames);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        beginCollection(); // This is a hacky way to always grant file IO access, can't ever deny
    }

    private void stopCollection() {
        mSensorManager.unregisterListener(this);
        mFirstTimeWriting = true;
        mStepText.setText("" + mZeroCrossing / 2);
        try {
            mCsvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        if(output[2] - mGravityCompensation > mMaxVal)
            mMaxVal = output[2] - mGravityCompensation;
        /*
        if(abs(output[1] - input[1]) > 0.2){
            Log.d(TAG, "tru");
            mMoving = true;
        }
        else{
            Log.d(TAG, "nah");
            mMoving = false;
        }
        */
        //Log.e(TAG, "" + (input[0] - output[0]));
        return output;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    public void onSensorChanged(SensorEvent event){
        long curTime;
        if(mFirstTimeWriting) {
            mStartTime = TimeUnit.NANOSECONDS.toMillis(event.timestamp);
            curTime = 0;
            mFirstTimeWriting = false;
        }
        else{
            curTime = TimeUnit.NANOSECONDS.toMillis(event.timestamp) - mStartTime;
        }
        if (event.sensor == mAccel) {
            mAccelText.setText(Arrays.toString(event.values));
            mAccelYData.add(event.values[1]);
            mAccelTimeData.add(curTime);
            writeAccel(curTime, event.values);
            previousData = smoothedData.clone();
            smoothedData = lowPass(event.values.clone(), smoothedData);
            checkZeroCrossing(previousData[2] - mGravityCompensation, smoothedData[2] - mGravityCompensation); // TODO
            mStepText.setText("" + mZeroCrossing / 2);
            mTotalDistanceText.setText((mZeroCrossing / 2 * mStride) + " meters");
        } else if(event.sensor == mGyro){
            mGyroText.setText(Arrays.toString(event.values));
            writeGyro(curTime, event.values);
            calculateIntegration(event);
            mTotalRotationText.setText(Double.toString(mTotalRotation) + " degrees");
        } else if(event.sensor == mMag) {
            mMagText.setText(Arrays.toString(event.values));
            writeMag(curTime, event.values);
        } else if(event.sensor == mLight){
            mLightText.setText(Arrays.toString(event.values));
            writeLight(curTime, event.values);
        }
    }

    private void calculateIntegration(SensorEvent event) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if(mMoving){
            timestamp = event.timestamp;
            return;
        }
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample
            float axisZ = abs(event.values[2]);
            double axisZDeg = toDegrees(axisZ);
            if(axisZDeg > 2) {
                mTotalRotation += axisZDeg * dT;
            }
        }
        timestamp = event.timestamp;
    }

    private void checkZeroCrossing(float previousData, float currentData){
        if((previousData > 0 && currentData < 0) || (previousData < 0 && currentData > 0)){
            if(mMaxVal > 0.04) { // TODO: Play around with threshold to find better value
                mZeroCrossing++;
                if (mZeroCrossing % 2 == 0) {
                    Log.d(TAG, "Maxval for step " + (mZeroCrossing / 2) + ": " + mMaxVal);
                    mMaxVal = 0;
                }
            }
            else{
                Log.e(TAG, "Rejected for maxval: " + mMaxVal);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Log.d(TAG, "Accuracy changed: " + sensor.getName() + " , " + i);
    }

    private void writeAccel(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[1] = Float.toString(value[0]);
        array[2] = Float.toString(value[1]);
        array[3] = Float.toString(value[2]);
        mCsvWriter.writeNext(array);
    }

    private void writeGyro(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[4] = Float.toString(value[0]);
        array[5] = Float.toString(value[1]);
        array[6] = Float.toString(value[2]);
        mCsvWriter.writeNext(array);
    }

    private void writeMag(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[7] = Float.toString(value[0]);
        array[8] = Float.toString(value[1]);
        array[9] = Float.toString(value[2]);
        mCsvWriter.writeNext(array);
    }

    private void writeLight(long time, float[] value){
        String[] array = new String[11];
        array[0] = Long.toString(time);
        array[10] = Float.toString(value[0]);
        mCsvWriter.writeNext(array);
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
