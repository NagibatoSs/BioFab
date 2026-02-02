package com.example.biofab

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.util.UUID

object BleManager {

    var bluetoothGatt: BluetoothGatt? = null
    var writeCharacteristic: BluetoothGattCharacteristic? = null
    var notifyCharacteristic: BluetoothGattCharacteristic? = null
    var selectedDevice: BluetoothDevice? = null
    var isReady = false

    val commandQueue = mutableListOf<String>()

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

    var isWriting = false

//    val gattCallback = object : BluetoothGattCallback() {
//        override fun onCharacteristicWrite(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            status: Int
//        ) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d("BLE", "Write OK: ${characteristic.uuid}")
//            } else {
//                Log.e("BLE", "Write FAILED: $status")
//            }
//
//            // --- обработка очереди ---
//            if (commandQueue.isNotEmpty()) {
//                val next = commandQueue.removeAt(0)
//                writeCharacteristic?.value = next.toByteArray(Charsets.UTF_8)
//                isWriting = true
//                gatt.writeCharacteristic(writeCharacteristic)
//            } else {
//                isWriting = false
//            }
//        }
//
//        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//            Log.e("BLE", "STATE CHANGE: status=$status, newState=$newState")
//
//            // Если произошла ошибка показать тост
//            if (status != BluetoothGatt.GATT_SUCCESS) {
//                val reason = mapDisconnectReason(status)
//            }
//
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                Log.d("BLE", "Connected to GATT server")
//
//                gatt.discoverServices()
//            }
//
//            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.e("BLE", "Disconnected: status=$status")
//
//                Log.d("BLE", "Disconnected from GATT server")
//                BleManager.bluetoothGatt = null
//                BleManager.isReady = false
//
//            }
//        }
//    }

    fun sendCommand(json: String) {
        val gatt = bluetoothGatt
        val ch = writeCharacteristic
        if (gatt == null || ch == null) {
            Log.e("BLE", "Cannot send: GATT or characteristic is null")
            return
        }

        // если уже идёт запись — добавляем в очередь
        if (isWriting) {
            commandQueue.add(json)
            Log.d("BLE", "GATT busy, command queued: $json")
        } else {
            ch.value = json.toByteArray(Charsets.UTF_8)
            isWriting = true
            gatt.writeCharacteristic(ch)
            Log.d("BLE", "Command sent immediately: $json")
        }
    }
//    private fun mapDisconnectReason(status: Int): String {
//        return when (status) {
//            0 -> "GATT_SUCCESS"
//            8 -> "GATT_INSUFFICIENT_AUTHENTICATION"
//            19 -> "GATT_CONN_TERMINATE_LOCAL_HOST"
//            22 -> "GATT_CONN_TERMINATE_PEER_USER"
//            34 -> "GATT_CONN_TIMEOUT"
//            62 -> "GATT_CONN_FAIL_ESTABLISH"
//            133 -> "GATT_ERROR 133 "
//            else -> "Ошибка $status"
//        }
//    }
}

