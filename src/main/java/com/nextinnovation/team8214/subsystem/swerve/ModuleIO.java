// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import edu.wpi.first.math.geometry.Rotation2d;
import org.littletonrobotics.junction.AutoLog;

interface ModuleIO {
  @AutoLog
  class ModuleIOInputs {
    boolean driveMotorConnected = false;
    boolean steerMotorConnected = false;
    boolean steerEncoderConnected = false;

    double driveVelRadPerSec = 0.0;
    double drivePositionRad = 0.0;
    double driveOutputVoltageVolt = 0.0;
    double driveSupplyCurrentAmp = 0.0;
    double driveStatorCurrentAmp = 0.0;
    double driveRotorVelocityRadPerSec = Double.NaN;

    double steerVelRadPerSec = 0.0;
    Rotation2d steerAbsPosition = new Rotation2d();
    double steerOutputVoltageVolt = 0.0;
    double steerSupplyCurrentAmp = 0.0;
    double steerStatorCurrentAmp = 0.0;
    double steerRotorVelocityRadPerSec = Double.NaN;
  }

  default void updateInputs(ModuleIOInputs inputs) {}

  default void setDrivePdf(double kp, double kd, double kv, double ks) {}

  default void setSteerPdf(double kp, double kd, double ks) {}

  default void setDriveVelocity(double velRadPerSec) {}

  default void setSteerPosition(double angleRad) {}

  default boolean setDriveNeutralMode(boolean wantBrake) {
    return true;
  }

  default void stop() {}

  default void flushAutoDriveCurrentLimit() {}

  default void flushTeleopDriveCurrentLimit() {}
}
