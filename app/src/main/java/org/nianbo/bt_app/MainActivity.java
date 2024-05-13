package org.nianbo.bt_app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> spinnerAdapter;
    private ArrayList<String> deviceList;
    private TextView infoStatus;
    private Set<BluetoothDevice> pairedDevices;
    Spinner btDevicesSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoStatus = findViewById(R.id.textViewInfo);
        btDevicesSpinner = findViewById(R.id.spinnerBtDevices);
        deviceList = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        btDevicesSpinner.setAdapter(spinnerAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            infoStatus.setText("Не поддерживается");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        pairedDevices = bluetoothAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                spinnerAdapter.add(device.getName() + " - " + device.getAddress());
            }
        } else {
            Log.d("Bluetooth", "Нет сопряженных устройств");
        }

        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void sendDataToDevice(String deviceAddress, String data) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        BluetoothSocket socket = null;
        OutputStream outputStream = null;

        try {
            // Создаем BluetoothSocket для выбранного устройства
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

            // Устанавливаем соединение
            socket.connect();

            // Получаем выходной поток для отправки данных
            outputStream = socket.getOutputStream();

            // Отправляем данные
            outputStream.write(data.getBytes());
            outputStream.flush();

            Log.d("Bluetooth", "Данные отправлены на устройство " + deviceAddress);
            showPopup(MainActivity.this, "Отправлено", String.format("Адрес: %s\nДанные: %s", deviceAddress, data));
        } catch (IOException e) {
            Log.e("Bluetooth", "Ошибка отправки данных на устройство " + deviceAddress, e);
            showPopup(MainActivity.this, "Ошибка", String.format("Адрес: %s\nДанные: %s\nОшибка: %s", deviceAddress, data, e.getMessage()));
        } finally {
            // Закрываем соединение и потоки
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public String getAddress() {
        return btDevicesSpinner.getSelectedItem().toString().split(" - ")[1];
    }

    // Нажатие кнопок
    public void onClickButtonUp(View v) {
        sendDataToDevice(getAddress(), "0");
    }

    public void onClickButtonDown(View v) {
        sendDataToDevice(getAddress(), "1");
    }

    private void showPopup(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}