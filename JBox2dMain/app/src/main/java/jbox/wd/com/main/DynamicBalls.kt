package jbox.wd.com.main

import android.content.res.AssetManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.apprichtap.haptic.RichTapUtils
import com.commit451.gimbal.Gimbal
import jbox.wd.com.main.Mobike.OnCollisionListener
import jbox.wd.com.main.databinding.ActivityDynamicBallsBinding
import org.jbox2d.dynamics.Body
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

class DynamicBalls : AppCompatActivity(), SensorEventListener {

    private val TAG = "TESTING"
    private lateinit var binding: ActivityDynamicBallsBinding

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private lateinit var gimbal: Gimbal

    private var ballIndex = 0
    private val magicCode = 888F
    private var maxFallingVelocity = 26.5F
    private var maxRollingVelocity = 15.0F
    private val rollingMonitor = Timer()
    private lateinit var rollingMonitorTask: TimerTask

    // Haptic effect description files
    private lateinit var heBallRoll: String
    private lateinit var heBallToBound: String
    private lateinit var heBallWithBall: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gimbal = Gimbal(this)
        gimbal.lock()

        binding = ActivityDynamicBallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize RichTap SDK to play haptics
        RichTapUtils.getInstance().init(this)
        // Load haptics assets
        heBallRoll = loadHeFromAssets("roll.he")
        heBallToBound = loadHeFromAssets("wall.he")
        heBallWithBall = loadHeFromAssets("ball.he")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        binding.jboxContainer.setOnCollisionListener(onCollisionListener)
        repeat(2) {
            addOneBall()
        }

        binding.btnImpulse.setOnClickListener {
            binding.jboxContainer.onRandomChanged()
        }
        binding.btnAdd.setOnClickListener {
            if (binding.jboxContainer.childCount < 5) { // 5 balls at most
                addOneBall()
                binding.jboxContainer.requestLayout()
            }
        }
        binding.btnMinus.setOnClickListener {
            binding.jboxContainer.run {
                if (childCount > 1) {
                    removeOneBody()
                    requestLayout()
                }
            }
        }

        rollingMonitorTask = RollingBallMonitorTask(this)
        rollingMonitor.schedule(rollingMonitorTask, 0, 100)
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
        imageView.setTag(R.id.tag_view_last_rotation, magicCode)
        imageView.setTag(R.id.tag_view_is_rolling, false)
        imageView.setTag(R.id.tag_view_contacted_bound_count, 0)
        imageView.id = ballIndex++
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

