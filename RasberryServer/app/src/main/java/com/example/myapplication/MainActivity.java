package com.example.myapplication;

import android.app.Activity;
import android.os.Bundle;

import com.example.myapplication.helper.MqttHelper;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.example.myapplication.helper.MqttHelper;


public class MainActivity extends Activity {
    private static final String TAG = "Test";
    private static final String TEMP_DATA = "Temperature";
    private static final int MAX_NUMBERS = 60;

    // UART Configuration Parameters
    private static final int BAUD_RATE = 115200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;

    private PeripheralManager manager = PeripheralManager.getInstance();
    private UartDevice uart;
    int temperature = 0;
    int MAX_TEMP = 100;
    int MIN_TEMP = 0;
    int MIN_TIME = 0;

    GraphView graphView;
    int[] temp_list = new int[MAX_NUMBERS];
    LineGraphSeries<DataPoint> temp_series;

    MqttHelper mqttHelper;

    public void openUART() throws IOException {
        // Configure the UART port
        uart = manager.openUartDevice("UART0");
        uart.setBaudrate(BAUD_RATE);
        uart.setDataSize(DATA_BITS);
        uart.setParity(UartDevice.PARITY_NONE);
        uart.setStopBits(STOP_BITS);
        uart.registerUartDeviceCallback(mCallback);

    }

    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            transferUartData();
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    private void transferUartData(){
        if (uart != null) {
            try {
                int read;
                int count = 0;
                final int maxCount = 16;
                temperature = 0;
                byte[] buffer = new byte[maxCount];
                while ((read = uart.read(buffer, buffer.length)) > 0) {
                    for(int i =0; i<buffer.length; i++)
                    {
                        if (buffer[i]-48 >=0 && buffer[i]-48 <=9) {
                            temperature = temperature * 10 * count + (buffer[i] - 48);
                        }
                        count ++;
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to transfer data over UART", e);
            }
        }
    }

    void setupTimerGetData()
    {
        Timer aTimer = new Timer();
        TimerTask aTask = new TimerTask() {
            @Override
            public void run() {
                refreshData();
            }
        };
        aTimer.schedule(aTask, 1000,60000);
    }

    void setupTimerSendToThinkspeakAndMqtt()
    {
        Timer aTimer = new Timer();
        TimerTask aTask = new TimerTask() {
            @Override
            public void run() {
                if (temperature != 0) sendDataToThingSpeaker(temperature);
                mqttHelper.publishToMQTT(temperature);
            }
        };
        aTimer.schedule(aTask, 1000,30000);
    }


    void setGraph()
    {
        graphView.setTitle("Temperature");
        graphView.setTitleTextSize(40);

        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(MIN_TIME);
        graphView.getViewport().setMaxX(MAX_NUMBERS);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(MIN_TEMP);
        graphView.getViewport().setMaxY(MAX_TEMP);
        graphView.getGridLabelRenderer().setVerticalAxisTitle("Temperature");
        graphView.getGridLabelRenderer().setHorizontalAxisTitle("Time");
    }
    public void refreshData()
    {
        if (temperature>=0 && temperature<=100) {
            temp_series = new LineGraphSeries<DataPoint>();
            for (int i = 0; i < MAX_NUMBERS - 1; i++) {
                temp_list[i] = temp_list[i + 1];
                temp_series.appendData(new DataPoint(i, temp_list[i]), true, MAX_NUMBERS);
            }
            temp_list[MAX_NUMBERS - 1] = temperature;
            temp_series.appendData(new DataPoint(MAX_NUMBERS - 1, temp_list[MAX_NUMBERS - 1]), true, MAX_NUMBERS);

            graphView.removeAllSeries();
            graphView.addSeries(temp_series);
        }
    }

    private void sendDataToThingSpeaker(int value) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();

        String API_KEY = "8J8G4JPXXKXTKSG9";

        String c = Integer.toString(value);
        Request request = builder.url("https://api.thingspeak.com/update?api_key=" + API_KEY + "&field1=" + c).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.i("Send Data to Thingkspeak","Fail");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String jsonString = response.body().string();

            }
        });
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
                Log.i("Debug", mqttMessage.toString());
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
        graphView = findViewById(R.id.graphview);
        try {
            List<String> deviceList = manager.getUartDeviceList();
            if (deviceList.isEmpty()) {
                Log.i(TAG, "No UART port available on this device.");
            } else {
                Log.i(TAG, "List of available devices: " + deviceList);
            }
            openUART();
        } catch (IOException e) {
            Log.i(TAG, "Unable to access UART device", e);
        }
        startMqtt();
        setGraph();
        setupTimerGetData();
        setupTimerSendToThinkspeakAndMqtt();
    }

}


