package com.example.wenshi.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "todo_items")
public class TodoItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public long targetTimestamp; // 目标完成时间戳
    public boolean isCompleted;

    public TodoItem(String title, long targetTimestamp) {
        this.title = title;
        this.targetTimestamp = targetTimestamp;
        this.isCompleted = false;
    }

    // Getter methods
    public int getId() {
        return id;
    }







}