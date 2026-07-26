package com.habitloop.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Wellness-only motion estimate. Raw accelerometer samples stay in memory and
 * are never stored or uploaded. This is deliberately not a respiratory vital.
 */
class BreathMotionDetector(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var baseline = 9.81f
    private var above = false
    private var lastPeakMs = 0L
    private var sampleCount = 0

    var breathCount by mutableIntStateOf(0)
        private set
    var signal by mutableFloatStateOf(0f)
        private set
    var quality by mutableStateOf("Place the phone gently against your upper chest")
        private set

    fun start() {
        breathCount = 0
        sampleCount = 0
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() = manager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val magnitude = sqrt(event.values.sumOf { (it * it).toDouble() }).toFloat()
        baseline = baseline * .985f + magnitude * .015f
        val deviation = magnitude - baseline
        signal = (abs(deviation) / .16f).coerceIn(0f, 1f)
        sampleCount++
        quality = when {
            signal > .85f -> "Too much movement—hold still"
            sampleCount < 80 -> "Calibrating motion…"
            else -> "Motion signal ready"
        }
        val now = System.currentTimeMillis()
        if (!above && deviation > .055f && now - lastPeakMs > 1800) {
            above = true
            lastPeakMs = now
            breathCount++
        } else if (above && deviation < .012f) {
            above = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
