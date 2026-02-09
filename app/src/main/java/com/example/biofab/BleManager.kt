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
import java.util.UUID
import android.content.Context

object BleManager {

    private const val TAG = "BLE"

    // UUID CCCD descriptor
    private val CCCD_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothGatt: BluetoothGatt? = null

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val handler = Handler(Looper.getMainLooper())

    private var listener: BleListener? = null

    private var isWriting = false

    private val commandQueue = mutableListOf<String>()

    var selectedDevice: BluetoothDevice? = null
        private set
    fun setDevice(device: BluetoothDevice?) {

        selectedDevice = device

    }

    val isConnected: Boolean
        get() = bluetoothGatt != null

    val isReady: Boolean
        get() = bluetoothGatt != null && writeCharacteristic != null

    interface BleListener {

        fun onConnecting() {}

        fun onConnected() {}

        fun onDisconnected() {}

        fun onReady() {}

        fun onMessageReceived(message: String) {}

        fun onError(error: String) {}
    }

    fun setListener(l: BleListener?) {
        listener = l
    }

    // ========================
    // CONNECT
    // ========================

    fun connect(context: Context, device: BluetoothDevice) {

        if (bluetoothGatt != null) {
            Log.d(TAG, "Already connected")
            return
        }

        selectedDevice = device

        listener?.onConnecting()

        bluetoothGatt =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(
                    context.applicationContext,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            } else {
                device.connectGatt(
                    context.applicationContext,
                    false,
                    gattCallback
                )
            }

        Log.d(TAG, "Connecting to ${device.address}")
    }


    // ========================
    // DISCONNECT
    // ========================

    fun disconnect() {

        bluetoothGatt?.let { gatt ->

            try {

                notifyCharacteristic?.let {

                    gatt.setCharacteristicNotification(it, false)

                    it.getDescriptor(CCCD_UUID)?.let { descriptor ->

                        descriptor.value =
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

                        gatt.writeDescriptor(descriptor)

                    }

                }

                gatt.disconnect()
                gatt.close()

            } catch (e: Exception) {

                Log.e(TAG, "Disconnect error", e)

            }

        }

        clear()

        listener?.onDisconnected()

        Log.d(TAG, "Disconnected")

    }


    private fun clear() {

        bluetoothGatt = null

        writeCharacteristic = null

        notifyCharacteristic = null

        commandQueue.clear()

        isWriting = false

        selectedDevice = null

    }


    // ========================
    // SEND COMMAND
    // ========================

    fun sendCommand(json: String) {

        val gatt = bluetoothGatt
        val ch = writeCharacteristic

        if (gatt == null || ch == null) {

            Log.e(TAG, "BLE not ready")

            listener?.onError("BLE not ready")

            return
        }

        if (isWriting) {

            commandQueue.add(json)

            Log.d(TAG, "Queued: $json")

            return
        }

        write(json)
    }


    private fun write(data: String) {

        val gatt = bluetoothGatt ?: return
        val ch = writeCharacteristic ?: return

        ch.value = data.toByteArray(Charsets.UTF_8)

        isWriting = true

        val success = gatt.writeCharacteristic(ch)

        Log.d(TAG, "Write start: $success data=$data")

    }


    private fun nextWrite() {

        if (commandQueue.isEmpty()) {

            isWriting = false

            return
        }

        val next = commandQueue.removeAt(0)

        write(next)

    }

    // ========================
    // GATT CALLBACK
    // ========================

    private val gattCallback =
        object : BluetoothGattCallback() {


            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                Log.d(TAG, "State change status=$status newState=$newState")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                val reason = mapDisconnectReason(status)
                Log.d(TAG, "Отключено: $reason")
            }

                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    bluetoothGatt = gatt

                    handler.post {
                        listener?.onConnected()
                    }

