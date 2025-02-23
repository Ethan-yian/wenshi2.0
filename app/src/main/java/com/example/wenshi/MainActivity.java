package com.example.wenshi;

import androidx.annotation.NonNull;
import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.example.wenshi.data.AppDatabase;
import com.example.wenshi.data.TemperatureRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private boolean permissionsGranted = false;

    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final UUID BLUETOOTH_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int RECONNECT_DELAY = 5000; // 5秒后重试连接
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long HISTORY_BUTTON_AUTO_HIDE_DELAY = 5000; // 5秒后自动隐藏

    private TextView temperatureText;
    private TextView humidityText;
    private TextView timeText;
    private TextView dateText;
    private TextView weekdayText;
    private Button viewHistoryButton;
    private CardView historyButtonContainer;
    private boolean isHistoryButtonVisible = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;
    private int reconnectAttempts = 0;
    private boolean isReconnecting = false;

    private Handler handler;
    private ExecutorService executor;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 注册蓝牙启用结果回调
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handler.postDelayed(this::showBluetoothDeviceDialog, 500);
                    } else {
                        Toast.makeText(this, "需要启用蓝牙来获取温度数据", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        initializeViews();
        initializeComponents();
        setupClickListeners();
        startTimeUpdate();
        initializeBluetooth();
        // 检查并请求权限
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            permissionsGranted = true;
            initializeBluetooth();
        }
    }


    private void initializeViews() {
        temperatureText = findViewById(R.id.temperatureText);
        humidityText = findViewById(R.id.humidityText);
        timeText = findViewById(R.id.timeText);
        dateText = findViewById(R.id.dateText);
        weekdayText = findViewById(R.id.weekdayText);
        viewHistoryButton = findViewById(R.id.viewHistoryButton);
        historyButtonContainer = findViewById(R.id.historyButtonContainer);

        // Set initial state of history button to invisible
        historyButtonContainer.setVisibility(View.INVISIBLE);
        historyButtonContainer.setAlpha(0f);
        historyButtonContainer.setScaleX(0.8f);
        historyButtonContainer.setScaleY(0.8f);
        isHistoryButtonVisible = false;

        // 设置整个界面的点击监听
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> toggleHistoryButton());
    }

    private void initializeComponents() {
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
        database = AppDatabase.getInstance(this);

        // 使用新的方式获取 BluetoothAdapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void setupClickListeners() {
        // 为历史按钮添加触摸动画效果
        viewHistoryButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .start();
                    return true;

                case MotionEvent.ACTION_UP:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .withEndAction(() -> {
                                Intent intent = new Intent(MainActivity.this, TemperatureChartActivity.class);
                                startActivity(intent);
                            })
                            .start();
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                    return true;
            }
            return false;
        });
    }

    private void toggleHistoryButton() {
        if (isHistoryButtonVisible) {
            hideHistoryButton();
        } else {
            showHistoryButton();
        }
    }

    private void showHistoryButton() {
        // 移除之前的自动隐藏任务
        handler.removeCallbacks(hideHistoryButtonRunnable);

        historyButtonContainer.setVisibility(View.VISIBLE);
        historyButtonContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start();
        isHistoryButtonVisible = true;

        // 5秒后自动隐藏
        handler.postDelayed(hideHistoryButtonRunnable, HISTORY_BUTTON_AUTO_HIDE_DELAY);
    }

    private final Runnable hideHistoryButtonRunnable = new Runnable() {
        @Override
        public void run() {
            hideHistoryButton();
        }
    };

    private void hideHistoryButton() {
        historyButtonContainer.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction(() -> {
                    historyButtonContainer.setVisibility(View.INVISIBLE);
                    isHistoryButtonVisible = false;
                })
                .start();
    }

    private void startTimeUpdate() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void updateDateTime() {
        Calendar calendar = Calendar.getInstance();

        // 更新时间 - 直接显示原始格式，不添加任何额外空格
        String time = String.format(Locale.CHINA, "%02d:%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));

        // 直接设置时间，不做任何替换
        timeText.setText(time);

        // 更新日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        dateText.setText(dateFormat.format(calendar.getTime()));

        // 更新星期
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        weekdayText.setText(weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            permissionsGranted = allGranted;
            if (allGranted) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "需要蓝牙权限才能获取温度数据", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void initializeBluetooth() {
        if (!permissionsGranted) {
            return;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableBtIntent);
            }
        } else {
            // 确保在主线程中显示对话框
            handler.post(this::showBluetoothDeviceDialog);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                showBluetoothDeviceDialog();
            } else {
                Toast.makeText(this, "需要启用蓝牙来获取温度数据", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBluetoothDeviceDialog() {
        try {
            if (!permissionsGranted || bluetoothAdapter == null) {
                Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT).show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    enableBluetoothLauncher.launch(enableBtIntent);
                }
                return;
            }

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "缺少蓝牙连接权限", Toast.LENGTH_SHORT).show();
                return;
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && pairedDevices.size() > 0) {
                List<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);

                // 确保在主线程中显示对话框
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> {
                        try {
                            BluetoothDeviceDialog dialog = new BluetoothDeviceDialog(
                                    MainActivity.this,
                                    deviceList,
                                    this::connectToDevice
                            );
                            dialog.setCancelable(true);
                            dialog.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,
                                    "显示蓝牙设备列表失败: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(this, "没有配对的蓝牙设备，请先配对设备", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "蓝牙操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 添加重新连接的方法
    public void reconnectBluetooth() {
        if (permissionsGranted) {
            initializeBluetooth();
        } else {
            checkAndRequestPermissions();
        }
    }

    // 在菜单项中添加重新连接选项
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reconnect) {
            reconnectBluetooth();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 添加重新连接蓝牙的菜单项
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void showPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null && device.getName().contains("HC-06")) {
                    connectToDevice(device);
                    break;
                }
            }
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        executor.execute(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(BLUETOOTH_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();
                isConnected = true;

                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                });

                startDataCollection();
            } catch (IOException e) {
                isConnected = false;

                handler.post(() -> Toast.makeText(MainActivity.this,
                        "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                handleDisconnection();
            }
        });
    }

    private void startDataCollection() {
        executor.execute(() -> {
            byte[] buffer = new byte[1024];
            while (isConnected) {
                try {
                    outputStream.write(new byte[]{0x4D});
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String data = new String(buffer, 0, bytes);
                        parseAndSaveData(data);
                    }
                    Thread.sleep(60000); // 每分钟更新一次
                } catch (IOException | InterruptedException e) {
                    handleDisconnection();
                    break;
                }
            }
        });
    }

    private void handleDisconnection() {
        isConnected = false;

        handler.post(() -> {
            temperatureText.setText("--.-°C");
            humidityText.setText("--.--%");
            Toast.makeText(MainActivity.this, "蓝牙连接断开", Toast.LENGTH_SHORT).show();
        });

        if (!isReconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            isReconnecting = true;
            handler.postDelayed(() -> {
                reconnectAttempts++;
                isReconnecting = false;
                showPairedDevices();
            }, RECONNECT_DELAY);
        }
    }

    private void parseAndSaveData(String data) {
        try {
            String[] parts = data.split(",");
            String tempStr = parts[0].split(":")[1].trim().replace("°C", "").trim();
            String humidStr = parts[1].split(":")[1].trim().replace("%", "").trim();

            float temperature = Float.parseFloat(tempStr);
            float humidity = Float.parseFloat(humidStr);

            updateTemperatureHumidity(temperature, humidity);

            TemperatureRecord record = new TemperatureRecord(temperature, humidity, System.currentTimeMillis());
            executor.execute(() -> {
                database.temperatureDao().insert(record);
            });

            reconnectAttempts = 0;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing data: " + data, e);
        }
    }

    private void updateTemperatureHumidity(float temperature, float humidity) {
        if (temperature < -40 || temperature > 80) {
            Log.w(TAG, "Temperature out of normal range: " + temperature);
            return;
        }
        if (humidity < 0 || humidity > 100) {
            Log.w(TAG, "Humidity out of normal range: " + humidity);
            return;
        }

        handler.post(() -> {
            try {
                // 更新温度显示
                String tempText = String.format(Locale.CHINA, "%.1f°C", temperature);
                temperatureText.setText(tempText);

                // 设置温度颜色
                int tempColor;
                if (temperature > 30) {
                    tempColor = Color.rgb(237, 85, 106); // 高温红色
                } else if (temperature < 10) {
                    tempColor = Color.rgb(33, 119, 184); // 低温蓝色
                } else {
                    tempColor = Color.rgb(60, 149, 102); // 适中绿色
                }

                // 更新湿度显示
                String humidText = String.format(Locale.CHINA, "%.1f%%", humidity);
                humidityText.setText(humidText);

                // 设置湿度颜色
                int humidColor;
                if (humidity > 70) {
                    humidColor = Color.rgb(68, 68, 255); // 潮湿蓝色
                } else if (humidity < 30) {
                    humidColor = Color.rgb(255, 68, 68); // 干燥红色
                } else {
                    humidColor = Color.rgb(68, 255, 68); // 适中绿色
                }

                // 添加温度颜色渐变动画
                ValueAnimator tempColorAnimation = ValueAnimator.ofObject(
                        new ArgbEvaluator(),
                        temperatureText.getCurrentTextColor(),
                        tempColor
                );
                tempColorAnimation.setDuration(500);
                tempColorAnimation.addUpdateListener(
                        animator -> temperatureText.setTextColor((Integer) animator.getAnimatedValue())
                );
                tempColorAnimation.start();

                // 添加湿度颜色渐变动画
                ValueAnimator humidColorAnimation = ValueAnimator.ofObject(
                        new ArgbEvaluator(),
                        humidityText.getCurrentTextColor(),
                        humidColor
                );
                humidColorAnimation.setDuration(500);
                humidColorAnimation.addUpdateListener(
                        animator -> humidityText.setTextColor((Integer) animator.getAnimatedValue())
                );
                humidColorAnimation.start();
            } catch (Exception e) {
                Log.e(TAG, "Error updating temperature display", e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
        handler.removeCallbacksAndMessages(null);
        executor.shutdown();

        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing bluetooth socket", e);
            }
        }
    }
}