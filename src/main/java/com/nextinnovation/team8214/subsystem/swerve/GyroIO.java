// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: BSD-3-Clause AND MIT

package com.nextinnovation.team8214.subsystem.swerve;

import edu.wpi.first.math.geometry.Rotation2d;
import org.littletonrobotics.junction.AutoLog;

public interface GyroIO {
  @AutoLog
  class GyroIOInputs {
    public boolean connected = false;
    public Rotation2d yawPosition = new Rotation2d();
    public Rotation2d rollPosition = new Rotation2d();
    public Rotation2d pitchPosition = new Rotation2d();
    public double yawVelocityRadPerSec = 0.0;
  }

  default void updateInputs(GyroIOInputs inputs) {}
}
