package com.example.biofab
import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.biofab.databinding.ActivityMainBinding
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.MainScope
import java.util.UUID
import javax.security.auth.callback.Callback
import kotlin.collections.forEach

class MainActivity : AppCompatActivity() {

    private  var isConnected: Boolean = false
    private var _binding: ActivityMainBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding must not be null!")

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bleScanner: BluetoothLeScanner? = null

    // Для хранения найденных устройств
    private val scannedDevices = mutableListOf<BluetoothDevice>()

    private var deviceDialog: AlertDialog? = null
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private var deviceList = mutableListOf<BluetoothDevice>()

    //private val WRITE_SERVICE_UUID = UUID.fromString("12345678-0000-1000-8000-00805f9b34fb")
    //private val WRITE_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    //private val WRITE_CHAR_UUID    = UUID.fromString("12345678-0000-1000-8000-00805f9b34ff")

    //private val WRITE_CHAR_UUID    = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    //private var bluetoothGatt: BluetoothGatt? = null


    //private val TARGET_MANUFACTURER_ID = 0x1234

    private fun connectToSelectedDevice() {
        val device = BleManager.selectedDevice
        if (device == null) {
            Log.e("BLE", "No selected device!")
            return
        }

        if (!BleManager.isConnected()) {
            BleManager.connect(device, this, gattCallback)
            Log.d("BLE", "Connecting to ${device.address}")
        } else {
            Log.d("BLE", "Already connected to ${device.address}")
            stateUiConnected()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "Write OK: ${characteristic.uuid}")
            } else {
                Log.e("BLE", "Write FAILED: $status")
            }
        }
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.e("BLE", "STATE CHANGE: status=$status, newState=$newState")

            // Если произошла ошибка → показать тост
            if (status != BluetoothGatt.GATT_SUCCESS) {
                val reason = mapDisconnectReason(status)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Отключено: $reason", Toast.LENGTH_LONG).show()
                }
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "Connected to GATT server")
                runOnUiThread {
                    binding.tvConnection.text = "Подключаемся..."
                }
                gatt.discoverServices()
            }

            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("BLE", "Disconnected: status=$status")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Disconnected from GATT server", Toast.LENGTH_SHORT).show()
                }
                Log.d("BLE", "Disconnected from GATT server")
                BleManager.bluetoothGatt = null
                BleManager.isReady = false

                runOnUiThread {
                    binding.tvConnection.text = "Отключено"
                    stateUiDisconnected()
                }
            }
        }
        private fun mapDisconnectReason(status: Int): String {
            return when (status) {
                0 -> "GATT_SUCCESS (нормальное завершение)"
                8 -> "GATT_INSUFFICIENT_AUTHENTICATION (нет прав)"
                19 -> "GATT_CONN_TERMINATE_LOCAL_HOST (приложение разорвало)"
                22 -> "GATT_CONN_TERMINATE_PEER_USER (устройство закрыло соединение)"
                34 -> "GATT_CONN_TIMEOUT (таймаут)"
                62 -> "GATT_CONN_FAIL_ESTABLISH (не удалось установить соединение)"
                133 -> "GATT_ERROR 133 (классическая BLE ошибка Android)"
                else -> "Ошибка $status"
            }
        }
