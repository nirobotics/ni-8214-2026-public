// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.shooter.controller;

import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.subsystem.shooter.ShooterConfig;
import com.nextinnovation.team8214.subsystem.shooter.ShooterControlRequest;
import com.nextinnovation.team8214.subsystem.shooter.ShooterControlRequest.FlywheelSetpoint;
import com.nextinnovation.team8214.subsystem.shooter.ShooterControlRequest.PitchConstraints;
import com.nextinnovation.team8214.subsystem.shooter.ShooterControlRequest.PitchSetpoint;
import com.nextinnovation.team8214.subsystem.shooter.ShootingCalculator;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import com.nextinnovation.team8214.util.GeomUtil;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.util.Optional;
import lombok.experimental.ExtensionMethod;
import org.littletonrobotics.junction.Logger;

@ExtensionMethod({GeomUtil.class})
public class TransportController {
  private static final LoggedTunableNumber placementYSwitchDiffMeter =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/transportController/placementYSwitchDiffMeter",
          0.2);

  private static final LoggedTunableNumber flywheelVelocityToleranceMeterPerSec =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT
              + "/controller/transportController/flywheelVelocityToleranceMeterPerSec",
          0.2);

  private static final LoggedTunableNumber pitchMaxVelocityDegreePerSec =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/transportController/pitchMaxVelocityDegreePerSec",
          1080.0);

  private static final LoggedTunableNumber pitchMaxAccelDegreePerSec2 =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/transportController/pitchMaxAccelDegreePerSec2",
          2160.0);

  private static final LoggedTunableNumber pitchPositionToleranceDegree =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/transportController/pitchPositionToleranceDegree",
          3.0);

  private Translation2d placement = null;

  public ShooterControlRequest update() {
    var odometry = Odometry.getInstance();

    var robotInFieldPosition = odometry.getEstimatedPose().getTranslation();

    var flippedRobotY =
        AllianceFlipUtil.applyY(odometry.getEstimatedPose().getTranslation().getY());

    if (placement == null) {
      if (flippedRobotY > (Field.WIDTH / 2.0)) {
        placement = AllianceFlipUtil.apply(Field.TRANSPORT_LEFT_PLACEMENT);
      } else {
        placement = AllianceFlipUtil.apply(Field.TRANSPORT_RIGHT_PLACEMENT);
      }
    } else {
      if (flippedRobotY >= Field.WIDTH / 2.0 + placementYSwitchDiffMeter.get()) {
        placement = AllianceFlipUtil.apply(Field.TRANSPORT_LEFT_PLACEMENT);
      } else if (flippedRobotY <= Field.WIDTH / 2.0 - placementYSwitchDiffMeter.get()) {
        placement = AllianceFlipUtil.apply(Field.TRANSPORT_RIGHT_PLACEMENT);
      }
    }

    var robot2Placement = placement.minus(robotInFieldPosition);
    var robot2PlacementDistanceMeter =
        robot2Placement.getNorm() + ShooterConfig.SHOOTER_IN_ROBOT_POSITION.getX();

    Logger.recordOutput(
        ShooterConfig.LOG_ROOT + "/controller/transportController/robot2PlacementDistanceMeter",
        robot2PlacementDistanceMeter);

    var shootingCalculator = ShootingCalculator.getInstance();
    shootingCalculator.switchTable("transport");
    shootingCalculator.publishCurrentDistance(robot2PlacementDistanceMeter);

    var orgXyShootingVector =
        new Translation2d(
            shootingCalculator.getInterpolatedX(robot2PlacementDistanceMeter),
            robot2Placement.getAngle());

    var wholeShootingVector =
        new Translation3d(
            orgXyShootingVector.getX(),
            orgXyShootingVector.getY(),
            shootingCalculator.getInterpolatedY(robot2PlacementDistanceMeter));

    return new ShooterControlRequest(
        Optional.of(
            new FlywheelSetpoint(
                ShooterControlRequest.flywheelMeter2Rad(wholeShootingVector.getNorm()),
                ShooterControlRequest.flywheelMeter2Rad(
                    flywheelVelocityToleranceMeterPerSec.get()))),
        Optional.empty(),
        Optional.of(
            new PitchSetpoint(
                new Rotation2d(
                        wholeShootingVector.toTranslation2d().getNorm(), wholeShootingVector.getZ())
                    .getRadians(),
                Units.degreesToRadians(pitchPositionToleranceDegree.get()))),
        Optional.of(
            new PitchConstraints(
                Units.degreesToRadians(pitchMaxVelocityDegreePerSec.get()),
                Units.degreesToRadians(pitchMaxAccelDegreePerSec2.get()))),
        Optional.of(wholeShootingVector.toTranslation2d().getAngle().rotateBy(Rotation2d.k180deg)));
  }
}
