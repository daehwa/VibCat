package com.example.eoghk.vibcat;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sm;
    Sensor accSensor;
    TextView textView;
    Vibrator vide;
    boolean isVibrating;
    String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.text);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sm.registerListener(this,accSensor,SensorManager.SENSOR_DELAY_FASTEST);

        vide = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0]*50;
        float y = event.values[1]*50;
        float z = event.values[2]*50;
        String str = "x: "+x+"\n"+"y: "+y+"\n"+"z: "+z;
        textView.setText(str);
        if (isVibrating)
            result +=z+"\n";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void Vibrate (View v){
        vide.vibrate(1000);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                result = "";
                isVibrating = true;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                isVibrating = false;
            }
        }).start();
    }

    public void Send(View v){
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        String text = result;
        intent.putExtra(Intent.EXTRA_TEXT, text);

// Title of intent
        Intent chooser = Intent.createChooser(intent, "친구에게 공유하기");
        startActivity(chooser);
    }
}
