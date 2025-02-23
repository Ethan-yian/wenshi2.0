package com.example.wenshi;

import java.util.List;
import com.github.mikephil.charting.data.Entry;

public class StatsManager {
    private List<Entry> entries;
    private float currentValue;
    private float minValue;
    private float maxValue;
    private float avgValue;
    private float trend;

    public StatsManager(List<Entry> entries) {
        this.entries = entries;
        calculateStats();
    }

    private void calculateStats() {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        currentValue = entries.get(entries.size() - 1).getY();
        minValue = Float.MAX_VALUE;
        maxValue = Float.MIN_VALUE;
        float sum = 0;

        for (Entry entry : entries) {
            float value = entry.getY();
            minValue = Math.min(minValue, value);
            maxValue = Math.max(maxValue, value);
            sum += value;
        }

        avgValue = sum / entries.size();
        calculateTrend();
    }

    private void calculateTrend() {
        if (entries.size() < 2) {
            trend = 0;
            return;
        }

        // 使用简单线性回归计算趋势
        float sumX = 0;
        float sumY = 0;
        float sumXY = 0;
        float sumXX = 0;
        int n = entries.size();

        for (int i = 0; i < n; i++) {
            float x = i;
            float y = entries.get(i).getY();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        trend = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    }

    public float getCurrentValue() { return currentValue; }
    public float getMinValue() { return minValue; }
    public float getMaxValue() { return maxValue; }
    public float getAverageValue() { return avgValue; }
    public float getTrend() { return trend; }

    public String getTrendDescription() {
        if (Math.abs(trend) < 0.1) return "稳定";
        if (trend > 0) return "上升";
        return "下降";
    }
}