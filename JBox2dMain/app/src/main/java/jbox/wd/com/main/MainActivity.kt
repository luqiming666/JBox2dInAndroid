package jbox.wd.com.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jbox.wd.com.main.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMobikeDemo.setOnClickListener {
            startActivity(Intent(this, MobikeDemo::class.java))
        }
        binding.btnDynamicBalls.setOnClickListener {
            startActivity(Intent(this, DynamicBalls::class.java))
        }

    }

}