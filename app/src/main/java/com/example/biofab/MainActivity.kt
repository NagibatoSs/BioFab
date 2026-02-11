package com.example.biofab
import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    //Биндинги
    private var _binding: ActivityMainBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding must not be null!")

    //Для UI
    private  var connectedUI: Boolean = false

    //BLE
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bleScanner: BluetoothLeScanner? = null

    // Для поиска подключений и хранения
    private val scannedDevices = mutableListOf<BluetoothDevice>()
    private var deviceDialog: AlertDialog? = null
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private var deviceList = mutableListOf<BluetoothDevice>()

    private fun connectToSelectedDevice() {
        val device = BleManager.selectedDevice
        if (device == null) {
            Log.e("BLE", "No selected device!")
            return
        }

        if (!BleManager.isConnected) {
            BleManager.connect(this, device)
            Log.d("BLE", "Connecting to ${device.address}")
        } else {
            Log.d("BLE", "Already connected to ${device.address}")
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bluetooth init
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner

        // load saved device

        val device = BleDeviceStore.load(this)

        if (device != null) {

            BleManager.setDevice(device)

            Log.d("BLE", "Already connected to ${device.address}")

            connectToSelectedDevice()

        }
        if (BleManager.selectedDevice != null)
        {
            setConnectedUi()
        }

        BleManager.setListener(object : BleManager.BleListener {

            override fun onConnecting() {
                binding.connectionStatusText.text = "Connecting..."
            }

            override fun onConnected() {
                setConnectedUi()
            }

            override fun onReady() {
                setConnectedUi()
            }

            override fun onDisconnected() {

                binding.connectionStatusText.text = "Disconnected"
                stateUiDisconnected()

            }

            override fun onMessageReceived(message: String) {
                Log.d("BLE", message)
            }

            override fun onError(error: String) {
                Log.e("BLE", error)
            }

        })

        //События
        binding.connectionButton.setOnClickListener {
            connectMachine()
        }

        binding.menuButton.setOnClickListener {
            openMenu()
        }

        binding.newSynthesisButton.setOnClickListener {
            val intent = Intent(this, NewSynthesisActivity::class.java)
            startActivity(intent)
        }

        binding.rightMenuMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.rightMenuTraining.setOnClickListener {
            val intent = Intent(this, TrainingActivity::class.java)
            startActivity(intent)
        }
        binding.rightMenuSynthesis.setOnClickListener {
            val intent = Intent(this, NewSynthesisActivity::class.java)
            startActivity(intent)
        }
        binding.rightMenuInfoContacts.setOnClickListener {
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
            BleManager.setDevice(dev)

            Log.d("BLE", "Selected device: ${dev.name} - ${dev.address}")
            //binding.connectionStatusText.text = "Подключено: ${dev.name ?: "Unknown"}"
            binding.connectionStatusText.text =
                "Выбрано: ${dev.name ?: "Unknown"}"

//            saveSelectedDevice(dev)
            // сохраняем устройство локально
            BleDeviceStore.save(this,dev)

            bleScanner?.stopScan(bleScanCallback)
            deviceDialog?.dismiss()

            connectToSelectedDevice()
            //UI
        }
    }

//    private fun stateUiConnected() {
//        val drawable = binding.connectionStatusCircle.background.mutate() as GradientDrawable
//        drawable.setColor(ContextCompat.getColor(this, R.color.green))
//        binding.connectionButton.text = "Отключить"
//        val drawableBtn = binding.connectionStatusCircle.background.mutate() as GradientDrawable
//        drawableBtn.setColor(ContextCompat.getColor(this, R.color.textSecondary))
//        connectedUI = true
//    }

    private  fun stateUiDisconnected() {
        binding.connectionStatusText.text = "Не подключено"
        val drawable = binding.connectionStatusCircle.background.mutate() as GradientDrawable
        drawable.setColor(ContextCompat.getColor(this, R.color.red))
        binding.connectionButton.text = "Подключить"
        val drawableBtn = binding.connectionButton.background.mutate() as GradientDrawable
        drawableBtn.setColor(ContextCompat.getColor(this, R.color.cyan))
        connectedUI = false
    }

    private fun showDeviceSelectionDialog() {
        if (scannedDevices.isEmpty()) {
            Log.d("BLE", "No devices found to show")
            return
        }

        val deviceNames = scannedDevices.map { d ->
            (d.name ?: "Unknown") + " (${d.address})"
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите устройство")

        builder.setItems(deviceNames.toTypedArray()) { dialog, which ->
            BleManager.setDevice(scannedDevices[which])

            Log.d("BLE", "Selected: ${BleManager.selectedDevice?.name} - ${BleManager.selectedDevice?.address}")

            binding.connectionStatusText.text = "Подключено ${BleManager.selectedDevice?.address}"
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

        val granted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!granted) {
            Toast.makeText(this@MainActivity, "Нет разрешений, они должны сейчас запросится", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this@MainActivity, "Bluetooth не включен", Toast.LENGTH_LONG).show()
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

    private fun setConnectedUi(){
        val device = BleManager.selectedDevice
        binding.connectionStatusText.text = "Подключено: ${device?.name ?: "Unknown"}"
        val drawable = binding.connectionStatusCircle.background.mutate() as GradientDrawable
        drawable.setColor(ContextCompat.getColor(this@MainActivity, R.color.green))
        binding.connectionButton.text = "Отключить"
        val drawableBtn = binding.connectionButton.background.mutate() as GradientDrawable
        drawableBtn.setColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
        connectedUI = true
    }

    private fun disconnectDevice() {
        if (BleManager.isConnected) {
            BleManager.disconnect()
            Log.d("BLE", "Device disconnected")
        } else {
            Log.d("BLE", "No device to disconnect")
        }
    }

    private fun connectMachine(){
        if (!BleManager.isConnected)
        {
            deviceList.clear()
            if (::deviceAdapter.isInitialized)
                deviceAdapter.clear()
            showDynamicDeviceDialog()
            startBleScan()
        }
        else
        {
            BleDeviceStore.clear(this)
            disconnectDevice()
            BleManager.setDevice(null)
        }

    }

}
