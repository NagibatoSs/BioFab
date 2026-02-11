package com.example.biofab

import ChipAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
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
import androidx.core.widget.doAfterTextChanged
import com.example.biofab.databinding.ActivityNewSynthesisBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import android.text.TextWatcher
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged

private const val DEFAULT_VALUE_AMINO_COUNT = 1
private const val DEFAULT_VALUE_MASS_COUNT = 500
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


        binding.aminoCountSlider.value = DEFAULT_VALUE_AMINO_COUNT.toFloat()
        binding.aminoCountNumber.setText(DEFAULT_VALUE_AMINO_COUNT.toString())

        binding.massCountSlider.value = DEFAULT_VALUE_MASS_COUNT.toFloat()
        binding.massCountNumber.setText(DEFAULT_VALUE_MASS_COUNT.toString())

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
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
        binding.menuButton.setOnClickListener {
            openMenu()
        }
        binding.newSynthesisButton.setOnClickListener {
            hideStartParamsContainer()
            startNewSynthesis()
        }

        binding.btnStop.setOnClickListener {
            stopSynthesis()
            showStartParamsContainer()
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

        binding.aminoCountSlider.apply {
            valueFrom = 1f
            valueTo = 6f
            stepSize = 1f
            value = DEFAULT_VALUE_AMINO_COUNT.toFloat()   // ← ОБЯЗАТЕЛЬНО
        }

        binding.aminoCountSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.aminoCountNumber.setText(value.toInt().toString())
            }
        }

        binding.aminoCountNumber.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: return@doAfterTextChanged
            val number = text.toIntOrNull()
            if (number != null && number in 1..6) {
                binding.aminoCountSlider.value = number.toFloat()
            }
        }
        binding.aminoCountNumber.filters = arrayOf(
            InputFilter { source, start, end, dest, dstart, dend ->

                val newText = dest.substring(0, dstart) +
                        source.substring(start, end) +
                        dest.substring(dend)

                val value = newText.toIntOrNull()

                if (value == null || value < 1 || value > 6) {
                    ""
                } else {
                    null
                }
            }
        )
        binding.aminoCountNumber.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(0, v.top)
                }
            }
        }

//        binding.aminoCountSlider.addOnChangeListener { _, value, fromUser ->
//            if (fromUser) {
//                binding.aminoCountNumber.setText(value.toInt().toString())
//            }
//        }
//        binding.aminoCountNumber.doAfterTextChanged {
//            val number = it.toString().toIntOrNull() ?: return@doAfterTextChanged
//            binding.aminoCountSlider.value = number.toFloat()
//        }
//        binding.aminoCountNumber.filters = arrayOf(
//            InputFilter { source, start, end, dest, dstart, dend ->
//                val newText = dest.toString().substring(0, dstart) +
//                        source.toString().substring(start, end) +
//                        dest.toString().substring(dend)
//                val value = newText.toIntOrNull()
//                if (value == null || value < 1 || value > 6) {
//                    ""
//                } else {
//                    null
//                }
//            }
//        )


        binding.massCountSlider.apply {
            valueFrom = 200f
            valueTo = 1000f
            stepSize = 100f
            value = DEFAULT_VALUE_MASS_COUNT.toFloat()
        }

        binding.massCountSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.massCountNumber.setText(value.toInt().toString())
            }
        }

        binding.massCountNumber.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: return@doAfterTextChanged
            val number = text.toIntOrNull() ?: return@doAfterTextChanged
            if (number in 0..1000) {
                val sliderStep = 200
                var sliderValue = (number / sliderStep) * sliderStep
                // Ограничиваем слайдер в его диапазоне
                sliderValue = sliderValue.coerceIn(binding.massCountSlider.valueFrom.toInt(), binding.massCountSlider.valueTo.toInt())
                binding.massCountSlider.value = sliderValue.toFloat()
            }
        }
        binding.massCountNumber.filters = arrayOf(
            InputFilter { source, start, end, dest, dstart, dend ->

                val newText = dest.substring(0, dstart) +
                        source.substring(start, end) +
                        dest.substring(dend)

                val value = newText.toIntOrNull()

                if (value == null || value < 0 || value > 1000) {
                    ""
                } else {
                    null
                }
            }
        )


//        binding.massCountNumber.doAfterTextChanged {
//            val number = it.toString().toIntOrNull() ?: return@doAfterTextChanged
//            binding.massCountSlider.value = number.toFloat()
//        }
//        binding.massCountNumber.filters = arrayOf(
//            InputFilter { source, _, _, dest, _, _ ->
//                val newText = dest.toString() + source.toString()
//                val value = newText.toIntOrNull()
//                if (value == null || value < 200 || value > 1000) {
//                    ""
//                } else {
//                    null
//                }
//            }
//        )

        val recyclerView = findViewById<RecyclerView>(R.id.motorTypesRecycler)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 3 колонки

        val items = listOf("DCM", "TFA", "TEA", "DIC", "OUT", "MIX")
        val adapter = ChipAdapter(items) { selected ->
            Log.d("ChipSelected", "Выбрано: $selected")
        }
        recyclerView.adapter = adapter

        binding.casseteInput.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: return@doAfterTextChanged
            if (text.isEmpty()) return@doAfterTextChanged

            val value = text.toIntOrNull() ?: return@doAfterTextChanged

            if (value !in 1..10) {
                binding.casseteInput.setText("10")
            }
        }

        binding.motorMlInput.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: return@doAfterTextChanged
            if (text.isEmpty()) return@doAfterTextChanged

            val value = text.toIntOrNull() ?: return@doAfterTextChanged

            if (value !in 1..100) {
                binding.motorMlInput.setText("100")
                binding.motorMlInput.setSelection(3)
            }
        }
    }

    private fun hideStartParamsContainer(){
        binding.startParamsContainer.isGone = true;
    }
    private fun showStartParamsContainer(){
        binding.startParamsContainer.isGone = false;
    }

    private fun hidePauseParamsContainer(){
        binding.pauseParamsContainer.isGone = true;
    }
    private fun showPauseParamsContainer(){
        binding.pauseParamsContainer.isGone = false;
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
        binding.newSynthesisButton.isGone = false
        binding.btnPause.isGone = true
        binding.btnStop.isGone = true
        binding.btnResume.isGone = true
        stopSynthesisCommand()
        hidePauseParamsContainer()
    }

    private fun pauseSynthesis(){
        binding.newSynthesisButton.isGone = true
        binding.btnPause.isGone = true
        binding.btnStop.isGone = false
        binding.btnResume.isGone = false
        pauseSynthesisCommand()
        showPauseParamsContainer()
    }

    private fun resumeSynthesis(){
        binding.btnResume.isGone = true
        binding.btnPause.isGone = false
        resumeSynthesisCommand()
        hidePauseParamsContainer()
    }

    private fun startSynthesisCommand() {
        BleManager.sendCommand("""{"cmd":"start"}""")
        BleManager.sendCommand("""{"aa":"${binding.aminoCountSlider.value.toInt()}"}""")
        BleManager.sendCommand("""{"mass":"${binding.massCountSlider.value.toInt()}"}""")
    }

    private fun stopSynthesisCommand() {
        BleManager.sendCommand("""{"cmd":"stop"}""")
    }

    private fun pauseSynthesisCommand() {
        BleManager.sendCommand("""{"cmd":"pause"}""")
    }

    private fun resumeSynthesisCommand() {
        BleManager.sendCommand("""{"cmd":"resume"}""")
    }

    private  fun sendCommand(cmd: String) {
        BleManager.sendCommand("""{"cmd":"$cmd"}""")
    }

}