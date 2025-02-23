package com.example.wenshi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.wenshi.data.AppDatabase;
import com.example.wenshi.data.TemperatureRecord;
import com.example.wenshi.data.TemperatureStats;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//---------------------

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wenshi.data.AppDatabase;
import com.example.wenshi.data.TemperatureRecord;
import com.example.wenshi.data.TemperatureStats;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemperatureChartActivity extends AppCompatActivity {
    private LineChart chart;
    private Spinner chartTypeSpinner;
    private FloatingActionButton fabExport, fabAnalysis, fabDatePicker;
    private AppDatabase database;
    private ExecutorService executorService;
    private Calendar selectedDate = Calendar.getInstance();
    private List<Entry> currentTempEntries = new ArrayList<>();
    private List<Entry> currentHumidEntries = new ArrayList<>();
    private String[] currentXLabels;
    private static final float WARNING_TEMP = 30f;
    private static final float CRITICAL_TEMP = 35f;
    private static final float WARNING_HUMIDITY = 80f;
    private SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_chart);

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        initializeViews();
        setupChart();
        setupSpinner();
        setupFABs();
    }

    private void initializeViews() {
        chart = findViewById(R.id.chart);
        chartTypeSpinner = findViewById(R.id.chartTypeSpinner);
        fabExport = findViewById(R.id.fabExport);
        fabAnalysis = findViewById(R.id.fabAnalysis);
        fabDatePicker = findViewById(R.id.fabDatePicker);
    }

    private void setupSpinner() {
        String[] chartTypes = {"最近记录", "每小时平均", "每日平均"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, chartTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartTypeSpinner.setAdapter(adapter);

        chartTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateChart(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(android.graphics.Color.WHITE);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(android.graphics.Color.BLUE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(50f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setTextColor(android.graphics.Color.RED);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(100f);

        addWarningLines(leftAxis, rightAxis);
    }

    private void addWarningLines(YAxis leftAxis, YAxis rightAxis) {
        // 温度警戒线
        LimitLine warningTemp = new LimitLine(WARNING_TEMP, "警告温度");
        warningTemp.setLineColor(android.graphics.Color.YELLOW);
        warningTemp.setLineWidth(1f);
        warningTemp.enableDashedLine(10f, 5f, 0f);

        LimitLine criticalTemp = new LimitLine(CRITICAL_TEMP, "危险温度");
        criticalTemp.setLineColor(android.graphics.Color.RED);
        criticalTemp.setLineWidth(1f);
        criticalTemp.enableDashedLine(10f, 5f, 0f);

        leftAxis.addLimitLine(warningTemp);
        leftAxis.addLimitLine(criticalTemp);

        // 湿度警戒线
        LimitLine warningHumidity = new LimitLine(WARNING_HUMIDITY, "警告湿度");
        warningHumidity.setLineColor(android.graphics.Color.YELLOW);
        warningHumidity.setLineWidth(1f);
        warningHumidity.enableDashedLine(10f, 5f, 0f);

        rightAxis.addLimitLine(warningHumidity);
    }

    private void setupFABs() {
        fabExport.setOnClickListener(v -> exportData());
        fabAnalysis.setOnClickListener(v -> showAnalysis());
        fabDatePicker.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择日期")
                .setSelection(selectedDate.getTimeInMillis())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDate.setTimeInMillis(selection);
            updateChart(chartTypeSpinner.getSelectedItemPosition());
        });

        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void updateChart(int chartType) {
        executorService.execute(() -> {
            currentTempEntries.clear();
            currentHumidEntries.clear();

            switch (chartType) {
                case 0: // 最近记录
                    loadRecentRecords();
                    break;
                case 1: // 每小时平均
                    loadHourlyAverages();
                    break;
                case 2: // 每日平均
                    loadDailyAverages();
                    break;
            }

            runOnUiThread(this::updateChartUI);
        });
    }

    private void loadRecentRecords() {
        List<TemperatureRecord> records = database.temperatureDao().getLast100Records();
        currentXLabels = new String[records.size()];

        for (int i = 0; i < records.size(); i++) {
            TemperatureRecord record = records.get(i);
            currentTempEntries.add(new Entry(i, record.temperature));
            currentHumidEntries.add(new Entry(i, record.humidity));
            currentXLabels[i] = hourFormat.format(new Date(record.timestamp));
        }
    }

    private void loadHourlyAverages() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = dateFormat.format(selectedDate.getTime());
        List<TemperatureStats> stats = database.temperatureDao().getHourlyAveragesForDay(dateString);

        for (TemperatureStats stat : stats) {
            int hour = Integer.parseInt(stat.timeLabel);
            currentTempEntries.add(new Entry(hour, stat.temperature));
            currentHumidEntries.add(new Entry(hour, stat.humidity));
        }

        currentXLabels = new String[24];
        for (int i = 0; i < 24; i++) {
            currentXLabels[i] = String.format("%02d:00", i);
        }
    }

    private void loadDailyAverages() {
        String monthString = monthFormat.format(selectedDate.getTime());
        List<TemperatureStats> stats = database.temperatureDao().getDailyAveragesForMonth(monthString);

        for (TemperatureStats stat : stats) {
            int day = Integer.parseInt(stat.timeLabel) - 1;
            currentTempEntries.add(new Entry(day, stat.temperature));
            currentHumidEntries.add(new Entry(day, stat.humidity));
        }

        int daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        currentXLabels = new String[daysInMonth];
        for (int i = 0; i < daysInMonth; i++) {
            currentXLabels[i] = String.format("%d日", i + 1);
        }
    }

    private void updateChartUI() {
        // 温度数据集
        LineDataSet tempDataSet = new LineDataSet(currentTempEntries, "温度 (°C)");
        tempDataSet.setColor(android.graphics.Color.BLUE);
        tempDataSet.setCircleColor(android.graphics.Color.BLUE);
        tempDataSet.setCircleRadius(3f);
        tempDataSet.setDrawCircleHole(false);
        tempDataSet.setLineWidth(2f);
        tempDataSet.setDrawValues(false);
        tempDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        tempDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        tempDataSet.setDrawFilled(true);
        tempDataSet.setFillAlpha(50);
        tempDataSet.setFillColor(android.graphics.Color.BLUE);

        // 湿度数据集
        LineDataSet humidDataSet = new LineDataSet(currentHumidEntries, "湿度 (%)");
        humidDataSet.setColor(android.graphics.Color.RED);
        humidDataSet.setCircleColor(android.graphics.Color.RED);
        humidDataSet.setCircleRadius(3f);
        humidDataSet.setDrawCircleHole(false);
        humidDataSet.setLineWidth(2f);
        humidDataSet.setDrawValues(false);
        humidDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        humidDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        humidDataSet.setDrawFilled(true);
        humidDataSet.setFillAlpha(50);
        humidDataSet.setFillColor(android.graphics.Color.RED);

        // 设置X轴标签
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < currentXLabels.length) {
                    return currentXLabels[index];
                }
                return "";
            }
        });

        // 更新图表数据
        LineData lineData = new LineData(tempDataSet, humidDataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void exportData() {
        if (currentTempEntries.isEmpty() || currentHumidEntries.isEmpty()) {
            Snackbar.make(chart, "没有可导出的数据", Snackbar.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                File exportDir = new File(getExternalFilesDir(null), "exports");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                File exportFile = new File(exportDir, "temperature_data_" + timestamp + ".csv");

                try (FileWriter writer = new FileWriter(exportFile)) {
                    writer.append("时间,温度(°C),湿度(%)\n");

                    for (int i = 0; i < currentXLabels.length; i++) {
                        if (i < currentTempEntries.size() && i < currentHumidEntries.size()) {
                            writer.append(String.format(Locale.getDefault(), "%s,%.1f,%.1f\n",
                                    currentXLabels[i],
                                    currentTempEntries.get(i).getY(),
                                    currentHumidEntries.get(i).getY()));
                        }
                    }
                }

                Uri contentUri = FileProvider.getUriForFile(this,
                        "com.example.wenshi.fileprovider", exportFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/csv");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    startActivity(Intent.createChooser(shareIntent, "分享数据"));
                    Snackbar.make(chart, "数据导出成功", Snackbar.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Snackbar.make(chart, "导出失败: " + e.getMessage(), Snackbar.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showAnalysis() {
        if (currentTempEntries.isEmpty() || currentHumidEntries.isEmpty()) {
            Snackbar.make(chart, "没有数据可供分析", Snackbar.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_analysis, null);
        updateAnalysisView(dialogView);

        new MaterialAlertDialogBuilder(this)
                .setTitle("数据分析")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .show();
    }

    private void updateAnalysisView(View view) {
        // 计算统计数据
        float avgTemp = 0, avgHumid = 0;
        float minTemp = Float.MAX_VALUE, maxTemp = Float.MIN_VALUE;
        float minHumid = Float.MAX_VALUE, maxHumid = Float.MIN_VALUE;

        for (Entry entry : currentTempEntries) {
            float temp = entry.getY();
            avgTemp += temp;
            minTemp = Math.min(minTemp, temp);
            maxTemp = Math.max(maxTemp, temp);
        }

        for (Entry entry : currentHumidEntries) {
            float humid = entry.getY();
            avgHumid += humid;
            minHumid = Math.min(minHumid, humid);
            maxHumid = Math.max(maxHumid, humid);
        }

        avgTemp /= currentTempEntries.size();
        avgHumid /= currentHumidEntries.size();

        // 更新UI
        ((TextView) view.findViewById(R.id.tvTempAvg)).setText(
                String.format(Locale.getDefault(), "平均温度: %.1f°C", avgTemp));
        ((TextView) view.findViewById(R.id.tvTempRange)).setText(
                String.format(Locale.getDefault(), "温度范围: %.1f°C - %.1f°C", minTemp, maxTemp));
        ((TextView) view.findViewById(R.id.tvHumidAvg)).setText(
                String.format(Locale.getDefault(), "平均湿度: %.1f%%", avgHumid));
        ((TextView) view.findViewById(R.id.tvHumidRange)).setText(
                String.format(Locale.getDefault(), "湿度范围: %.1f%% - %.1f%%", minHumid, maxHumid));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}