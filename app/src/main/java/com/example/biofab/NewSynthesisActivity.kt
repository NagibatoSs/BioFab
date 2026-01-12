package com.example.biofab

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.example.biofab.databinding.ActivityNewSynthesisBinding

class NewSynthesisActivity : AppCompatActivity() {
    private var _binding: ActivityNewSynthesisBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding must not be null!")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityNewSynthesisBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false // белый текст
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
        binding.menuButton.setOnClickListener {
            openMenu()
        }
        binding.newSynthesisButton.setOnClickListener {
            startNewSynthesis()
        }

        binding.btnStop.setOnClickListener {
            stopSynthesis()
        }

        binding.btnPause.setOnClickListener {
            pauseSynthesis()
        }

        binding.btnResume.setOnClickListener {
            resumeSynthesis()
        }
        binding.btnCmd.setOnClickListener {
            commandSendingBtn()
        }
    }
    private fun openMenu(){
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun commandSendingBtn(){
        val cmd = binding.cmdInput.text.toString().trim()
        if (cmd.isNotEmpty()) {
            sendCommand(cmd)
            Toast.makeText(
                this,
                "Команда $cmd успешно отправлена",
                Toast.LENGTH_SHORT
            ).show()
            binding.cmdInput.text?.clear()
        }
        else {
            Toast.makeText(
                this,
                "В командной строке пусто, ничего не отправлено",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun startNewSynthesis(){
        binding.newSynthesisButton.isGone = true;
        binding.btnPause.isVisible = true;
        binding.btnStop.isGone = false;
        startSynthesisCommand()
    }

    private fun stopSynthesis(){
        binding.newSynthesisButton.isGone = false;
        binding.btnPause.isGone = true
        binding.btnStop.isGone = true;
        binding.btnResume.isGone = true
        stopSynthesisCommand()
    }

    private fun pauseSynthesis(){
        binding.newSynthesisButton.isGone = true;
        binding.btnPause.isGone = true;
        binding.btnStop.isGone = false;
        binding.btnResume.isGone = false;
        pauseSynthesisCommand()
    }

    private fun resumeSynthesis(){
        binding.btnResume.isGone = true;
        binding.btnPause.isGone = false
        resumeSynthesisCommand()
    }

    private fun sendCommandJson(json: String) {
        val gatt = BleManager.bluetoothGatt
        val ch = BleManager.writeCharacteristic

        if (gatt == null || ch == null) {
            Toast.makeText(this, "BLE не готово для отправки команды", Toast.LENGTH_SHORT).show()
            Log.e("BLE", "Cannot send: GATT or characteristic is null")
            return
        }

        ch.value = json.toByteArray(Charsets.UTF_8)

        val success = gatt.writeCharacteristic(ch)
        Log.d("BLE", "Write start: $success, data=$json")
    }

    private fun startSynthesisCommand() {
        sendCommandJson("""{"cmd":"start"}""")
    }

    private fun stopSynthesisCommand() {
        sendCommandJson("""{"cmd":"stop"}""")
    }

    private fun pauseSynthesisCommand() {
        sendCommandJson("""{"cmd":"pause"}""")
    }

    private fun resumeSynthesisCommand() {
        sendCommandJson("""{"cmd":"resume"}""")
    }

    private  fun sendCommand(cmd: String) {
        sendCommandJson("""{"cmd":"$cmd"}""")
    }

}