// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.vision;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.util.GeomUtil;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import com.nextinnovation.team8214.util.LoggerUtil;
import com.nextinnovation.team8214.util.VirtualSubsystem;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.util.Units;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import org.littletonrobotics.junction.Logger;
import org.photonvision.simulation.VisionSystemSim;

@ExtensionMethod({GeomUtil.class})
public class ApriltagVision extends VirtualSubsystem {
  private final List<ApriltagVisionCamera> cameras = new ArrayList<>();
  private VisionSystemSim visionSystemSim = null;

  private final LoggedTunableNumber maxAllowedAmbiguity =
      new LoggedTunableNumber(
          ApriltagVisionConfig.LOG_GROUP,
          ApriltagVisionConfig.LOG_ROOT + "/maxAllowedAmbiguity",
          0.2);

  private final List<Odometry.VisionObservation> allGoodVisionObservations = new ArrayList<>();
  private final List<Pose3d> allGoodRobotInField = new ArrayList<>();
  private final List<Pose2d> allGoodRobotInField2d = new ArrayList<>();
  private final List<Pose3d> allBadRobotInField = new ArrayList<>();
  private final List<Pose3d> allUsedTagInField = new ArrayList<>();

  public ApriltagVision() {
    if (Config.MODE == Config.Mode.SIM) {
      visionSystemSim = new VisionSystemSim("chassisApriltag");
      visionSystemSim.addAprilTags(Field.APRILTAG_LAYOUT.getLayout());
    }

    cameras.add(
        new ApriltagVisionCamera(
            "upLeft", ApriltagVisionConfig.UP_LEFT.toTransform3d(), 0.01, 1.0, visionSystemSim));

    cameras.add(
        new ApriltagVisionCamera(
            "downLeft",
            ApriltagVisionConfig.DOWN_LEFT.toTransform3d(),
            0.008,
            0.7,
            visionSystemSim));

    cameras.add(
        new ApriltagVisionCamera(
            "downRight",
            ApriltagVisionConfig.DOWN_RIGHT.toTransform3d(),
            0.008,
            0.7,
            visionSystemSim));

    cameras.add(
        new ApriltagVisionCamera(
            "upRight", ApriltagVisionConfig.UP_RIGHT.toTransform3d(), 0.01, 1.0, visionSystemSim));
  }

  @Override
  public void periodic() {
    if (Config.MODE == Config.Mode.SIM && visionSystemSim != null) {
      var startTime = LoggerUtil.getTimestampSec();
      visionSystemSim.update(Odometry.getInstance().getSimPose());
      Logger.recordOutput(
          "performance/apriltagVision/chassisVisionSystemSim/update",
          LoggerUtil.getTimestampSec() - startTime);
    }

    allGoodVisionObservations.clear();
    allGoodRobotInField.clear();
    allGoodRobotInField2d.clear();
    allBadRobotInField.clear();
    allUsedTagInField.clear();

    boolean hasAnyUpdate = false;

    for (var camera : cameras) {
      camera.update();
      if (camera.getInputs().hasUpdate) {
        hasAnyUpdate = true;
        processCameraInput(camera);
      }
    }

    if (hasAnyUpdate) {
      logAndApplyObservations();
    }
  }

  private void processCameraInput(ApriltagVisionCamera camera) {
    var inputs = camera.getInputs();
    if (!inputs.connected || !inputs.hasTargets || inputs.poseObservations.length == 0) return;

    for (var poseObservation : inputs.poseObservations) {
      if (poseObservation.ambiguity() > maxAllowedAmbiguity.get()) {
        continue;
      }

      var cameraToRobot = camera.getRobotToCamera().inverse();
      final var robotInField = poseObservation.cameraInField().transformBy(cameraToRobot);

      if (isPoseOutsideField(robotInField)) {
        allBadRobotInField.add(robotInField);
        continue;
      }

      var xyStdDev =
          (poseObservation.tagCount() > 1)
              ? camera.getXyStdMeter()
                  * Math.pow(poseObservation.avgDistance(), 1.2)
                  / Math.pow(poseObservation.tagCount(), 2.0)
              : Double.POSITIVE_INFINITY;

      var thetaStdDev =
          (poseObservation.tagCount() > 1)
              ? Units.degreesToRadians(camera.getThetaStdDegree())
                  * Math.pow(poseObservation.avgDistance(), 1.2)
                  / Math.pow(poseObservation.tagCount(), 2.0)
              : Double.POSITIVE_INFINITY;

      var robotInField2d = robotInField.toPose2d();
      allGoodVisionObservations.add(
          new Odometry.VisionObservation(
              poseObservation.timestamp(),
              robotInField2d,
              VecBuilder.fill(xyStdDev, xyStdDev, thetaStdDev)));

      allGoodRobotInField.add(robotInField);
      allGoodRobotInField2d.add(robotInField2d);
    }

    for (var id : inputs.ids) {
      Field.APRILTAG_LAYOUT.getLayout().getTagPose(id).ifPresent(allUsedTagInField::add);
    }
  }

  private boolean isPoseOutsideField(Pose3d pose) {
    return pose.getX() < -ApriltagVisionConfig.FIELD_BORDER_THRESHOLD_METER
        || pose.getX() > Field.LENGTH + ApriltagVisionConfig.FIELD_BORDER_THRESHOLD_METER
        || pose.getY() < -ApriltagVisionConfig.FIELD_BORDER_THRESHOLD_METER
        || pose.getY() > Field.WIDTH + ApriltagVisionConfig.FIELD_BORDER_THRESHOLD_METER
        || Math.abs(pose.getZ()) > ApriltagVisionConfig.ROBOT_POSE_Z_THRESHOLD_METER;
  }

  private void logAndApplyObservations() {
    Logger.recordOutput(
        ApriltagVisionConfig.LOG_ROOT + "/allBadRobotInField",
        allBadRobotInField.toArray(Pose3d[]::new));
    Logger.recordOutput(
        ApriltagVisionConfig.LOG_ROOT + "/allGoodRobotInField",
        allGoodRobotInField.toArray(Pose3d[]::new));
    Logger.recordOutput(
        ApriltagVisionConfig.LOG_ROOT + "/allGoodRobotInField2d",
        allGoodRobotInField2d.toArray(Pose2d[]::new));
    Logger.recordOutput(
        ApriltagVisionConfig.LOG_ROOT + "/allUsedTagInField",
        allUsedTagInField.toArray(Pose3d[]::new));

    allGoodVisionObservations.stream()
        .sorted(Comparator.comparingDouble(Odometry.VisionObservation::timestamp))
        .forEach(Odometry.getInstance()::addVisionObservation);
  }
}
