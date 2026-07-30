// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve.controller;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.subsystem.swerve.SwerveConfig;
import com.nextinnovation.team8214.util.EqualsUtil;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import org.littletonrobotics.junction.Logger;

public class TeleopController {
  private static final LoggedTunableNumber translationDeadband =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/translationDeadband",
          0.02);
  private static final LoggedTunableNumber maxTranslationScalar =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/maxTranslationScalar",
          1.0);
  private static final LoggedTunableNumber rotationDeadband =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/rotationDeadband",
          0.02);
  private static final LoggedTunableNumber usualRotationScalar =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/usualRotationScalar",
          0.65);
  private static final LoggedTunableNumber escapeRotationScalar =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/escapeRotationScalar",
          1.0);

  private static final LoggedTunableNumber headingMaintainerKp =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/headingMaintainer/kp",
          3.5);
  private static final LoggedTunableNumber headingMaintainerKd =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/headingMaintainer/kd",
          0.0);
  private static final LoggedTunableNumber headingMaintainerToleranceDegree =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/headingMaintainer/toleranceDegree",
          3.0);
  private static final LoggedTunableNumber headingMaintainerMinOutputDegreePerSec =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/headingMaintainer/minOutputDegreePerSec",
          1.0);
  private static final LoggedTunableNumber headingMaintainerMaxOutputDegreePerSec =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/headingMaintainer/maxOutputDegreePerSec",
          90.0);

  private static final LoggedTunableNumber headingMaintainerMinEnableVelDegreePerSec =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/teleopController/headingMaintainer/minEnableVelDegreePerSec",
          5.0);

  private double x = 0;
  private double y = 0;
  private double omega = 0;
  private boolean wantEscape = false;
  private final PIDController headingMaintainer;
  private boolean enableHeadingMaintainer = false;

  public TeleopController() {
    headingMaintainer =
        new PIDController(
            headingMaintainerKp.get(), 0.0, headingMaintainerKd.get(), Config.LOOP_PERIOD_SEC);
    headingMaintainer.enableContinuousInput(-Math.PI, Math.PI);
    headingMaintainer.setTolerance(Units.degreesToRadians(headingMaintainerToleranceDegree.get()));
  }

  public void setInput(double x, double y, double omega, boolean wantEscape) {
    this.x = x;
    this.y = y;
    this.omega = omega;
    this.wantEscape = wantEscape;
  }

  public ChassisSpeeds update() {
    var translation = calcTranslation();
    var rotation = calcRotation();

    headingMaintainer.setPID(headingMaintainerKp.get(), 0.0, headingMaintainerKd.get());
    headingMaintainer.setTolerance(Units.degreesToRadians(headingMaintainerToleranceDegree.get()));

    var yawVelRadPerSec = Odometry.getInstance().getRobotCentricVel().dtheta;
    var yawRad = Odometry.getInstance().getEstimatedPose().getRotation().getRadians();

    if (!wantEscape
        && EqualsUtil.epsilonEquals(rotation, 0.0, 1e-9)
        && EqualsUtil.epsilonEquals(
            yawVelRadPerSec,
            0.0,
            Units.degreesToRadians(headingMaintainerMinEnableVelDegreePerSec.get()))) {
      if (!enableHeadingMaintainer) {
        headingMaintainer.setSetpoint(yawRad);
        enableHeadingMaintainer = true;
      }
    } else {
      disableHeadingMaintainer();
    }

    if (DriverStation.getAlliance().isPresent()
        && DriverStation.getAlliance().get() == DriverStation.Alliance.Red) {
      translation = translation.rotateBy(Rotation2d.fromRadians(Math.PI));
    }

    var headingMaintainerMaxOutputRadPerSec =
        Units.degreesToRadians(headingMaintainerMaxOutputDegreePerSec.get());

    var headingMaintainerOutput =
        enableHeadingMaintainer
            ? MathUtil.clamp(
                MathUtil.applyDeadband(
                    headingMaintainer.calculate(yawRad),
                    Units.degreesToRadians(headingMaintainerMinOutputDegreePerSec.get())),
                -headingMaintainerMaxOutputRadPerSec,
                headingMaintainerMaxOutputRadPerSec)
            : 0.0;
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/teleopController/headingMaintainer/Output",
        headingMaintainerOutput);

    return ChassisSpeeds.fromFieldRelativeSpeeds(
        translation.getX() * SwerveConfig.MAX_TRANSLATION_VEL_METER_PER_SEC,
        translation.getY() * SwerveConfig.MAX_TRANSLATION_VEL_METER_PER_SEC,
        rotation * SwerveConfig.MAX_ANGULAR_VEL_RAD_PER_SEC + headingMaintainerOutput,
        Odometry.getInstance().getEstimatedPose().getRotation());
  }

  private Translation2d calcTranslation() {
    var magnitude =
        MathUtil.applyDeadband(Math.hypot(x, y), translationDeadband.get())
            * maxTranslationScalar.get();
    var direction = magnitude < 1e-16 ? new Rotation2d() : new Rotation2d(x, y);

    return new Translation2d(magnitude * magnitude, direction);
  }

  private double calcRotation() {
    var magnitude =
        MathUtil.applyDeadband(omega, rotationDeadband.get())
            * (wantEscape ? escapeRotationScalar.get() : usualRotationScalar.get());

    return Math.copySign(magnitude * magnitude, magnitude);
  }

  public void disableHeadingMaintainer() {
    headingMaintainer.reset();
    enableHeadingMaintainer = false;
  }

  public void resetHeadingMaintainerSetpointToCurrent() {
    var yawRad = Odometry.getInstance().getEstimatedPose().getRotation().getRadians();
    headingMaintainer.setSetpoint(yawRad);
  }
}
