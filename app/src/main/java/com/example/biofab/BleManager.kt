package com.example.biofab

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.util.UUID

object BleManager {

    var bluetoothGatt: BluetoothGatt? = null
    var writeCharacteristic: BluetoothGattCharacteristic? = null
    var notifyCharacteristic: BluetoothGattCharacteristic? = null
    var selectedDevice: BluetoothDevice? = null
    var isReady = false

    private var gattCallback: BluetoothGattCallback? = null

    /** Подключение к устройству, если еще нет активного соединения */
    fun connect(device: BluetoothDevice, context: android.content.Context, callback: BluetoothGattCallback) {
        if (bluetoothGatt != null) {
            // Уже подключено
            return
        }
        selectedDevice = device
        gattCallback = callback
//        Handler(Looper.getMainLooper()).post {
//            bluetoothGatt = device.connectGatt(context, false, callback)
//        }
        bluetoothGatt = device.connectGatt(context, false, callback,BluetoothDevice.TRANSPORT_LE)
//        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
//        } else {
//            device.connectGatt(context, false, callback)
//        }
    }

    /** Безопасное отключение */
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            try {
                notifyCharacteristic?.let { ch ->
                    gatt.setCharacteristicNotification(ch, false)
                    ch.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    )?.let { descriptor ->
                        descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        clear()
    }

    /** Полная очистка состояния */
    fun clear() {
        bluetoothGatt = null
        writeCharacteristic = null
        notifyCharacteristic = null
        selectedDevice = null
        isReady = false
        gattCallback = null
    }

    /** Проверка, подключено ли устройство */
    fun isConnected(): Boolean {
        return bluetoothGatt != null
    }
}