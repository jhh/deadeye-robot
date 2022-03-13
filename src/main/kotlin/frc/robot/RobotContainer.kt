// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.
package frc.robot

import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard
import edu.wpi.first.wpilibj2.command.InstantCommand
import frc.robot.subsystems.VisionSubsystem
import mu.KotlinLogging

class RobotContainer {

    val visionSubsystem = VisionSubsystem().also {
        it.enabled = false
    }

    init {
        configureButtonBindings()
    }

    private fun configureButtonBindings() {
        val tab = Shuffleboard.getTab("Dashboard")
        val layout = tab.getLayout("Vision", BuiltInLayouts.kGrid)
        layout.add(
            "Enable",
            object : InstantCommand({ visionSubsystem.enabled = true }, visionSubsystem) {
                override fun getName() = "Enable"
                override fun runsWhenDisabled() = true
            }
        )
        layout.add(
            "Disable",
            object : InstantCommand({ visionSubsystem.enabled = false }, visionSubsystem) {
                override fun getName() = "Disable"
                override fun runsWhenDisabled() = true
            }
        )
    }

}