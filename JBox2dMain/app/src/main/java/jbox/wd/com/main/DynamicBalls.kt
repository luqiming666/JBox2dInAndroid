package jbox.wd.com.main

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.ImageView
import jbox.wd.com.main.Mobike.OnCollisionListener
import jbox.wd.com.main.databinding.ActivityDynamicBallsBinding

class DynamicBalls : AppCompatActivity(), SensorEventListener {

    private val TAG = "TESTING"
    private lateinit var binding: ActivityDynamicBallsBinding

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private var ballCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDynamicBallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        repeat(1) {
            addOneBall()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        binding.jboxContainer.setOnCollisionListener(onCollisionListener)
        binding.btnAdd.setOnClickListener {
            addOneBall()
            binding.jboxContainer.onRandomChanged()
        }
        binding.btnMinus.setOnClickListener {
            binding.jboxContainer.run {
                removeOneBody()
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
        imageView.id = ballCount++
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

    private val onCollisionListener = object : OnCollisionListener {
        override fun onCollisionEntered(viewIdA: Int, viewIdB: Int) {
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)}")
        }

        override fun onCollisionExited(viewIdA: Int, viewIdB: Int) {
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)} - exited")
        }
    }

    private fun getViewName(id: Int): String {
        return when (id) {
            R.id.physics_bound_left -> "Bound-left"
            R.id.physics_bound_right -> "Bound-right"
            R.id.physics_bound_top -> "Bound-top"
            R.id.physics_bound_bottom -> "Bound-bottom"
            else -> "Ball-$id"
        }
    }
}