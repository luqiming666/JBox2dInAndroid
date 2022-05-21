package jbox.wd.com.main

import android.content.res.AssetManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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

class DynamicBalls : AppCompatActivity(), SensorEventListener {

    private val TAG = "TESTING"
    private lateinit var binding: ActivityDynamicBallsBinding

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private lateinit var gimbal: Gimbal

    private var ballIndex = 0
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
        imageView.setTag(R.id.tag_view_last_rotation, imageView.rotation)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
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
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)}")
            if (isBall(viewIdA)) {
                if (isBall(viewIdB)) {
                    RichTapUtils.getInstance().playHaptic(heBallWithBall, 0)
                } else {
                    RichTapUtils.getInstance().playHaptic(heBallToBound, 0)
                    val view = binding.jboxContainer.findViewById<View>(viewIdA)
                    val boundCount = view.getTag(R.id.tag_view_contacted_bound_count) as Int + 1
                    view.setTag(R.id.tag_view_contacted_bound_count, boundCount)
                }
            } else {
                if (isBall(viewIdB)) {
                    RichTapUtils.getInstance().playHaptic(heBallToBound, 0)
                    val view = binding.jboxContainer.findViewById<View>(viewIdB)
                    val boundCount = view.getTag(R.id.tag_view_contacted_bound_count) as Int + 1
                    view.setTag(R.id.tag_view_contacted_bound_count, boundCount)
                }
            }
        }

        override fun onCollisionExited(viewIdA: Int, viewIdB: Int) {
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)} - exited")
            if (isBall(viewIdA)) {
                if (!isBall(viewIdB)) {
                    val view = binding.jboxContainer.findViewById<View>(viewIdA)
                    val boundCount = view.getTag(R.id.tag_view_contacted_bound_count) as Int - 1
                    view.setTag(R.id.tag_view_contacted_bound_count, boundCount)
                }
            } else {
                if (isBall(viewIdB)) {
                    val view = binding.jboxContainer.findViewById<View>(viewIdB)
                    val boundCount = view.getTag(R.id.tag_view_contacted_bound_count) as Int - 1
                    view.setTag(R.id.tag_view_contacted_bound_count, boundCount)
                }
            }
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

    private fun isBall(id: Int): Boolean {
        return id != R.id.physics_bound_left && id != R.id.physics_bound_right &&
            id != R.id.physics_bound_top && id != R.id.physics_bound_bottom
    }

    private fun getNormalizedVelocity(view: View): Int {
        var vel: Int = 10
        try {
            val body = view.getTag(R.id.wd_view_body_tag) as Body
            vel = (body.linearVelocity.normalize() / 5.0 * 255).toInt()
            if (vel > 255) vel = 255
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return vel
    }

    private fun monitorRollingBalls() {
        val childCount = binding.jboxContainer.childCount
        for (i in 0 until childCount) {
            val view = binding.jboxContainer.getChildAt(i)
            val isRolling = view.getTag(R.id.tag_view_is_rolling) as Boolean
            val boundCount = view.getTag(R.id.tag_view_contacted_bound_count) as Int
            if (boundCount > 0) {  // the ball is on one of edges
                val lastRotation = view.getTag(R.id.tag_view_last_rotation) as Float
                if (abs(view.rotation - lastRotation) > 0.01F) {
                    view.setTag(R.id.tag_view_last_rotation, view.rotation)
                    // The ball is rolling on the edge!
                    if (!isRolling) {
                        view.setTag(R.id.tag_view_is_rolling, true)
                        //val amplitude = getNormalizedVelocity(view)
                        RichTapUtils.getInstance().playHaptic(heBallRoll, -1, 100)
                    } else {
                        // Adjust amplitude based on ball's rolling speed
                        //RichTapUtils.getInstance().sendLoopParameter(amplitude, 0)
                    }
                } else {
                    // The ball has stopped rolling
                    view.setTag(R.id.tag_view_is_rolling, false)
                    if (isRolling) {
                        RichTapUtils.getInstance().stop() // TODO: only stop the rolling effect
                    }
                }
            } else {
                view.setTag(R.id.tag_view_is_rolling, false)
                if (isRolling) {
                    RichTapUtils.getInstance().stop() // TODO: only stop the rolling effect
                }
            }
        }
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