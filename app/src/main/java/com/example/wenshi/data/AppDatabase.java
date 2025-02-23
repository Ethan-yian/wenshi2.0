package com.example.wenshi.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TemperatureRecord.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TemperatureDao temperatureDao();

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "temperature_db")
                    .build();
        }
        return instance;
    }
}