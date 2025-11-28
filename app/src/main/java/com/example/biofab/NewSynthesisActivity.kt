package com.example.biofab

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import com.example.biofab.databinding.ActivityMainBinding
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
        binding.btnMenu.setOnClickListener {
            openMenu()
        }
        binding.btnNewSynthesis.setOnClickListener {
            startNewSynthesis()
        }

        binding.btnStop.setOnClickListener {
            stopSynthesis()
        }

        binding.btnPause.setOnClickListener {
            //pauseSynthesis()
        }
    }
    private fun openMenu(){
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun startNewSynthesis(){
        binding.btnNewSynthesis.isGone = true;
        binding.btnPause.isGone = false;
        binding.btnStop.isGone = false;
    }

    private fun stopSynthesis(){
        binding.btnNewSynthesis.isGone = false;
        binding.btnPause.isGone = true;
        binding.btnStop.isGone = true;
    }

}