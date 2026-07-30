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
public class ScoreController {
  private static final LoggedTunableNumber flywheelVelocityToleranceMeterPerSec =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT
              + "/controller/scoreController/flywheelVelocityToleranceMeterPerSec",
          0.1);

  private static final LoggedTunableNumber pitchMaxVelocityDegreePerSec =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/scoreController/pitchMaxVelocityDegreePerSec",
          1080.0);

  private static final LoggedTunableNumber pitchMaxAccelDegreePerSec2 =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/scoreController/pitchMaxAccelDegreePerSec2",
          2160.0);

  private static final LoggedTunableNumber pitchPositionToleranceDegree =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/scoreController/pitchPositionToleranceDegree",
          3.0);

  public ShooterControlRequest update(double manualDistanceOffset) {
    var odometry = Odometry.getInstance();

    var robotInFieldPosition = odometry.getEstimatedPose().getTranslation();

    var robot2Hub = AllianceFlipUtil.apply(Field.HUB_CENTER).minus(robotInFieldPosition);
    var shooter2HubDistanceMeter =
        robot2Hub.getNorm() + ShooterConfig.SHOOTER_IN_ROBOT_POSITION.getX();

    Logger.recordOutput(
        ShooterConfig.LOG_ROOT + "/controller/scoreController/shooter2HubDistanceMeter",
        shooter2HubDistanceMeter);

    var shootingCalculator = ShootingCalculator.getInstance();
    shootingCalculator.switchTable("score");
    shootingCalculator.publishCurrentDistance(shooter2HubDistanceMeter);
    var shootingTableDistanceMeter = Math.max(0.0, shooter2HubDistanceMeter + manualDistanceOffset);
    var orgXyShootingVector =
        new Translation2d(
            shootingCalculator.getInterpolatedX(shootingTableDistanceMeter), robot2Hub.getAngle());

    var wholeShootingVector =
        new Translation3d(
            orgXyShootingVector.getX(),
            orgXyShootingVector.getY(),
            shootingCalculator.getInterpolatedY(shootingTableDistanceMeter));

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
