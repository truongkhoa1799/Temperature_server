package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import com.example.myapplication.helper.MqttHelper;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.example.myapplication.Data;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {
    int MAX_NUMBERS = 60;
    public int[] list = new int[MAX_NUMBERS];
    MqttHelper mqttHelper;
    TextView current_date;
    TextView current_time;
    TextView current_temp;
    Button his_but;
    Button back_main_but;
    DateTimeFormatter date_format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter time_format = DateTimeFormatter.ofPattern("HH:mm");

    OkHttpClient okHttpClient = new OkHttpClient();
    Request.Builder builder = new Request.Builder();

    void setupThingkSpeakTimer()
    {
        Timer aTimer = new Timer();
        TimerTask aTask = new TimerTask() {
            @Override
            public void run() {
                getData();
            }
        };
        aTimer.schedule(aTask, 1000,60000);
    }
    void getData()
    {
        final Request request = builder.url("https://api.thingspeak.com/channels/931129/fields/1.json?api_key=LCAKO3L3V4KMUPSY&results=").build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d("get infor", "fail");
            }
            @Override
            public void onResponse(Response response) throws IOException {
                String jsonstring = response.body().string();
                try{
                    int count =MAX_NUMBERS;
                    JSONObject jsonObject = new JSONObject(jsonstring);
                    JSONArray feeds = jsonObject.getJSONArray("feeds");
                    for(int i =feeds.length()-1; i>=feeds.length()-MAX_NUMBERS; i--)
                    {
                        count = count-1;
                        JSONObject jo = feeds.getJSONObject(i);
                        String data = jo.getString("field1");
                        list[count] = Integer.parseInt(data);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void setupTimerGetTime()
    {
        Timer aTimer = new Timer();
        TimerTask aTask = new TimerTask() {
            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                String time = "";
                String date = "";
                String temp_time = time_format.format(now);
                String temp_date = date_format.format(now);
                time += "Hour:    ";
                date += "Date:    ";
                time += temp_time;
                date += temp_date;
                current_date.setText(date);
                current_time.setText(time);
            }
        };
        aTimer.schedule(aTask, 1000,60000);
    }
    private void startMqtt() {
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String temp = "Temperature:    " +  mqttMessage.toString() + "  \u00B0" + "C";
                current_temp.setText(temp);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        his_but  = findViewById(R.id.analysis_data_but);
        current_date = findViewById(R.id.date_time);
        current_time = findViewById(R.id.hour_time);
        current_temp = findViewById(R.id.temperature);
        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.milkshake);
        his_but.setAnimation(myAnim);
        setupTimerGetTime();
        startMqtt();
        setupThingkSpeakTimer();
        his_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(myAnim);
                Intent myIntent = new Intent(MainActivity.this, Data.class);
                myIntent.putExtra("list_data" , list);
                MainActivity.this.startActivity(myIntent);
            }
        });

    }
}

