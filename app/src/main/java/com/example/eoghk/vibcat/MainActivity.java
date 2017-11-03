package com.example.eoghk.vibcat;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import umich.cse.yctung.androidlibsvm.LibSVM;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sm;
    Sensor accSensor;
    TextView textView;
    TextView result_tv;
    Vibrator vide;
    boolean isVibrating;
    String result, learning_data;
    EditText et;
    int index = 0;
    float []  arr = new float [96];
    String systemPath = Environment.getExternalStorageDirectory() + "/";
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.text);
        et = (EditText)findViewById(R.id.test_num);
        result_tv = (TextView)findViewById(R.id.result);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sm.registerListener(this,accSensor,SensorManager.SENSOR_DELAY_FASTEST);

        vide = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0]*20;
        float y = event.values[1]*20;
        float z = event.values[2]*20;
        String str = "x: "+x+"\n"+"y: "+y+"\n"+"z: "+z;
        textView.setText(str);
        if (isVibrating && index <96) {
            result += z + "\n";
            //learning_data += " " + index + ":" + z;
            //learning_data += "," + z;
            arr[index] = z;
            index++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void Vibrate (View v){
        if (et.getText().toString().equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(),"Please type test number", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        vide.vibrate(1000);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                result = "";
                learning_data = et.getText().toString();
                isVibrating = true;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                isVibrating = false;
                index = 0;

                //write in SD card
                String SDCARD_FILE_PATH = "/sdcard/study/";
                String filename = "VibCatData.txt";
                Arrays.sort(arr);

                for (int i = 0; i<96;i++){
                    learning_data += " "+ i + ":" + arr[i];
                }

                try {
                    FileWriter fw = new FileWriter(SDCARD_FILE_PATH + filename,true);
                    PrintWriter writer=new PrintWriter(fw, true);
                    writer.write(learning_data+"\n");
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void TestIt(View v){
        vide.vibrate(1000);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                result = "";
                learning_data = "0";
                isVibrating = true;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                isVibrating = false;
                index = 0;

                //write in SD card
                String SDCARD_FILE_PATH = "/sdcard/study/";
                String filename = "test_file";
                Arrays.sort(arr);

                for (int i = 0; i<96;i++){
                    learning_data += " "+ i + ":" + arr[i];
                }

                try {
                    FileWriter fw = new FileWriter(SDCARD_FILE_PATH + filename,false);
                    PrintWriter writer=new PrintWriter(fw, false);
                    writer.write(learning_data+"\n");
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                List<String> commands = new ArrayList<>();
                commands.add("-b");
                commands.add(Integer.toString(0));
                String testFilePicker = systemPath+"study/test_file";
                commands.add(testFilePicker);
                String modelFilePicker = systemPath+"study/VibCat.model";
                commands.add(modelFilePicker);
                commands.add(systemPath+"study/result_file");
                new getResult().execute(commands.toArray(new String[0]));

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // This gets executed on the UI thread so it can safely modify Views
                        result_tv.setText(objClassifier());
                    }
                });

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

    private class getResult extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            LibSVM.getInstance().predict(TextUtils.join(" ", params));
            return null;
        }
    }

    private  String objClassifier(){
        String r = "";
        File file = new File(systemPath+"study/result_file");
        FileReader fr = null ;

        int value=0;

        try {
            fr = new FileReader(file) ;
            value = fr.read()-48;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("hello: "+value);
        switch(value){
            case 0:
                r = "Desk";
                break;
            case 1:
                r = "Hand";
                break;
            /*case 2:
                r = "Grab Hand";
                break;
            case 3:
                r = "Floor";
                break;*/
            default:
                r = "Not Classified";
        }

        return r;
    }
}