    override fun onDestroy() {
        RichTapUtils.getInstance().stop()
        RichTapUtils.getInstance().quit()
        rollingMonitorTask.cancel()

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.about -> {
                AlertDialog.Builder(this).apply {
                    setTitle("About...")
                    setMessage("App Version: ${BuildConfig.VERSION_NAME}\n" +
                            "RichTap SDK: ${RichTapUtils.VERSION_NAME}")
                    setCancelable(true)
                    setPositiveButton("OK") { _, _ ->}
                    show()
                }
            }

            R.id.close -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gimbal.normalizeGravityEvent(event)
            binding.jboxContainer.changeWorldGravity(-event.values[0], event.values[1])
            //Log.i(TAG, "Sensor: ${-event.values[0]}, ${event.values[1]}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val onCollisionListener = object : OnCollisionListener {
        override fun onCollisionEntered(viewIdA: Int, viewIdB: Int) {
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)}, thread: ${Thread.currentThread().id}")
            if (isBall(viewIdA)) {
                if (isBall(viewIdB)) {
                    RichTapUtils.getInstance().playHaptic(heBallWithBall, 0)
                } else {
                    val view = binding.jboxContainer.findViewById<View>(viewIdA)
                    view.increaseBoundCount()
                    RichTapUtils.getInstance().playHaptic(heBallToBound, 0, view.getAmplitude())
                }
            } else {
                if (isBall(viewIdB)) {
                    val view = binding.jboxContainer.findViewById<View>(viewIdB)
                    view.increaseBoundCount()
                    RichTapUtils.getInstance().playHaptic(heBallToBound, 0, view.getAmplitude())
                }
            }
        }

        override fun onCollisionExited(viewIdA: Int, viewIdB: Int) {
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)} - exited")
            if (isBall(viewIdA)) {
                if (!isBall(viewIdB)) {
                    val view = binding.jboxContainer.findViewById<View>(viewIdA)
                    view.decreaseBoundCount()
                }
            } else {
                if (isBall(viewIdB)) {
                    val view = binding.jboxContainer.findViewById<View>(viewIdB)
                    view.decreaseBoundCount()
                }
            }
        }
    }

    fun View.increaseBoundCount() {
        synchronized(this@DynamicBalls) {
            val count = getTag(R.id.tag_view_contacted_bound_count) as Int + 1
            setTag(R.id.tag_view_contacted_bound_count, count)
        }
    }

    fun View.decreaseBoundCount() {
        synchronized(this@DynamicBalls) {
            val count = getTag(R.id.tag_view_contacted_bound_count) as Int - 1
            setTag(R.id.tag_view_contacted_bound_count, count)
        }
    }

    private fun View.isOnBound(): Boolean {
        synchronized(this@DynamicBalls) {
            val count = getTag(R.id.tag_view_contacted_bound_count) as Int
            return count > 0
        }
    }

    fun View.getAmplitude(): Int {
        val body = getTag(R.id.wd_view_body_tag) as Body
        val vel = body.linearVelocity.length()
        if (vel > maxFallingVelocity) maxFallingVelocity = vel
        return floor(vel / maxFallingVelocity * 255).toInt()
    }

    fun View.getAmplitude2(): Int {
        val body = getTag(R.id.wd_view_body_tag) as Body
        val vel = body.linearVelocity.length()
        if (vel > maxRollingVelocity) maxRollingVelocity = vel
        return floor(vel / maxRollingVelocity * 255).toInt()
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

    private fun isBall(id: Int): Boolean {
        return id != R.id.physics_bound_left && id != R.id.physics_bound_right &&
            id != R.id.physics_bound_top && id != R.id.physics_bound_bottom
    }

    private fun monitorRollingBalls() {
        Log.v(TAG, "monitorRollingBalls - enter, thread: ${Thread.currentThread().id}")
        val startTime = System.currentTimeMillis()

        val childCount = binding.jboxContainer.childCount
        for (i in 0 until childCount) {
            val view = binding.jboxContainer.getChildAt(i)
            val isRolling = view.getTag(R.id.tag_view_is_rolling) as Boolean
            if (view.isOnBound()) {
                val lastRotation = view.getTag(R.id.tag_view_last_rotation) as Float
                view.setTag(R.id.tag_view_last_rotation, view.rotation)
                if (abs(lastRotation - magicCode) < 0.01F) continue

                val delta = abs(view.rotation - lastRotation)
                //Log.v(TAG, "monitorRollingBalls - rotated: $delta")
                if (delta > 1.0F) { // set a threshold
                    //Log.v(TAG, "Rolling at velocity: ${view.getAmplitude2()}")
                    if (!isRolling) {
                        view.setTag(R.id.tag_view_is_rolling, true)
                        RichTapUtils.getInstance().playHaptic(heBallRoll, -1, view.getAmplitude2())
                    } else {
                        // Adjust amplitude based on ball's rolling speed
                        RichTapUtils.getInstance().sendLoopParameter(view.getAmplitude2(), 0)
                    }
                } else {
                    // The ball has stopped rolling
                    view.setTag(R.id.tag_view_is_rolling, false)
                    if (isRolling) {
                        RichTapUtils.getInstance().stop() // TODO: only stop the rolling effect
                    }
                }
            } else {
                view.setTag(R.id.tag_view_last_rotation, magicCode)
                view.setTag(R.id.tag_view_is_rolling, false)
                if (isRolling) {
                    RichTapUtils.getInstance().stop() // TODO: only stop the rolling effect
                }
            }
        }

        Log.v(TAG, "monitorRollingBalls - exit, time: ${System.currentTimeMillis()-startTime} ms")
    }

    class RollingBallMonitorTask(private val activity: DynamicBalls) : TimerTask() {
        override fun run() {
            activity.monitorRollingBalls()
        }
    }

    private fun loadHeFromAssets(fileName: String): String {
        val sb = StringBuilder()
        try {
            val stream = assets.open(fileName, AssetManager.ACCESS_STREAMING)
            val reader = BufferedReader(InputStreamReader(stream, "utf-8"))
            reader.use {
                reader.forEachLine {
                    sb.append(it)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }
}