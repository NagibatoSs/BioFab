package com.example.biofab

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

object BleDeviceStore {

    private const val PREFS_NAME = "MyAppPrefs"
    private const val KEY_NAME = "device_name"
    private const val KEY_ADDRESS = "device_address"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun save(context: Context, device: BluetoothDevice) {

        prefs(context).edit()
            .putString(KEY_NAME, device.name)
            .putString(KEY_ADDRESS, device.address)
            .apply()
    }

    fun load(context: Context): BluetoothDevice? {

        val address = prefs(context)
            .getString(KEY_ADDRESS, null)
            ?: return null

        return BluetoothAdapter
            .getDefaultAdapter()
            ?.getRemoteDevice(address)
    }

    fun clear(context: Context) {

        prefs(context).edit()
            .remove(KEY_NAME)
            .remove(KEY_ADDRESS)
            .apply()
    }

    fun hasDevice(context: Context): Boolean {

        return prefs(context).contains(KEY_ADDRESS)
    }
}