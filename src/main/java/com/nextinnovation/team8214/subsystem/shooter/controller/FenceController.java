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
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import java.util.Optional;

public class FenceController {
  private static final LoggedTunableNumber flywheelVelocityToleranceMeterPerSec =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT
              + "/controller/fenceController/flywheelVelocityToleranceMeterPerSec",
          0.2);
  private static final LoggedTunableNumber pitchMaxVelocityDegreePerSec =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/fenceController/pitchMaxVelocityDegreePerSec",
          900.0);
  private static final LoggedTunableNumber pitchMaxAccelDegreePerSec2 =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/fenceController/pitchMaxAccelDegreePerSec2",
          900.0);
  private static final LoggedTunableNumber pitchPositionToleranceDegree =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/controller/fenceController/pitchPositionToleranceDegree",
          3.0);

  public ShooterControlRequest update() {
    var shootingCalculator = ShootingCalculator.getInstance();
    shootingCalculator.switchTable("fence");
    var velocity =
        new Translation2d(
            shootingCalculator.getInterpolatedX(0.0), shootingCalculator.getInterpolatedY(0.0));

    var robot2Hub =
        AllianceFlipUtil.apply(Field.HUB_CENTER)
            .minus(Odometry.getInstance().getEstimatedPose().getTranslation());
    var shooter2HubDistanceMeter =
        robot2Hub.getNorm() - ShooterConfig.SHOOTER_IN_ROBOT_POSITION.getX();
    ShootingCalculator.getInstance().publishCurrentDistance(shooter2HubDistanceMeter);

    return new ShooterControlRequest(
        Optional.of(
            new FlywheelSetpoint(
                ShooterControlRequest.flywheelMeter2Rad(velocity.getNorm()),
                ShooterControlRequest.flywheelMeter2Rad(
                    flywheelVelocityToleranceMeterPerSec.get()))),
        Optional.empty(),
        Optional.of(
            new PitchSetpoint(
                Units.degreesToRadians(velocity.getAngle().getDegrees()),
                Units.degreesToRadians(pitchPositionToleranceDegree.get()))),
        Optional.of(
            new PitchConstraints(
                Units.degreesToRadians(pitchMaxVelocityDegreePerSec.get()),
                Units.degreesToRadians(pitchMaxAccelDegreePerSec2.get()))),
        Optional.empty());
  }
}
