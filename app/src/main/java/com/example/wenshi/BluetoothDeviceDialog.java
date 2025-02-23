package com.example.wenshi;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceDialog extends Dialog {
    private List<BluetoothDevice> devices;
    private OnDeviceSelectedListener listener;
    private ListView deviceList;
    private TextView emptyStateText;
    private MaterialButton btnScanDevices;

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    public BluetoothDeviceDialog(@NonNull Context context, List<BluetoothDevice> devices, OnDeviceSelectedListener listener) {
        super(context);
        this.devices = new ArrayList<>(devices);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_bluetooth_devices);

        deviceList = findViewById(R.id.deviceList);
        emptyStateText = findViewById(R.id.emptyStateText);
        btnScanDevices = findViewById(R.id.btnScanDevices);

        DeviceAdapter adapter = new DeviceAdapter(getContext(), devices);
        deviceList.setAdapter(adapter);

        updateEmptyState();

        deviceList.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null) {
                listener.onDeviceSelected(devices.get(position));
            }
            dismiss();
        });

        btnScanDevices.setOnClickListener(v -> {
            // 启动蓝牙扫描
            // TODO: 实现蓝牙扫描逻辑
        });
    }

    private void updateEmptyState() {
        if (devices.isEmpty()) {
            deviceList.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            deviceList.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private static class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
        private final LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            super(context, 0, devices);
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_bluetooth_device, parent, false);
            }

            BluetoothDevice device = getItem(position);
            if (device != null) {
                TextView nameText = view.findViewById(R.id.deviceName);
                TextView addressText = view.findViewById(R.id.deviceAddress);

                nameText.setText(device.getName() != null ? device.getName() : "未知设备");
                addressText.setText(device.getAddress());
            }

            return view;
        }
    }
}