// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import static edu.wpi.first.units.Units.RadiansPerSecond;

import edu.wpi.first.math.geometry.Rotation2d;
import java.util.function.DoubleSupplier;
import org.ironmaple.simulation.drivesims.GyroSimulation;

public class GyroIOMapleSim implements GyroIO {
  private final GyroSimulation gyroSimulation;
  private final DoubleSupplier rollRadSupplier;
  private final DoubleSupplier pitchRadSupplier;

  public GyroIOMapleSim(
      GyroSimulation gyroSimulation,
      DoubleSupplier rollRadSupplier,
      DoubleSupplier pitchRadSupplier) {
    this.gyroSimulation = gyroSimulation;
    this.rollRadSupplier = rollRadSupplier;
    this.pitchRadSupplier = pitchRadSupplier;
  }

  @Override
  public void updateInputs(GyroIOInputs inputs) {
    inputs.connected = true;
    inputs.yawPosition = gyroSimulation.getGyroReading();
    inputs.rollPosition = Rotation2d.fromRadians(rollRadSupplier.getAsDouble());
    inputs.pitchPosition = Rotation2d.fromRadians(pitchRadSupplier.getAsDouble());
    inputs.yawVelocityRadPerSec = gyroSimulation.getMeasuredAngularVelocity().in(RadiansPerSecond);
  }
}
