package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.strykeforce.deadeye.Deadeye;
import org.strykeforce.deadeye.TargetListTargetData;

public class DeadeyeSubsystem extends SubsystemBase {

  Deadeye<TargetListTargetData> deadeye;

  public DeadeyeSubsystem() {
    deadeye = new Deadeye<>("W0", TargetListTargetData.class);
    deadeye.setTargetDataListener(System.out::println);
  }

  public void setEnabled(boolean enabled) {
    deadeye.setEnabled(enabled);
  }
}
