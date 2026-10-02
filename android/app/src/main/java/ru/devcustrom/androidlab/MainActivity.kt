package ru.devcustrom.androidlab

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.devcustrom.androidlab.databinding.ActivityMainBinding
import ru.devcustrom.androidlab.lab1.Lab1Activity
import ru.devcustrom.androidlab.lab2.Lab2Activity

/**
 * Стартовый экран.
 *
 * Сейчас на нём кнопки на Лабы 1 и 2 — в Лабе 3 здесь появится список всех
 * лабораторных работ.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openLab1Button.setOnClickListener {
            startActivity(Intent(this, Lab1Activity::class.java))
        }

        binding.openLab2Button.setOnClickListener {
            startActivity(Intent(this, Lab2Activity::class.java))
        }
    }
}