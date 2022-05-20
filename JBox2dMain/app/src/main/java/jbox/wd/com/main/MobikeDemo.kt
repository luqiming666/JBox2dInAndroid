package jbox.wd.com.main

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.ImageView
import jbox.wd.com.main.databinding.ActivityMobikeDemoBinding

class MobikeDemo : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMobikeDemoBinding

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null

    private val imgs = intArrayOf(
        R.mipmap.share_fb,
        R.mipmap.share_kongjian,
        R.mipmap.share_pyq,
        R.mipmap.share_qq,
        R.mipmap.share_tw,
        R.mipmap.share_wechat,
        R.mipmap.share_weibo
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMobikeDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initView()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        binding.mobikeJboxJump.setOnClickListener {
            binding.mobikeJboxView.onRandomChanged()
        }
    }

    private fun initView() {
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        for (i in imgs.indices) {
            val imageView = ImageView(this)
            imageView.setImageResource(imgs[i])
            imageView.setTag(R.id.wd_view_circle_tag, true)
            binding.mobikeJboxView.addView(imageView, layoutParams)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1] * 2.0f
            binding.mobikeJboxView.onSensorChanged(-x, y)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}