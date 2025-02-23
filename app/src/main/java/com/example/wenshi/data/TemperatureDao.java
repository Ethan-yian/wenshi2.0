package com.example.wenshi.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TemperatureDao {
    @Insert
    void insert(TemperatureRecord record);

    @Query("DELETE FROM temperature_records")
    void deleteAll();

    // 获取最近100次记录
    @Query("SELECT * FROM temperature_records ORDER BY timestamp DESC LIMIT 5")
    List<TemperatureRecord> getLast100Records();

    // 获取指定日期的每小时平均值
    @Query("SELECT AVG(temperature) as temperature, AVG(humidity) as humidity, " +
            "MIN(timestamp) as timestamp, " +
            "strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) as time_label " +
            "FROM temperature_records " +
            "WHERE date(datetime(timestamp/1000, 'unixepoch', 'localtime')) = date(:dateString) " +
            "GROUP BY strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) " +
            "ORDER BY time_label ASC")
    List<TemperatureStats> getHourlyAveragesForDay(String dateString);

    // 获取指定月份的每日平均值
    @Query("SELECT AVG(temperature) as temperature, AVG(humidity) as humidity, " +
            "MIN(timestamp) as timestamp, " +
            "strftime('%d', datetime(timestamp/1000, 'unixepoch', 'localtime')) as time_label " +
            "FROM temperature_records " +
            "WHERE strftime('%Y-%m', datetime(timestamp/1000, 'unixepoch', 'localtime')) = :yearMonth " +
            "GROUP BY strftime('%d', datetime(timestamp/1000, 'unixepoch', 'localtime')) " +
            "ORDER BY time_label ASC")
    List<TemperatureStats> getDailyAveragesForMonth(String yearMonth);
}