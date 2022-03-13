package frc.robot.subsystems

import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj2.command.SubsystemBase
import mu.KotlinLogging
import org.strykeforce.deadeye.Deadeye
import org.strykeforce.deadeye.TargetDataListener

private val logger = KotlinLogging.logger {}

class VisionSubsystem : SubsystemBase(), TargetDataListener<HubTargetData> {

    private val deadeye: Deadeye<HubTargetData> = run {
        val networkTableInstance = NetworkTableInstance.create()
        networkTableInstance.startClient("10.27.67.10")
        Deadeye("A0", HubTargetData::class.java, networkTableInstance)
    }.also {
        it.targetDataListener = this
        HubTargetData.kFrameCenter = it.capture.width / 2
    }

    var enabled
        get() = deadeye.enabled
        set(value) {
            deadeye.enabled = value
        }

    override fun onTargetData(data: HubTargetData?) {
        logger.debug { "onTargetData: $data" }
    }

}