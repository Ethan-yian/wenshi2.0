package com.example.wenshi.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "temperature_records")
public class TemperatureRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public float temperature;
    public float humidity;
    public long timestamp;

    public TemperatureRecord(float temperature, float humidity, long timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }
}