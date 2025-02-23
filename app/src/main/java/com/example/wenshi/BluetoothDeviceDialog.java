package com.example.wenshi;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceDialog extends Dialog {
    private List<BluetoothDevice> devices;
    private OnDeviceSelectedListener listener;
    private ListView deviceList;

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
        setContentView(R.layout.dialog_bluetooth_devices);
        setTitle("选择蓝牙设备");

        deviceList = findViewById(R.id.deviceList);
        ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(
                getContext(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                devices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                BluetoothDevice device = devices.get(position);
                text1.setText(device.getName() != null ? device.getName() : "未知设备");
                text2.setText(device.getAddress());

                return view;
            }
        };

        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null) {
                listener.onDeviceSelected(devices.get(position));
            }
            dismiss();
        });
    }
}