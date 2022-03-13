package frc.robot.subsystems

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import edu.wpi.first.math.geometry.Rotation2d
import mu.KotlinLogging
import okio.Buffer
import okio.BufferedSource
import org.slf4j.LoggerFactory
import org.strykeforce.deadeye.DeadeyeJsonAdapter
import org.strykeforce.deadeye.Point
import org.strykeforce.deadeye.Rect
import org.strykeforce.deadeye.TargetListTargetData
import java.io.IOException
import java.util.*

private val logger = KotlinLogging.logger {}

class HubTargetData : TargetListTargetData {
    private val errorPixels: Double
    private val range: Double

    constructor() : super() {
        errorPixels = 0.0
        range = 0.0
    }

    constructor(
        id: String,
        serial: Int,
        valid: Boolean,
        errorPixels: Double,
        range: Double,
        targets: List<Rect>
    ) : super(id, serial, valid, targets) {
        this.errorPixels = errorPixels
        this.range = range
    }// return valid && !targets.isEmpty();

    /**
     * Check if this contains valid target data. Valid data is defined by the `valid` flag being
     * set `true` by the Deadeye camera and that there is one or more targets detected.
     *
     * @return true if valid
     */
    val isValid: Boolean
        get() = valid && targets.size > 2
    // return valid && !targets.isEmpty();
    /**
     * Return the distance from the center of the Hub target group to the center of the camera frame.
     * You should check [.isValid] before calling this method.
     *
     * @return the number of pixels, positive if ..., negative if ...
     * @throws IndexOutOfBoundsException if the list of targets is empty
     */
    fun getErrorPixels(): Double {
        val minX = targets[0].bottomRight.x
        val maxX = targets[targets.size - 1].topLeft.x
        return (maxX + minX) / 2.0 - kFrameCenter
    }

    /**
     * Return the angle from the center of the Hub target group to the center of the camera frame. You
     * should check [.isValid] before calling this method.
     *
     * @return the angle in Radians
     * @throws IndexOutOfBoundsException if the list of targets is empty
     */
    val errorRadians: Double
        get() = -kHorizonFov * getErrorPixels() / (kFrameCenter * 2)

    /**
     * Return the angle from the center of the Hub target group to the center of the camera frame. You
     * should check [.isValid] before calling this method.
     *
     * @return the angle as a `Rotation2d`
     * @throws IndexOutOfBoundsException if the list of targets is empty
     */
    val errorRotation2d: Rotation2d
        get() = Rotation2d(errorRadians)
    val interpolateT: Double
        get() = (kFrameCenter - Math.abs(getErrorPixels())) / kFrameCenter

    fun testGetTargetsPixelWidth(): Double {
        val pixelWidth: Double
        pixelWidth = if (targets.size % 2 == 1) {
            val leftTarget = targets[(targets.size - 1) / 2 - 1]
            val rightTarget = targets[(targets.size - 1) / 2 + 1]
            (rightTarget.bottomRight.x - leftTarget.topLeft.x).toDouble()
        } else {
            val leftTarget = targets[targets.size / 2 - 2]
            val rightTarget = targets[targets.size / 2 + 1]
            (rightTarget.topLeft.x - leftTarget.bottomRight.x).toDouble()
        }
        return pixelWidth
    }

    fun testGetDistance(): Double {
        val pixelWidth: Double
        return if (targets.size % 2 == 1) { // odd # of targets
            val leftTarget = targets[(targets.size - 1) / 2 - 1]
            val rightTarget = targets[(targets.size - 1) / 2 + 1]
            pixelWidth = (rightTarget.bottomRight.x - leftTarget.topLeft.x).toDouble()
            val enclosedAngle =
                Math.toDegrees(kHorizonFov) * pixelWidth / (kFrameCenter * 2)
            25.25 / 2 / Math.tan(Math.toRadians(enclosedAngle / 2))
        } else { // even # of targets
            val leftTarget = targets[targets.size / 2 - 2]
            val rightTarget = targets[targets.size / 2 + 1]
            pixelWidth = (rightTarget.topLeft.x - leftTarget.bottomRight.x).toDouble()
            val enclosedAngle =
                Math.toDegrees(kHorizonFov) * pixelWidth / (kFrameCenter * 2)
            25.625 / 2 / Math.tan(Math.toRadians(enclosedAngle / 2))
        }
    }

