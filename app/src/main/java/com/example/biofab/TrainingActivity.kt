package com.example.biofab

import FaqAdapter
import FaqItem
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.biofab.databinding.ActivityTrainingBinding
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager

class TrainingActivity : AppCompatActivity() {
    private var _binding: ActivityTrainingBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding must not be null!")

    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTrainingBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false // белый текст
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //FAQ
        setFaq()

        val playerView = findViewById<PlayerView>(R.id.playerView)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // видео из res/raw/myvideo.mp4
        val mediaItem = MediaItem.fromUri("https://storage.yandexcloud.net/biofab/peptides1.mp4")
        player.setMediaItem(mediaItem)

        player.prepare()
        player.playWhenReady = false

        binding.menuButton.setOnClickListener {
            openMenu()
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
        binding.btnVideos.setOnClickListener {
            binding.constraintLayoutVideos.isVisible = true
            binding.recyclerView.isGone = true;
            binding.btnVideos.setBackgroundColor(ContextCompat.getColor(this, R.color.cyan))
            binding.btnFAQ.setBackgroundColor(ContextCompat.getColor(this, R.color.textSecondary))
        }
        binding.btnFAQ.setOnClickListener {
            binding.constraintLayoutVideos.isGone = true;
            binding.recyclerView.isVisible = true
            binding.btnFAQ.setBackgroundColor(ContextCompat.getColor(this, R.color.cyan))
            binding.btnVideos.setBackgroundColor(ContextCompat.getColor(this, R.color.textSecondary))
        }
    }
    override fun onStop() {
        super.onStop()
        player.release()
    }
    private fun openMenu(){
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setFaq(){
        val list = listOf(
            FaqItem("Почему не подключается к синтезатору?", "Проверьте питание и Bluetooth синтезатора, а также разрешение на телефоне."),
            FaqItem("Как начать синтез?", "В разделе «Синтез» нажмите «Начать синтез». Или нажмите «Новый синтез» на главной странице."),
            FaqItem("Где найти инструкцию?", "Смотрите раздел «Обучение»."),
            FaqItem("Сколько длится синтез?", "Время зависит от протокола, указывается перед запуском."),
            FaqItem("Как остановить синтез?", "В разделе «Синтез» нажмите кнопку «Стоп» для полного завершения процесса или «Пауза» для приостановки процесса синтеза."),
            FaqItem("Почему не приходят уведомления?", "Проверьте настройки телефона и приложения.")
        )

        val adapter = FaqAdapter(list)

        val rv = binding.recyclerView
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }
}