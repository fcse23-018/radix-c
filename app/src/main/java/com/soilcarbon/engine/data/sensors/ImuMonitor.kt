package com.soilcarbon.engine.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.soilcarbon.engine.domain.model.ImuReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** Rotation-vector based pitch/roll monitor used by the FR-1.3 live angle gate. */
class ImuMonitor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _reading = MutableStateFlow<ImuReading?>(null)
    val reading: StateFlow<ImuReading?> = _reading.asStateFlow()

    val isAvailable: Boolean get() = rotationSensor != null || accelerometer != null

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun start() {
        val sensor = rotationSensor ?: accelerometer ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timestamp = System.currentTimeMillis()
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            _reading.value = ImuReading(
                pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat(),
                rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat(),
                azimuthDegrees = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat(),
                timestampMillis = timestamp
            )
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val pitch = Math.toDegrees(atan2(-x.toDouble(), sqrt((y * y + z * z).toDouble())))
            val roll = Math.toDegrees(atan2(y.toDouble(), z.toDouble()))
            _reading.value = ImuReading(
                pitchDegrees = pitch.toFloat(),
                rollDegrees = roll.toFloat(),
                timestampMillis = timestamp
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        fun isLevel(reading: ImuReading?, maxTiltDegrees: Float): Boolean =
            reading != null &&
                abs(reading.pitchDegrees) <= maxTiltDegrees &&
                abs(reading.rollDegrees) <= maxTiltDegrees
    }
}
