// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import com.nextinnovation.cyberpower.EnergySubsystem;
import com.nextinnovation.cyberpower.MotorType;
import com.nextinnovation.team8214.util.Alert;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import org.littletonrobotics.junction.Logger;

class Module {
  private static final LoggedTunableNumber driveKp =
      new LoggedTunableNumber(SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/module/driveKp");
  private static final LoggedTunableNumber driveKd =
      new LoggedTunableNumber(SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/module/driveKd");
  private static final LoggedTunableNumber driveKv =
      new LoggedTunableNumber(SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/module/driveKv");
  private static final LoggedTunableNumber driveKs =
      new LoggedTunableNumber(SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/module/driveKs");
  private static final LoggedTunableNumber steerKp =
      new LoggedTunableNumber(SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/module/steerKp");
  private static final LoggedTunableNumber steerKd =
      new LoggedTunableNumber(SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/module/steerKd");
  private static final LoggedTunableNumber steerKs =
      new LoggedTunableNumber(SwerveConfig.LOG_GROUP, SwerveConfig.LOG_ROOT + "/module/steerKs");

  static {
    var driveSlot = SwerveConfig.getDriveTalonConfig().Slot0;
    driveKp.initDefault(driveSlot.kP);
    driveKd.initDefault(driveSlot.kD);
    driveKv.initDefault(driveSlot.kV);
    driveKs.initDefault(driveSlot.kS);

    var steerSlot = SwerveConfig.getSteerTalonNoEncoderConfig().Slot0;
    steerKp.initDefault(steerSlot.kP);
    steerKd.initDefault(steerSlot.kD);
    steerKs.initDefault(steerSlot.kS);
  }

  private final String name;

  private final ModuleIO io;
  private final ModuleIOInputsAutoLogged inputs = new ModuleIOInputsAutoLogged();

  private final Alert driveMotorOfflineAlert;
  private final Alert steerMotorOfflineAlert;
  private final Alert steerEncoderOfflineAlert;

  Module(ModuleIO io, String name, EnergySubsystem energySubsystem) {
    this.io = io;
    this.name = "module" + name;

    energySubsystem.registerLeaderMotor(
        this.name + "Drive",
        MotorType.KRAKEN_X60_FOC,
        SwerveConfig.DRIVE_REDUCTION,
        () -> inputs.driveMotorConnected,
        () -> inputs.driveSupplyCurrentAmp,
        () -> inputs.driveStatorCurrentAmp,
        () -> inputs.driveRotorVelocityRadPerSec);

    energySubsystem.registerLeaderMotor(
        this.name + "Steer",
        MotorType.KRAKEN_X44_FOC,
        SwerveConfig.STEER_REDUCTION,
        () -> inputs.steerMotorConnected,
        () -> inputs.steerSupplyCurrentAmp,
        () -> inputs.steerStatorCurrentAmp,
        () -> inputs.steerRotorVelocityRadPerSec);

    driveMotorOfflineAlert =
        new Alert(this.name + " drive motor offline!", Alert.AlertType.WARNING);
    steerMotorOfflineAlert =
        new Alert(this.name + " steer motor offline!", Alert.AlertType.WARNING);
    steerEncoderOfflineAlert =
        new Alert(this.name + " steer encoder offline!", Alert.AlertType.WARNING);
  }

  void updateInputs() {
    io.updateInputs(inputs);
    Logger.processInputs(SwerveConfig.LOG_ROOT + "/" + name, inputs);

    // Update gains when changed during live debugging
    LoggedTunableNumber.ifChanged(
        hashCode(),
        () -> io.setDrivePdf(driveKp.get(), driveKd.get(), driveKv.get(), driveKs.get()),
        driveKp,
        driveKd,
        driveKv,
        driveKs);

    LoggedTunableNumber.ifChanged(
        hashCode(),
        () -> io.setSteerPdf(steerKp.get(), steerKd.get(), steerKs.get()),
        steerKp,
        steerKd,
        steerKs);

    // Display alerts
    driveMotorOfflineAlert.set(!inputs.driveMotorConnected);
    steerMotorOfflineAlert.set(!inputs.steerMotorConnected);
    steerEncoderOfflineAlert.set(!inputs.steerEncoderConnected);
  }

  void setState(SwerveModuleState state) {
    io.setDriveVelocity(state.speedMetersPerSecond / SwerveConfig.WHEEL_RADIUS_METER);
    io.setSteerPosition(state.angle.getRadians());
  }

  SwerveModuleState getState() {
    return new SwerveModuleState(
        inputs.driveVelRadPerSec * SwerveConfig.WHEEL_RADIUS_METER, inputs.steerAbsPosition);
  }

  double getDrivePositionRad() {
    return inputs.drivePositionRad;
  }

  double getSteerPositionRad() {
    return inputs.steerAbsPosition.getRadians();
  }

  void stop() {
    io.stop();
  }

  boolean setDriveNeutralMode(boolean wantBrake) {
    return io.setDriveNeutralMode(wantBrake);
  }

  void flushDriveCurrentLimit(boolean wantAutoMode) {
    if (wantAutoMode) {
      io.flushAutoDriveCurrentLimit();
    } else {
      io.flushTeleopDriveCurrentLimit();
    }
  }
}
