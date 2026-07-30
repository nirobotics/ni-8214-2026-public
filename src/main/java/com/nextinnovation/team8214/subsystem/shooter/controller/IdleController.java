// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.shooter.controller;

import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.subsystem.shooter.ShooterConfig;
import com.nextinnovation.team8214.subsystem.shooter.ShooterControlRequest;
import com.nextinnovation.team8214.subsystem.shooter.ShooterControlRequest.FlywheelConstraints;
import com.nextinnovation.team8214.subsystem.shooter.ShooterControlRequest.FlywheelSetpoint;
import com.nextinnovation.team8214.subsystem.shooter.ShootingCalculator;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.Optional;
import org.littletonrobotics.junction.Logger;

public class IdleController {

  private static final double BLUE_BUMP_FAR_EDGE_X_METER = 4.62;
  private static final double MIN_SHOOTER_2_HUB_DISTANCE_METER = 2.0;
  private static final double MAX_SHOOTER_2_HUB_DISTANCE_METER = 3.8;

  private static final LoggedTunableNumber flywheelMaxAccelMeterPerSec2 =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/idleController/flywheelMaxAccelMeterPerSec2",
          1.5);
  private static final LoggedTunableNumber flywheelVelocityToleranceMeterPerSec =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT
              + "/controller/idleController/flywheelVelocityToleranceMeterPerSec",
          0.1);

  public ShooterControlRequest update(double manualDistanceOffset) {
    var robotInFieldPosition = Odometry.getInstance().getEstimatedPose().getTranslation();
    var clampedRobotInFieldPosition = clampRobotXToBumpFarEdge(robotInFieldPosition);
    var robot2Hub = AllianceFlipUtil.apply(Field.HUB_CENTER).minus(clampedRobotInFieldPosition);
    var shooter2HubDistanceMeter =
        MathUtil.clamp(
            robot2Hub.getNorm()
                + ShooterConfig.SHOOTER_IN_ROBOT_POSITION.getX()
                + manualDistanceOffset,
            MIN_SHOOTER_2_HUB_DISTANCE_METER + manualDistanceOffset,
            MAX_SHOOTER_2_HUB_DISTANCE_METER + manualDistanceOffset);

    var shootingCalculator = ShootingCalculator.getInstance();
    shootingCalculator.switchTable("score");
    shootingCalculator.publishCurrentDistance(shooter2HubDistanceMeter);
    var velocity =
        new Translation2d(
            shootingCalculator.getInterpolatedX(shooter2HubDistanceMeter),
            shootingCalculator.getInterpolatedY(shooter2HubDistanceMeter));

    Logger.recordOutput(
        ShooterConfig.LOG_ROOT + "/controller/idleController/clampedRobotInFieldPosition",
        clampedRobotInFieldPosition);
    Logger.recordOutput(
        ShooterConfig.LOG_ROOT + "/controller/idleController/shooter2HubDistanceMeter",
        shooter2HubDistanceMeter);

    return new ShooterControlRequest(
        Optional.of(
            new FlywheelSetpoint(
                ShooterControlRequest.flywheelMeter2Rad(velocity.getNorm()),
                ShooterControlRequest.flywheelMeter2Rad(
                    flywheelVelocityToleranceMeterPerSec.get()))),
        Optional.of(
            new FlywheelConstraints(
                ShooterControlRequest.flywheelMeter2Rad(flywheelMaxAccelMeterPerSec2.get()))),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private Translation2d clampRobotXToBumpFarEdge(Translation2d robotInFieldPosition) {
    var blueSideX = AllianceFlipUtil.applyX(robotInFieldPosition.getX());
    var clampedBlueSideX = Math.min(blueSideX, BLUE_BUMP_FAR_EDGE_X_METER);
    var clampedFieldX =
        AllianceFlipUtil.shouldFlip()
            ? AllianceFlipUtil.forceApplyX(clampedBlueSideX)
            : clampedBlueSideX;
    return new Translation2d(clampedFieldX, robotInFieldPosition.getY());
  }
}
