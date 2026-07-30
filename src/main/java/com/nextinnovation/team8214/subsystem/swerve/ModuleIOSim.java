// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import static edu.wpi.first.units.Units.*;

import com.nextinnovation.team8214.Config;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.DriverStation;
import org.ironmaple.simulation.drivesims.SwerveModuleSimulation;
import org.ironmaple.simulation.motorsims.SimulatedMotorController;

public class ModuleIOSim implements ModuleIO {
  private final SwerveModuleSimulation moduleSim;
  private final SimulatedMotorController.GenericMotorController driveSim;
  private final SimulatedMotorController.GenericMotorController steerSim;

  private final PIDController drivePid;
  private final PIDController steerPid;

  private final SlewRateLimiter driveVoltageLimiter = new SlewRateLimiter(2.5);

  private double driveAppliedVoltageVolt = 0.0;
  private double steerAppliedVoltageVolt = 0.0;

  private double kv;
  private double ks;

  ModuleIOSim(SwerveModuleSimulation moduleSim) {
    this.moduleSim = moduleSim;
    var driveGains = SwerveConfig.getDriveGains();
    var steerGains = SwerveConfig.getSteerGains();

    drivePid = new PIDController(driveGains.kp(), 0.0, driveGains.kd(), Config.LOOP_PERIOD_SEC);
    kv = driveGains.kv();
    ks = driveGains.ks();
    steerPid = new PIDController(steerGains.kp(), 0.0, steerGains.kd(), Config.LOOP_PERIOD_SEC);

    driveSim = moduleSim.useGenericMotorControllerForDrive();
    steerSim = moduleSim.useGenericControllerForSteer();

    steerPid.enableContinuousInput(-Math.PI, Math.PI);
  }

  @Override
  public void updateInputs(ModuleIO.ModuleIOInputs inputs) {
    if (DriverStation.isDisabled()) {
      stop();
    }

    if (DriverStation.isDisabled()) {
      setDriveVoltage(driveVoltageLimiter.calculate(driveAppliedVoltageVolt));
    } else {
      driveVoltageLimiter.reset(driveAppliedVoltageVolt);
    }

    driveSim.requestVoltage(Volt.of(driveAppliedVoltageVolt));
    steerSim.requestVoltage(Volt.of(steerAppliedVoltageVolt));

    inputs.driveMotorConnected = true;
    inputs.drivePositionRad = moduleSim.getDriveWheelFinalPosition().in(Radians);
    inputs.driveVelRadPerSec = moduleSim.getDriveWheelFinalSpeed().in(RadiansPerSecond);
    inputs.driveOutputVoltageVolt = driveAppliedVoltageVolt;
    inputs.driveSupplyCurrentAmp = moduleSim.getDriveMotorSupplyCurrent().in(Amps);
    inputs.driveStatorCurrentAmp = moduleSim.getDriveMotorStatorCurrent().in(Amps);
    inputs.driveRotorVelocityRadPerSec =
        moduleSim.getDriveEncoderUnGearedSpeed().in(RadiansPerSecond);

    inputs.steerEncoderConnected = true;
    inputs.steerMotorConnected = true;
    inputs.steerAbsPosition = moduleSim.getSteerAbsoluteFacing();
    inputs.steerVelRadPerSec = moduleSim.getSteerAbsoluteEncoderSpeed().in(RadiansPerSecond);
    inputs.steerOutputVoltageVolt = steerAppliedVoltageVolt;
    inputs.steerSupplyCurrentAmp = moduleSim.getSteerMotorSupplyCurrent().in(Amps);
    inputs.steerStatorCurrentAmp = moduleSim.getSteerMotorStatorCurrent().in(Amps);
    inputs.steerRotorVelocityRadPerSec =
        moduleSim.getSteerRelativeEncoderVelocity().in(RadiansPerSecond);
  }

  private void setDriveVoltage(double voltageVolt) {
    driveAppliedVoltageVolt = voltageVolt;
  }

  private void setSteerVoltage(double voltageVolt) {
    steerAppliedVoltageVolt = voltageVolt;
  }

  @Override
  public void setDrivePdf(double kp, double kd, double kv, double ks) {
    drivePid.setPID(kp, 0.0, kd);
    this.kv = kv;
    this.ks = ks;
  }

  @Override
  public void setSteerPdf(double kp, double kd, double ks) {
    steerPid.setPID(kp, 0.0, kd);
  }

  @Override
  public void setDriveVelocity(double velRadPerSec) {
    var driveAppliedVoltageVolt =
        drivePid.calculate(moduleSim.getDriveWheelFinalSpeed().in(RadiansPerSecond), velRadPerSec)
            + velRadPerSec * kv
            + Math.signum(velRadPerSec) * ks;

    setDriveVoltage(driveAppliedVoltageVolt);
  }

  @Override
  public void setSteerPosition(double angleRad) {
    setSteerVoltage(steerPid.calculate(moduleSim.getSteerAbsoluteFacing().getRadians(), angleRad));
  }

  @Override
  public void stop() {
    setDriveVoltage(0.0);
    setSteerVoltage(0.0);
  }
}