                    gatt.discoverServices()

                }

                else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    clear()

                    handler.post {
                        listener?.onDisconnected()
                    }

                }

            }



            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {

                if (status != BluetoothGatt.GATT_SUCCESS) {

                    listener?.onError("Service discovery failed")

                    return
                }

                var foundWrite: BluetoothGattCharacteristic? = null
                var foundNotify: BluetoothGattCharacteristic? = null

                for (service in gatt.services) {

                    for (ch in service.characteristics) {

                        val props = ch.properties

                        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0)
                            foundWrite = ch

                        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
                            foundNotify = ch

                    }

                }

                if (foundWrite == null || foundNotify == null) {

                    listener?.onError("Characteristics not found")

                    return
                }

                writeCharacteristic = foundWrite
                notifyCharacteristic = foundNotify

                enableNotifications()

            }


            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {

                if (status == BluetoothGatt.GATT_SUCCESS) {

                    handler.post {
                        listener?.onReady()
                    }

                    Log.d(TAG, "BLE READY")

                }
                else
                    Log.d(TAG, "Descriptor write failed: ${descriptor.uuid}, status=$status")

            }



            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {

                isWriting = false

                nextWrite()
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d("BLE", "Write OK: ${characteristic.uuid}")
//            } else {
//                Log.e("BLE", "Write FAILED: $status")
//            }

            }


            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {

                val text =
                    characteristic.value.toString(Charsets.UTF_8)

                handler.post {
                    listener?.onMessageReceived(text)
                }

            }

        }


    private fun enableNotifications() {

        val gatt = bluetoothGatt ?: return
        val ch = notifyCharacteristic ?: return

        gatt.setCharacteristicNotification(ch, true)

        val descriptor = ch.getDescriptor(CCCD_UUID)

        descriptor?.value =
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

        gatt.writeDescriptor(descriptor)

    }



//    var bluetoothGatt: BluetoothGatt? = null
//    var writeCharacteristic: BluetoothGattCharacteristic? = null
//    var notifyCharacteristic: BluetoothGattCharacteristic? = null
//    var selectedDevice: BluetoothDevice? = null
//    var isReady = false
//
//    val commandQueue = mutableListOf<String>()
//
//    private var gattCallback: BluetoothGattCallback? = null
//
//    /** Подключение к устройству, если еще нет активного соединения */
//    fun connect(device: BluetoothDevice, context: android.content.Context, callback: BluetoothGattCallback) {
//        if (bluetoothGatt != null) {
//            Log.e("BLE", "The BLE device already connected")
//            return
//        }
//        selectedDevice = device
//        gattCallback = callback
//        bluetoothGatt = device.connectGatt(context, false, callback,BluetoothDevice.TRANSPORT_LE)
//    }
//
//    /** Безопасное отключение */
//    fun disconnect() {
//        bluetoothGatt?.let { gatt ->
//            try {
//                notifyCharacteristic?.let { ch ->
//                    gatt.setCharacteristicNotification(ch, false)
//                    ch.getDescriptor(
//                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//                    )?.let { descriptor ->
//                        descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
//                        gatt.writeDescriptor(descriptor)
//                    }
//                }
//                gatt.disconnect()
//                gatt.close()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//        clear()
//    }
//
//    fun clear() {
//        bluetoothGatt = null
//        writeCharacteristic = null
//        notifyCharacteristic = null
//        selectedDevice = null
//        isReady = false
//        gattCallback = null
//    }
//
//    fun isConnected(): Boolean {
//        return bluetoothGatt != null
//    }
//
//    var isWriting = false
//
//
//    fun sendCommand(json: String) {
//        val gatt = bluetoothGatt
//        val ch = writeCharacteristic
//        if (gatt == null || ch == null) {
//            Log.e("BLE", "Cannot send: GATT or characteristic is null")
//            return
//        }
//
//        // если уже идёт запись — добавляем в очередь
//        if (isWriting) {
//            commandQueue.add(json)
//            Log.d("BLE", "GATT busy, command queued: $json")
//        } else {
//            ch.value = json.toByteArray(Charsets.UTF_8)
//            isWriting = true
//            gatt.writeCharacteristic(ch)
//            Log.d("BLE", "Command sent immediately: $json")
//        }
//    }

            private fun mapDisconnectReason(status: Int): String {
            return when (status) {
                0 -> "GATT_SUCCESS"
                8 -> "GATT_INSUFFICIENT_AUTHENTICATION"
                19 -> "GATT_CONN_TERMINATE_LOCAL_HOST"
                22 -> "GATT_CONN_TERMINATE_PEER_USER"
                34 -> "GATT_CONN_TIMEOUT"
                62 -> "GATT_CONN_FAIL_ESTABLISH"
                133 -> "GATT_ERROR 133 "
                else -> "Ошибка $status"
            }
        }
}

