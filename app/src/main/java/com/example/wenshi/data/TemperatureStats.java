package com.example.wenshi.data;

import androidx.room.ColumnInfo;

public class TemperatureStats {
    @ColumnInfo(name = "temperature")
    public float temperature;

    @ColumnInfo(name = "humidity")
    public float humidity;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "time_label")
    public String timeLabel;
}