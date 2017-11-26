package com.example.eoghk.vibcat;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import mr.go.sgfilter.ContinuousPadder;
import mr.go.sgfilter.MeanValuePadder;
import mr.go.sgfilter.SGFilter;

public class VibCat extends AppCompatActivity implements SensorEventListener {

    SensorManager sm;
    Sensor accSensor;
    TextView textView;
    TextView result_tv;
    Button vibBtn;
    Vibrator vide;
    boolean isVibrating;
    String result, learning_data;
    EditText et;
    int index = 0;
    final int data_size = 90;
    final int diff = 50;
    float []  arr = new float [data_size];
    String systemPath = Environment.getExternalStorageDirectory() + "/";
    private Handler mHandler = new Handler();

    // machine learning
    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "file:///android_asset/graph.pb";
    private static final String INPUT_NODE = "input_node";
    private static final String OUTPUT_NODE = "hypothesis";
    private static final int[] INPUT_SIZE = {1, 84};

    int train_num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.text);
        et = (EditText)findViewById(R.id.test_num);
        result_tv = (TextView)findViewById(R.id.result);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sm.registerListener(this,accSensor,0,0);

        vide = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        vibBtn = (Button)findViewById(R.id.vib_btn) ;

        initMyModel();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }

    public void Train100(View v){
        if (et.getText().toString().equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(),"Please type test number", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        final Timer timer = new Timer();
        final Handler handler = new Handler(){
            public void handleMessage(Message msg){
                vibBtn.performClick();
                train_num++;
                if (train_num > 99){
                    timer.cancel();
                    train_num = 0;
                }
            }
        };
        //timer.schedule(myTask, 5000);  // 5초후 실행하고 종료
        TimerTask myTask = new TimerTask() {
            public void run() {
                new Thread(){
                    public void run(){
                        Message msg = handler.obtainMessage();
                        handler.sendMessage(msg);
                    }
                }.start();
            }
        };
        timer.schedule(myTask, 1000, 1500); // 1초후 첫실행, 1초마다 계속실행
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0]*20;
        float y = event.values[1]*20;
        float z = event.values[2]*20;
        //String str = "x: "+x+"\n"+"y: "+y+"\n"+"z: "+z;
        //textView.setText(str);
        if (isVibrating && index <data_size) {
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
    final int vibrate_time = 1000;
    public void Vibrate (View v){
        if (et.getText().toString().equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(),"Please type test number", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        vide.vibrate(vibrate_time);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                result = "";
                learning_data = "";
                isVibrating = true;

                try {
                    Thread.sleep(vibrate_time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                isVibrating = false;
                index = 0;

                //write in SD card
                String SDCARD_FILE_PATH = "/sdcard/study/";
                String filename = "VibCatData.csv";
                //Arrays.sort(arr);

                int size = data_size -diff;
                float [] arranged_arr = new float[size];

                arr = Smoothing(arr);

                int start_point = 0;
                float m = arr[0];

                for (int i = 0; i < diff; i++) {
                    if (m < arr[i]){
                        m = arr[i];
                        start_point = i;
                    }
                }

                for (int i = 0;i< size; i++){
                    arranged_arr[i] = arr[i+start_point];
                }

                float [] abs = new float[size];
                float avg=0,min=arranged_arr[0],max=arranged_arr[0],rms=0;
                int index_max=0;

                ;
                for (int i = 0; i<size;i++){
                    if(arranged_arr[i]<=min){ min = arranged_arr[i];}
                    if(arranged_arr[i]>=max){ max = arranged_arr[i]; index_max = i; }
                }
                /*float [] temp = Arrays.copyOf(arranged_arr, arranged_arr.length);
                for (int i= 0 ; i<size; i++){
                    arranged_arr[i] = temp[(index_max+i)%size];
                }*/

                for (int i = 0; i<size;i++){
                    learning_data +=  arranged_arr[i] + ",";
                    avg = arranged_arr[i];
                    rms = arranged_arr[i]*arranged_arr[i];
                    abs[i] = Math.abs(arranged_arr[i]);
                }
                avg = avg / size;
                rms = (float)Math.sqrt(rms/size);

                for (int i=0; i<size;i++){
                    learning_data += abs[i] + ",";
                }
                learning_data += avg + "," + rms + "," + max + "," + min+"," + et.getText().toString();

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
        vide.vibrate(vibrate_time);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                result = "";
                learning_data = "";
                isVibrating = true;

                try {
                    Thread.sleep(vibrate_time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                isVibrating = false;
                index = 0;

                int size = data_size -diff;
                float [] arranged_arr = new float[size];

                arr = Smoothing(arr);

                int start_point = 0;
                float m = arr[0];

                for (int i = 0; i < diff; i++) {
                    if (m < arr[i]){
                        m = arr[i];
                        start_point = i;
                    }
                }

                for (int i = 0;i< size; i++){
                    arranged_arr[i] = arr[i+start_point];
                }

                float [] abs = new float[size];
                float avg=0,min=arranged_arr[0],max=arranged_arr[0],rms=0;
                int index_max=0;

                ;
                for (int i = 0; i<size;i++){
                    if(arranged_arr[i]<=min){ min = arranged_arr[i];}
                    if(arranged_arr[i]>=max){ max = arranged_arr[i]; index_max = i; }
                }
                /*float [] temp = Arrays.copyOf(arranged_arr, arranged_arr.length);
                for (int i= 0 ; i<size; i++){
                    arranged_arr[i] = temp[(index_max+i)%size];
                }*/

                for (int i = 0; i<size;i++){
                    learning_data +=  arranged_arr[i] + ",";
                    avg = arranged_arr[i];
                    rms = arranged_arr[i]*arranged_arr[i];
                    abs[i] = Math.abs(arranged_arr[i]);
                }
                avg = avg / size;
                rms = (float)Math.sqrt(rms/size);

                for (int i=0; i<size;i++){
                    learning_data += abs[i] + ",";
                }
                learning_data += avg + "," + rms + "," + max + "," + min;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // This gets executed on the UI thread so it can safely modify Views
                        result_tv.setText(runMyModel());
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

    private void initMyModel() {

        inferenceInterface = new TensorFlowInferenceInterface();
        inferenceInterface.initializeTensorFlow(getAssets(), MODEL_FILE);
    }

    private String runMyModel() {
        int size = data_size-diff;
        float[] inputs = new float[size*2+4];
        String []data = learning_data.split(",");
        for (int i=0;i<size*2+4;i++)
            inputs[i]=Float.parseFloat(data[i]);

        MinMaxScaler(inputs);

        inferenceInterface.fillNodeFloat(INPUT_NODE, INPUT_SIZE, inputs);
        inferenceInterface.runInference(new String[] {OUTPUT_NODE});

        float[] res = {0,0,0,0};
        inferenceInterface.readNodeFloat(OUTPUT_NODE, res);

        textView.setText("desk : "+ res[0]*100 +"%\n" +
                          "cup : "+ res[1]*100 +"%\n" +
                          "cup with water : "+ res[2]*100 +"%\n" +
                          "glass bottle : "+ res[3]*100 +"%\n");

        int pos=-1;
        float max_of_result=0;
        for (int i=0;i<res.length;i++) {
            if(max_of_result<res[i]) {
                max_of_result=res[i];
                pos = i;
            }
        }
        switch (pos){
            case 0:
                return "desk";
            case 1:
                return "cup";
            case 2:
                return "cup with water";
            case 3:
                return "glass bottle";
            default:
                return "unclassified";
        }
    }

    public float [] Smoothing (float [] arranged_arr){
        double [] coeffs = SGFilter.computeSGCoefficients(5,5,4);
        //float [] leftPad = new float[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

        ContinuousPadder padder1 = new ContinuousPadder();
        SGFilter sgFilter = new SGFilter(5,5);
        sgFilter.appendPreprocessor(padder1);

        float [] smooth = sgFilter.smooth(arranged_arr,new float [0],new float [0], coeffs);

        MeanValuePadder padder2 = new MeanValuePadder(10,false,true);
        sgFilter.removePreprocessor(padder1);
        sgFilter.appendPreprocessor(padder2);
        smooth = sgFilter.smooth(arranged_arr,new float [0],new float[0],coeffs);
        return smooth;
    }

    double [] min = {3.1742923,2.6723328,-0.9329409,-7.848109,-13.242874,-12.916292,-15.06111,-19.410429,-26.362415,-26.011412,-24.472523,-25.916616,-28.58012,-27.217031,-29.435776,-28.691624,-26.187265,-25.524036,-24.485094,-23.122128,-21.522705,-15.196705,-11.695398,-6.113199,-4.1327143,-7.634536,-6.171756,-0.5866152,-1.7096997,-4.9359117,-5.3176217,-8.838405,-11.051855,-14.0792265,-14.254248,-14.977636,-23.55734,-24.305145,-31.268702,-26.413118,3.1742923,2.6723328,0.8416653,0.062319305,0.016117185,0.018131195,0.020693665,0.007525224,0.010169992,0.17749,0.037536487,0.7707232,0.4372507,0.364421,0.7611249,0.5746137,0.26358166,0.04265952,0.015441654,0.011508159,0.00754572,0.024656639,0.05574126,0.026155796,0.2475089,0.011727082,0.20167242,0.03153968,0.089057796,0.24660306,0.10237696,0.080932714,0.115914024,0.001466124,0.003589639,0.018692017,0.01978623,0.21528652,0.31517208,0.27306396,-0.660328,0.043175206,6.308842,-31.268702
    },
            max = {25.35208,22.431547,20.108902,18.224909,18.240839,17.04319,12.615083,15.535141,15.624341,8.277436,5.1536407,0.7707232,-0.4372507,0.364421,0.7611249,0.7868346,3.503293,5.4952164,3.3717575,8.318792,9.006035,13.960263,17.031307,20.708813,20.956863,23.71411,22.334377,23.214798,25.869389,23.19502,22.303297,22.154547,20.564587,20.827875,18.671219,12.413641,9.128503,7.986317,4.3281093,0.27306396,25.35208,22.431547,20.108902,18.224909,18.240839,17.04319,15.06111,19.410429,26.362415,26.011412,24.472523,25.916616,28.58012,27.217031,29.435776,28.691624,26.187265,25.524036,24.485094,23.122128,21.522705,15.196705,17.031307,20.708813,20.956863,23.71411,22.334377,23.214798,25.869389,23.19502,22.303297,22.154547,20.564587,20.827875,18.671219,14.977636,23.55734,24.305145,31.268702,26.413118,0.006826599,4.1762805,25.869389,-6.975212
            };
    private void MinMaxScaler(float[] inputs){
        for(int i=0;i<inputs.length;i++){
            /*if (min[i]>inputs[i])
                min[i]=inputs[i];
            if (max[i]<inputs[i])
                max[i]=inputs[i];*/
            float numerator = inputs[i] - (float) min[i];
            float denominator = (float) max[i] - (float) min[i];
            double value=numerator / (denominator + 0.00000007);
            inputs[i]= ((float) value);
        }
    }
}
