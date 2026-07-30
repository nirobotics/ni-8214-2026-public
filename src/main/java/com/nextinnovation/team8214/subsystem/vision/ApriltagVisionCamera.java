// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.vision;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.util.Alert;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import lombok.Getter;
import org.littletonrobotics.junction.Logger;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionSystemSim;

public class ApriltagVisionCamera {
  private final String name;
  private final ApriltagVisionIO io;

  @Getter
  private final ApriltagVisionIOInputsAutoLogged inputs = new ApriltagVisionIOInputsAutoLogged();

  private final Alert offlineAlert;

  private final LoggedTunableNumber xyStdMeter;
  private final LoggedTunableNumber thetaStdDegree;
  @Getter private final Transform3d robotToCamera;

  public ApriltagVisionCamera(
      String name,
      Transform3d robotToCamera,
      double xyStdMeter,
      double thetaStdDegree,
      VisionSystemSim visionSystemSim) {
    this.name = name;
    this.robotToCamera = robotToCamera;
    this.offlineAlert = new Alert(name + " PhotonVision offline!", Alert.AlertType.WARNING);

    this.xyStdMeter =
        new LoggedTunableNumber(
            ApriltagVisionConfig.LOG_GROUP,
            ApriltagVisionConfig.LOG_ROOT + "/" + name + "/xyStdMeter",
            xyStdMeter);
    this.thetaStdDegree =
        new LoggedTunableNumber(
            ApriltagVisionConfig.LOG_GROUP,
            ApriltagVisionConfig.LOG_ROOT + "/" + name + "/thetaStdDegree",
            thetaStdDegree);

    switch (Config.MODE) {
      case REAL -> io = new ApriltagVisionIOPhoton(name);
      case SIM ->
          io =
              new ApriltagVisionIOPhotonSim(name, createSimProps(), robotToCamera, visionSystemSim);
      default -> io = new ApriltagVisionIO() {};
    }
  }

  public void update() {
    io.updateInputs(inputs);
    Logger.processInputs(ApriltagVisionConfig.LOG_ROOT + "/" + name, inputs);
    offlineAlert.set(!inputs.connected);
  }

  public double getXyStdMeter() {
    return xyStdMeter.get();
  }

  public double getThetaStdDegree() {
    return thetaStdDegree.get();
  }

  private static SimCameraProperties createSimProps() {
    var prop = new SimCameraProperties();
    prop.setCalibration(1280, 800, Rotation2d.fromDegrees(82.0));
    prop.setCalibError(0.4, 0.1);
    prop.setFPS(35.0);
    prop.setAvgLatencyMs(34);
    prop.setLatencyStdDevMs(3);
    return prop;
  }
}
