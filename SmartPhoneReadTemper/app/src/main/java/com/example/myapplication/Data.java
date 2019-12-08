package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.example.myapplication.MainActivity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Timer;
import java.util.TimerTask;

public class Data extends AppCompatActivity {
    Button back_but;
    int MAX_NUMBERS = 60;
    int MIN_TIME = 0;
    int MIN_TEMP = 0;
    int MAX_TEMP = 100;
    int[] list = new int[MAX_NUMBERS];
    GraphView graphView;
    LineGraphSeries<DataPoint> temp_series;

    void setGraph()
    {
        temp_series = new LineGraphSeries<DataPoint>();
        for (int i = 0; i < MAX_NUMBERS ; i++) {
            temp_series.appendData(new DataPoint(i, 0), true, MAX_NUMBERS);
        }
        graphView.removeAllSeries();
        graphView.addSeries(temp_series);

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
        refreshData();
    }

    public void refreshData()
    {
        temp_series = new LineGraphSeries<DataPoint>();
        for (int i = 0; i < MAX_NUMBERS ; i++) {
            temp_series.appendData(new DataPoint(i, list[i]), true, MAX_NUMBERS);
        }

        graphView.removeAllSeries();
        graphView.addSeries(temp_series);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_layout);
        graphView = findViewById(R.id.graphview);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            list  = extras.getIntArray("list_data");
        }
        setGraph();
        back_but = findViewById(R.id.back_but);
        back_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Data.this.finish();
            }
        });
    }
}
