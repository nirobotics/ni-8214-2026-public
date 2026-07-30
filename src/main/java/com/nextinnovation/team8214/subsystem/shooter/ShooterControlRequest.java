// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.shooter;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import java.util.Objects;
import java.util.Optional;

public record ShooterControlRequest(
    Optional<FlywheelSetpoint> flywheelSetpoint,
    Optional<FlywheelConstraints> flywheelConstraints,
    Optional<PitchSetpoint> pitchSetpoint,
    Optional<PitchConstraints> pitchConstraints,
    Optional<Rotation2d> targetFieldCentricYaw) {
  private static final PitchSetpoint DEFAULT_PITCH_SETPOINT =
      new PitchSetpoint(
          Units.degreesToRadians(ShooterConfig.START_ANGLE_DEGREE), Units.degreesToRadians(3.0));

  public ShooterControlRequest {
    Objects.requireNonNull(flywheelSetpoint);
    Objects.requireNonNull(flywheelConstraints);
    Objects.requireNonNull(pitchSetpoint);
    Objects.requireNonNull(pitchConstraints);
    Objects.requireNonNull(targetFieldCentricYaw);

    if (flywheelSetpoint.isEmpty() && flywheelConstraints.isPresent()) {
      throw new IllegalArgumentException("Flywheel constraints require a flywheel setpoint");
    }
    if (pitchSetpoint.isEmpty() && pitchConstraints.isPresent()) {
      throw new IllegalArgumentException("Pitch constraints require a pitch setpoint");
    }
  }

  public PitchSetpoint resolvedPitchSetpoint() {
    return pitchSetpoint.orElse(DEFAULT_PITCH_SETPOINT);
  }

  public static ShooterControlRequest idle() {
    return new ShooterControlRequest(
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public record FlywheelSetpoint(double velocityRadPerSec, double toleranceRadPerSec) {}

  public record FlywheelConstraints(double maxAccelerationRadPerSec2) {}

  public record PitchSetpoint(double positionRad, double toleranceRad) {}

  public record PitchConstraints(double maxVelocityRadPerSec, double maxAccelerationRadPerSec2) {}

  public static double flywheelMeter2Rad(double meter) {
    return meter / ShooterConfig.FLYWHEEL_RADIUS_METER;
  }

  public static double flywheelRad2Meter(double rad) {
    return rad * ShooterConfig.FLYWHEEL_RADIUS_METER;
  }
}
