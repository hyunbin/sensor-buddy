package com.hyunbinpark.sensorbuddy;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.FileWriter;

import java.util.Arrays;

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
                registerSensors();
            }
        });
        mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unregisterSensors();
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

    private void registerSensors(){
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mMag, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unregisterSensors(){
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    public void onSensorChanged(SensorEvent event){
        if(event.sensor == mAccel){
            mAccelText.setText(Arrays.toString(event.values));
        } else if(event.sensor == mGyro){
            mGyroText.setText(Arrays.toString(event.values));
        } else if(event.sensor == mMag) {
            mMagText.setText(Arrays.toString(event.values));
        } else if(event.sensor == mLight){
            mLightText.setText(Arrays.toString(event.values));
        }
    }

    public void writetoCSV(String sensor, float x, float y, float z, float lightSensor) throws Exception {

        String csv = "output.csv";
        CSVWriter writer = new CSVWriter(new FileWriter(csv));

        String [] country = "India#China#United States".split("#");

        switch (sensor) {
            case "a":
                
                break;
            case "g":

                break;
            case "m":

                break;
            default:
                break;
        }


        writer.writeNext(country);

        writer.close();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "Accuracy changed: " + sensor.getName() + " , " + i);
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