//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            if (status != BluetoothGatt.GATT_SUCCESS) return
//
//            Log.d("BLE", "Services discovered")
//
//            val service = gatt.getService(WRITE_SERVICE_UUID)
//            if (service == null) {
//                Log.e("BLE", "Write service NOT found!")
//                return
//            }
//
//            val writeChar = service.getCharacteristic(WRITE_CHAR_UUID)
//            if (writeChar == null) {
//                Log.e("BLE", "Write characteristic NOT found!")
//                return
//            }
//            gatt.setCharacteristicNotification(writeChar, true)
//
//            val cccd = writeChar.getDescriptor(
//                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//            )
//
//            if (cccd != null) {
//                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(cccd)
//                Log.d("BLE", "CCCD write started")
//            } else {
//                Log.e("BLE", "CCCD descriptor NOT found!")
//            }
//
//            // сохраняем характеристику
//            BleManager.writeCharacteristic = writeChar
//            BleManager.bluetoothGatt = gatt
//
//            // тип записи – самый надёжный
//            writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
//
//            BleManager.isReady = true
//
//            runOnUiThread {
//                binding.tvConnection.text = "Подключено"
//                stateUiConnected()
//            }
//
//            Log.d("BLE", "WRITE characteristic FOUND and ready")
//        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("BLE", "onServicesDiscovered called")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            var foundWrite: BluetoothGattCharacteristic? = null
            var foundNotify: BluetoothGattCharacteristic? = null


            for (service in gatt.services) {
                for (ch in service.characteristics) {
                    val props = ch.properties
                    if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                        foundWrite = ch
                    }
                    if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        foundNotify = ch
                        gatt.setCharacteristicNotification(ch, true)
                        val descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            Thread.sleep(200)
                            //gatt.writeDescriptor(it)
                        }
                    }
                }
            }

            if (foundWrite != null && foundNotify != null) {
                BleManager.writeCharacteristic = foundWrite
                BleManager.notifyCharacteristic = foundNotify

                BleManager.writeCharacteristic = foundWrite.apply {
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                BleManager.notifyCharacteristic = foundNotify
                gatt.setCharacteristicNotification(foundNotify, true)
                val descriptor = foundNotify.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
                //writeCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
//                BleManager.writeCharacteristic?.writeType =
//                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                //BleManager.isReady = true
                runOnUiThread {
                    binding.tvConnection.text = "Подключено"
                    stateUiConnected()
                }
                Toast.makeText(this@MainActivity, "Характеристики write и notify найдены", Toast.LENGTH_SHORT).show()
                Log.d("BLE", "Write and Notify characteristics FOUND")
            } else {
                Toast.makeText(this@MainActivity, "Характеристики write и notify не найдены", Toast.LENGTH_LONG).show()
                Log.e("BLE", "Characteristics not found")
            }
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val bytes = characteristic.value
            val text = bytes.toString(Charsets.UTF_8)

            Log.d("BLE", "=== NOTIFICATION RECEIVED ===")
            Log.d("BLE", "RAW BYTES: ${bytes.joinToString()}")
            Log.d("BLE", "AS TEXT: $text")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            runOnUiThread {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "Descriptor write success: ${descriptor.uuid}")
                    //binding.text.text = "Descriptor write success: ${descriptor.uuid}"
                    Toast.makeText(this@MainActivity, "Descriptor write success: ${descriptor.uuid}", Toast.LENGTH_SHORT).show()
                    BleManager.isReady = true
                } else {
                    Toast.makeText(this@MainActivity, "Descriptor write failed: ${descriptor.uuid}, status=$status", Toast.LENGTH_SHORT).show()
                    Log.e("BLE", "Descriptor write failed: ${descriptor.uuid}, status=$status")
                }
            }
        }
    }


    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address ?: return
            //val name = device.name ?: return

            // Если нет Manufacturer Data → не наше устройство
            val record = result.scanRecord ?: return
            //val manufacturerData = record.getManufacturerSpecificData(TARGET_MANUFACTURER_ID)
            //if (manufacturerData == null) return

            //if (device == null || device.address == null) return
            // Фильтр по RSSI
            if (result.rssi < -85) return

            // Фильтр по connectable
            if (!result.isConnectable) return

            // Если новое
            if (deviceList.none { it.address == device.address }) {
                deviceList.add(device)

                val name = device.name ?: "Unknown"
                deviceAdapter.add("$name (${device.address})")

                Log.d("BLE", "Added: $name - ${device.address}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "Scan failed: $errorCode")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false // белый текст
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация Bluetooth
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner

        BleManager.selectedDevice = loadSelectedDevice()
//        BleManager.selectedDevice?.let {
//            connectToSelectedDevice() // автоматическое подключение при старте
//        }
        BleManager.selectedDevice?.let { device ->
            if (BleManager.isConnected()) {
                stateUiConnected()
                Log.d("BLE", "Already connected to ${device.address}")
            } else {
                connectToSelectedDevice()
            }
        }

        //События
        binding.btnConnection.setOnClickListener {
            //startBleScan()
            //connectMachine()
            connectMachine()
        }

        binding.btnMenu.setOnClickListener {
            openMenu()
        }

        binding.btnNewSynthesis.setOnClickListener {
            val intent = Intent(this, NewSynthesisActivity::class.java)
            //sendStart()
            startActivity(intent)
        }

        binding.dashboard.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.training.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            startActivity(intent)
        }
        binding.synthesis.setOnClickListener {
            val intent = Intent(this, NewSynthesisActivity::class.java)
            startActivity(intent)
        }
        binding.infoContacts.setOnClickListener {
            val intent = Intent(this, InfoContactsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDynamicDeviceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите устройство")


        val listView = ListView(this)
        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = deviceAdapter

        builder.setView(listView)

        builder.setNegativeButton("Отмена") { dialog, _ ->
            bleScanner?.stopScan(bleScanCallback)
            dialog.dismiss()
        }

        deviceDialog = builder.create()

        deviceDialog?.setCanceledOnTouchOutside(false)

        deviceDialog?.show()

        // Клик по устройству
        listView.setOnItemClickListener { _, _, position, _ ->
            val dev = deviceList[position]
            BleManager.selectedDevice = dev

            Log.d("BLE", "Selected device: ${dev.name} - ${dev.address}")
            binding.tvConnection.text = "Выбрано: ${dev.name ?: "Unknown"}"

            saveSelectedDevice(dev)   // сохраняем устройство локально

            bleScanner?.stopScan(bleScanCallback)
            deviceDialog?.dismiss()

            connectToSelectedDevice()
            //UI
            stateUiConnected()
        }
    }
    private fun sendCommandJson(json: String) {
        val gatt = BleManager.bluetoothGatt
        val ch = BleManager.writeCharacteristic

        if (gatt == null || ch == null || !BleManager.isReady) {
            Log.w("BLE", "Cannot send: BLE not ready")
            return
        }

        ch.value = json.toByteArray(Charsets.UTF_8)
        gatt.writeCharacteristic(ch)
        Log.d("BLE", "Sent JSON: $json")
    }

    private fun sendStart() {
        sendCommandJson("""{"cmd":"start"}""")
    }

    private fun sendStop() {
        sendCommandJson("""{"cmd":"stop"}""")
    }

    private fun stateUiConnected() {
        val drawable = binding.imConnectionCircle.background.mutate() as GradientDrawable
        drawable.setColor(ContextCompat.getColor(this, R.color.green))
        binding.btnConnection.text = "Отключить"
        val drawableBtn = binding.btnConnection.background.mutate() as GradientDrawable
        drawableBtn.setColor(ContextCompat.getColor(this, R.color.textSecondary))
        isConnected = true
    }

    private  fun stateUiDisconnected() {
        binding.tvConnection.text = "Не подключено"
        val drawable = binding.imConnectionCircle.background.mutate() as GradientDrawable
        drawable.setColor(ContextCompat.getColor(this, R.color.red))
        binding.btnConnection.text = "Подключить"
        val drawableBtn = binding.btnConnection.background.mutate() as GradientDrawable
        drawableBtn.setColor(ContextCompat.getColor(this, R.color.iconsBiruza))

        isConnected = false
    }

    private fun showDeviceSelectionDialog() {
        if (scannedDevices.isEmpty()) {
            Log.d("BLE", "No devices found to show")
            return
        }

        val deviceNames = scannedDevices.map { d ->
            (d.name ?: "Unknown") + " (${d.address})"
        }

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Выберите устройство")

        builder.setItems(deviceNames.toTypedArray()) { dialog, which ->
            BleManager.selectedDevice = scannedDevices[which]

            Log.d("BLE", "Selected: ${BleManager.selectedDevice?.name} - ${BleManager.selectedDevice?.address}")

            // Тут позже будет подключение
            binding.tvConnection.text = "Подключено ${BleManager.selectedDevice?.address}"
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun startBleScan() {
        // Проверка разрешений
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        Toast.makeText(this@MainActivity, "Проверка разрешений", Toast.LENGTH_LONG).show()
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!granted) {
            Toast.makeText(this@MainActivity, "Нет разрешений, они должны сейчас запросится", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
            return
        }

        // Проверяем, включен ли Bluetooth
        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this@MainActivity, "Блютуз не включен, сейчас должно попросить включить", Toast.LENGTH_LONG).show()
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
            return
        }

        scannedDevices.clear()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)  //Без нее не находит даже устройства
            .build()

        bleScanner?.startScan(null, scanSettings, bleScanCallback)
        Log.d("BLE", "BLE scan started")

        //Смотрит время и если прошло 5 секунд останавливает скан
        binding.root.postDelayed({
            bleScanner?.stopScan(bleScanCallback)
            Log.d("BLE", "BLE scan stopped")
            Log.d("BLE", "Devices found: ${scannedDevices.size}")
            showDeviceSelectionDialog()
        }, 5000)
    }

    //вроде юзлес
    private fun checkBlePermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    //и это
    private fun requestBlePermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
    }
    //ЧЕ такое
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBleScan()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        bleScanner?.stopScan(bleScanCallback)
    }

    private fun openMenu(){
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }
    //Загружает из памяти сохранненный девайс
    private fun loadSelectedDevice(): BluetoothDevice? {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val name = sharedPref.getString("device_name", null)
        val address = sharedPref.getString("device_address", null)

        if (address != null) {
            val device = bluetoothAdapter.getRemoteDevice(address)
            binding.tvConnection.text = "Подключено ${name ?: "Unknown"}"
            stateUiConnected()
            return device
        }
        return null
    }

    //Сохраняет в память сохраненный девайс
    private fun saveSelectedDevice(device: BluetoothDevice) {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("device_name", device.name)
            putString("device_address", device.address)
            apply()
        }
    }

    private fun disconnectDevice() {
        if (BleManager.isConnected()) {
            BleManager.disconnect()
            stateUiDisconnected()
            Log.d("BLE", "Device disconnected")
        } else {
            Log.d("BLE", "No device to disconnect")
        }
    }

    private fun connectMachine(){
        if (!BleManager.isConnected())
        {
            deviceList.clear()
            if (::deviceAdapter.isInitialized) deviceAdapter.clear()

            showDynamicDeviceDialog()     // показываем окно сразу
            startBleScan()                // начинаем скан
        }
        else
        {
            stateUiDisconnected()

            val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                remove("device_name")
                remove("device_address")
                apply()
            }

            disconnectDevice()
            BleManager.selectedDevice = null
        }

    }

}
