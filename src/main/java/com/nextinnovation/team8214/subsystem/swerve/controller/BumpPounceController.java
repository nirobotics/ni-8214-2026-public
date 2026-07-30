// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve.controller;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.subsystem.swerve.SwerveConfig;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import org.littletonrobotics.junction.Logger;

public class BumpPounceController {
  private static final double TILT_FALLING_DEBOUNCE_SEC = 0.19;
  private static final double TILT_DETECTION_MIN_PROGRESS_METER = 0.25;
  private static final double MAX_ROTATION_VELOCITY_RAD_PER_SEC =
      SwerveConfig.MAX_ANGULAR_VEL_RAD_PER_SEC * 0.6;

  private static final LoggedTunableNumber xVelocityMeterPerSec =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/bumpPounceController/xVelocityMeterPerSec",
          3.3);
  private static final LoggedTunableNumber yKp =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/bumpPounceController/yKp", 2.0);
  private static final LoggedTunableNumber yKd =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/bumpPounceController/yKd", 0.0);
  private static final LoggedTunableNumber maxYVelocityMeterPerSec =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/bumpPounceController/maxYVelocityMeterPerSec",
          0.5);
  private static final LoggedTunableNumber tiltThresholdDegree =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/bumpPounceController/tiltThresholdDegree",
          7.5);
  private static final LoggedTunableNumber goalYToleranceMeter =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/bumpPounceController/goalYToleranceMeter",
          0.05);
  private static final LoggedTunableNumber rotationKp =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/bumpPounceController/rotationKp", 3.5);
  private static final LoggedTunableNumber rotationKd =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/bumpPounceController/rotationKd", 0.0);
  private static final LoggedTunableNumber maxRotationVelocityRadPerSec =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/bumpPounceController/maxRotationVelocityRadPerSec",
          MAX_ROTATION_VELOCITY_RAD_PER_SEC);
  private static final LoggedTunableNumber maxRotationAccelerationRadPerSec2 =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/bumpPounceController/maxRotationAccelerationRadPerSec2",
          MAX_ROTATION_VELOCITY_RAD_PER_SEC / 0.1);

  private final Translation2d startPosition;
  private final Pose2d goalPose;
  private final Rotation2d goalHeading;
  private final double xDirection;
  private final double travelDistanceMeter;
  private final PIDController yController;
  private final ProfiledPIDController rotationController;
  private final Debouncer tiltFallingDebouncer =
      new Debouncer(TILT_FALLING_DEBOUNCE_SEC, Debouncer.DebounceType.kFalling);

  private boolean hasSeenTilt = false;
  private boolean debouncedTilt = false;
  private boolean tiltDetectionArmed = false;
  private boolean hasSafetyStopped = false;

  public BumpPounceController(Pose2d startPose, Pose2d goalPose) {
    startPosition = startPose.getTranslation();
    this.goalPose = goalPose;
    goalHeading = startPose.getRotation();

    yController = new PIDController(yKp.get(), 0.0, yKd.get(), Config.LOOP_PERIOD_SEC);
    yController.setTolerance(goalYToleranceMeter.get());

    rotationController =
        new ProfiledPIDController(
            rotationKp.get(),
            0.0,
            rotationKd.get(),
            new TrapezoidProfile.Constraints(0.0, 0.0),
            Config.LOOP_PERIOD_SEC);
    rotationController.enableContinuousInput(-Math.PI, Math.PI);
    rotationController.reset(
        Odometry.getInstance().getEstimatedPose().getRotation().getRadians(),
        Odometry.getInstance().getFieldCentricVel().dtheta);

    var travel = goalPose.getTranslation().minus(startPosition);
    xDirection = Math.signum(travel.getX());
    travelDistanceMeter = Math.abs(travel.getX());
  }

  public ChassisSpeeds update(
      Rotation2d rollPosition, Rotation2d pitchPosition, boolean gyroConnected) {
    var currentPose = Odometry.getInstance().getEstimatedPose();
    var currentPosition = currentPose.getTranslation();
    var progressMeter = getProgressMeter(currentPosition);
    tiltDetectionArmed = tiltDetectionArmed || progressMeter >= TILT_DETECTION_MIN_PROGRESS_METER;
    updateTiltState(rollPosition, pitchPosition, gyroConnected && tiltDetectionArmed);
    hasSafetyStopped =
        hasSafetyStopped
            || travelDistanceMeter <= 1e-9
            || progressMeter >= travelDistanceMeter + 0.5;

    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/bumpPounceController/goalPose", goalPose);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/bumpPounceController/goalHeading", goalHeading);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/progressMeter", progressMeter);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/travelDistanceMeter", travelDistanceMeter);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/hasSafetyStopped", hasSafetyStopped);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/tiltDetectionArmed", tiltDetectionArmed);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/bumpPounceController/hasDone", hasDone());

    if (hasDone() || hasSafetyStopped) {
      return new ChassisSpeeds();
    }

    var xVelocity = getXVelocityMeterPerSec();
    var yVelocity = calculateYVelocityMeterPerSec(currentPosition.getY());
    var goalYErrorMeter = goalPose.getY() - currentPosition.getY();
    rotationController.setPID(rotationKp.get(), 0.0, rotationKd.get());
    rotationController.setConstraints(
        new TrapezoidProfile.Constraints(
            maxRotationVelocityRadPerSec.get(), maxRotationAccelerationRadPerSec2.get()));
    var rotationVelocityRadPerSec =
        rotationController.calculate(
            currentPose.getRotation().getRadians(), goalHeading.getRadians());

    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/rotationErrorRad",
        rotationController.getPositionError());
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/rotationVelocityRadPerSec",
        rotationVelocityRadPerSec);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/goalYErrorMeter", goalYErrorMeter);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/isCurrentYWithinGoalTolerance",
        isCurrentYWithinGoalTolerance(currentPosition));
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/xVelocityMeterPerSec", xVelocity);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/yVelocityMeterPerSec", yVelocity);

    return ChassisSpeeds.fromFieldRelativeSpeeds(
        xVelocity, yVelocity, rotationVelocityRadPerSec, currentPose.getRotation());
  }

  void updateTiltState(Rotation2d rollPosition, Rotation2d pitchPosition, boolean gyroConnected) {
    var tiltMagnitudeRad = Math.hypot(rollPosition.getRadians(), pitchPosition.getRadians());
    var isTilt =
        gyroConnected && tiltMagnitudeRad >= Units.degreesToRadians(tiltThresholdDegree.get());

    debouncedTilt = tiltFallingDebouncer.calculate(isTilt);
    hasSeenTilt = hasSeenTilt || isTilt;

    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/bumpPounceController/rollPosition", rollPosition);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/pitchPosition", pitchPosition);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/bumpPounceController/isTilt", isTilt);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/bumpPounceController/debouncedTilt", debouncedTilt);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/bumpPounceController/hasSeenTilt", hasSeenTilt);
  }

  Rotation2d getGoalHeading() {
    return goalHeading;
  }

  private boolean isCurrentYWithinGoalTolerance(Translation2d currentPosition) {
    return Math.abs(currentPosition.getY() - goalPose.getY()) <= goalYToleranceMeter.get();
  }

  double getXVelocityMeterPerSec() {
    return xDirection * Math.abs(xVelocityMeterPerSec.get());
  }

  double calculateYVelocityMeterPerSec(double currentYMeter) {
    yController.setPID(yKp.get(), 0.0, yKd.get());
    yController.setTolerance(goalYToleranceMeter.get());
    var output = yController.calculate(currentYMeter, goalPose.getY());

    if (Math.abs(currentYMeter - goalPose.getY()) <= goalYToleranceMeter.get()) {
      return 0.0;
    }

    var maxVelocity = Math.abs(maxYVelocityMeterPerSec.get());
    return MathUtil.clamp(output, -maxVelocity, maxVelocity);
  }

  double getTravelDistanceMeter() {
    return travelDistanceMeter;
  }

  double getProgressMeter(Translation2d currentPosition) {
    return Math.abs(currentPosition.getX() - startPosition.getX());
  }

  public boolean hasDone() {
    return hasSeenTilt && !debouncedTilt;
  }

  public boolean hasSafetyStopped() {
    return hasSafetyStopped;
  }
}