    /*return Math.sqrt(
        Math.pow(testGetDistance(), 2)
            - Math.pow(VisionConstants.kTapeHeightIn - VisionConstants.kCameraHeight, 2))
    + VisionConstants.kUpperHubRadiusIn;*/
    val groundDistance: Double
        get() {
            if (!isValid) {
                return 2767.0
            }
            val x = testGetDistance()
            return 0.00462384837384838 * Math.sqrt(x) - 2.70841658341659 * x + 518.807692307693
            /*return Math.sqrt(
        Math.pow(testGetDistance(), 2)
            - Math.pow(VisionConstants.kTapeHeightIn - VisionConstants.kCameraHeight, 2))
    + VisionConstants.kUpperHubRadiusIn;*/
        }

    override fun getJsonAdapter(): DeadeyeJsonAdapter<*> {
        return JsonAdapterImpl()
    }

    private class JsonAdapterImpl : DeadeyeJsonAdapter<HubTargetData?> {
        @Throws(IOException::class)
        override fun fromJson(source: BufferedSource): HubTargetData {
            val reader = JsonReader.of(source)
            var id: String = ""
            var serial = -1
            var valid = false
            var errorPixels = 0.0
            var range = 0.0
            val targets: MutableList<Rect> = ArrayList()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.selectName(OPTIONS)) {
                    0 -> id = reader.nextString()
                    1 -> serial = reader.nextInt()
                    2 -> valid = reader.nextBoolean()
                    3 -> errorPixels = reader.nextDouble()
                    4 -> range = reader.nextDouble()
                    5 -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val data = IntArray(DATA_LENGTH)
                            reader.beginArray()
                            var i = 0
                            while (i < DATA_LENGTH) {
                                data[i] = reader.nextInt()
                                i++
                            }
                            reader.endArray()
                            // bb.x, bb.y, bb.width, bb.height, area
                            val topLeft = Point(data[0], data[1])
                            val bottomRight = Point(data[0] + data[2], data[1] + data[3])
                            targets.add(Rect(topLeft, bottomRight, data[4]))
                        }
                        reader.endArray()
                    }
                    else -> throw IllegalStateException(
                        "Unexpected value: " + reader.selectName(
                            OPTIONS
                        )
                    )
                }
            }
            reader.endObject()
            return HubTargetData(id, serial, valid, errorPixels, range, targets
            )
        }

        @Throws(IOException::class)
        override fun toJson(targetData: HubTargetData?): String? {
            val buffer = Buffer()
            val writer = JsonWriter.of(buffer)
            writer.beginObject()
            writer.name("id").value(targetData!!.id)
            writer.name("sn").value(targetData.serial.toLong())
            writer.name("v").value(targetData.valid)
            writer.name("d").beginArray()
            for (t in targetData.targets) {
                writer.beginArray()
                writer.value(t.topLeft.x.toLong()).value(t.topLeft.y.toLong())
                writer.value(t.width().toLong()).value(t.height().toLong())
                writer.value(t.contourArea.toLong())
                writer.endArray()
            }
            writer.endArray()
            writer.endObject()
            return buffer.readUtf8()
        }

        companion object {
            const val DATA_LENGTH = 5
            private val OPTIONS = JsonReader.Options.of("id", "sn", "v", "ep", "r", "d")
        }
    }

    companion object {
        private const val kHorizonFov = 1.0
        var kFrameCenter = Int.MAX_VALUE
    }
}