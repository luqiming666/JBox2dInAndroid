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
import jbox.wd.com.main.databinding.ActivityDynamicBallsBinding

class DynamicBalls : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDynamicBallsBinding

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDynamicBallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        repeat(2) {
            addOneBall()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        binding.btnAdd.setOnClickListener {
            addOneBall()
            binding.jboxContainer.onRandomChanged()
        }
        binding.btnMinus.setOnClickListener {
            binding.jboxContainer.run {
                RemoveOneBody()
                onRandomChanged()
            }
        }
    }

    private fun addOneBall() {
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        val imageView = ImageView(this)
        imageView.setImageResource(R.mipmap.share_tw)
        imageView.setTag(R.id.wd_view_circle_tag, true)
        binding.jboxContainer.addView(imageView, layoutParams)
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
            binding.jboxContainer.onSensorChanged(-x, y)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}