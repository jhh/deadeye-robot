package frc.robot.subsystems

import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.Timer
import edu.wpi.first.wpilibj2.command.SubsystemBase
import mu.KotlinLogging
import org.strykeforce.deadeye.Deadeye
import org.strykeforce.deadeye.TargetDataListener

const val kCamera = "A0"
val kServer = "10.27.67.10"
private val logger = KotlinLogging.logger {}
private val metrics = MetricRegistry()
private val timer = Timer().apply { start() }

class VisionSubsystem : SubsystemBase(), TargetDataListener<HubTargetData> {

    private val deadeye: Deadeye<HubTargetData> = run {
        val networkTableInstance = NetworkTableInstance.create()
        networkTableInstance.startClient(kServer)
        Deadeye(kCamera, HubTargetData::class.java, networkTableInstance)
    }.also {
        it.targetDataListener = this
        HubTargetData.kFrameCenter = it.capture.width / 2
    }

    private val fpsMeter: Meter by lazy { metrics.meter(kCamera) }
    private val timer = Timer().apply { start() }

    var enabled
        get() = deadeye.enabled
        set(value) {
            deadeye.enabled = value
        }

    override fun onTargetData(data: HubTargetData) {
        fpsMeter.mark()
        if (timer.advanceIfElapsed(5.0))
            data.targets.forEachIndexed { index, rect -> logger.debug { "$index = $rect" } }
    }

    override fun periodic() {
//        if (timer.advanceIfElapsed(10.0))
//            logger.info { "mean FPS = ${fpsMeter.meanRate}" }
    }
}