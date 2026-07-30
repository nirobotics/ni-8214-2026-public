// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.*;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.FeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.nextinnovation.team8214.util.driver.CanId;
import com.nextinnovation.team8214.util.driver.Phoenix6Helper;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Voltage;
import lombok.Getter;

class ModuleIOKrakenFOC implements ModuleIO {
  private final TalonFXConfiguration driveTalonConfig;
  private final TalonFXConfiguration steerTalonConfig;

  // Hardware
  private final TalonFX driveMotor;
  private final TalonFX steerMotor;
  private final CANcoder cancoder;

  // Signal
  private final StatusSignal<AngularVelocity> driveVel;
  private final StatusSignal<AngularVelocity> driveRotorVel;
  @Getter private final StatusSignal<Angle> drivePosition;
  private final StatusSignal<Voltage> driveOutputVoltage;
  private final StatusSignal<Current> driveSupplyCurrent;
  private final StatusSignal<Current> driveStatorCurrent;

  private final StatusSignal<AngularVelocity> steerVel;
  private final StatusSignal<AngularVelocity> steerRotorVel;
  @Getter private final StatusSignal<Angle> steerAbsPosition;
  private final StatusSignal<Voltage> steerOutputVoltage;
  private final StatusSignal<Current> steerSupplyCurrent;
  private final StatusSignal<Current> steerStatorCurrent;

  private final StatusSignal<Angle> steerCancoderAbsPosition;

  // Control request
  private final VelocityVoltage driveVelSetter = new VelocityVoltage(0.0);
  private final NeutralOut driveNeutralSetter = new NeutralOut();
  private final DynamicMotionMagicVoltage steerPositionSetter =
      new DynamicMotionMagicVoltage(0.0, 0.0, 0.0);
  private final NeutralOut steerNeutralSetter = new NeutralOut();

  private boolean wantInputOnly = false;

  private final String wrappedName;

  public ModuleIOKrakenFOC(
      String name,
      CanId driveId,
      CanId steerId,
      CanId cancoderId,
      SwerveConfig.ModuleConfig config) {
    wrappedName = "[" + name + "]";
    driveMotor = new TalonFX(driveId.id(), driveId.bus());
    steerMotor = new TalonFX(steerId.id(), steerId.bus());
    cancoder = new CANcoder(cancoderId.id(), cancoderId.bus());

    // Zero drive motor position
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " zero drive position", () -> driveMotor.setPosition(0));

    // Config drive motor
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " clear drive sticky fault", driveMotor::clearStickyFaults);
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " config drive",
        () -> driveMotor.getConfigurator().apply(config.driveTalonConfig()));

    driveTalonConfig = config.driveTalonConfig();

    // Config steer motor
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " clear steer sticky fault", steerMotor::clearStickyFaults);
    var realSteerTalonConfig = config.steerTalonConfig();
    realSteerTalonConfig.Feedback.FeedbackRemoteSensorID = cancoder.getDeviceID();
    realSteerTalonConfig.Feedback.FeedbackSensorSource = FeedbackSensorSourceValue.FusedCANcoder;
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " config steer",
        () -> steerMotor.getConfigurator().apply(realSteerTalonConfig));

    steerTalonConfig = realSteerTalonConfig;

    // Config cancoder
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " config cancoder",
        () -> cancoder.getConfigurator().apply(config.cancoderConfig()));

    // Config Signal
    driveVel = driveMotor.getVelocity();
    driveRotorVel = driveMotor.getRotorVelocity();
    drivePosition = driveMotor.getPosition();
    driveOutputVoltage = driveMotor.getMotorVoltage();
    driveSupplyCurrent = driveMotor.getSupplyCurrent();
    driveStatorCurrent = driveMotor.getStatorCurrent();

    steerVel = steerMotor.getVelocity();
    steerRotorVel = steerMotor.getRotorVelocity();
    steerAbsPosition = steerMotor.getPosition();
    steerOutputVoltage = steerMotor.getMotorVoltage();
    steerSupplyCurrent = steerMotor.getSupplyCurrent();
    steerStatorCurrent = steerMotor.getStatorCurrent();

    steerCancoderAbsPosition = cancoder.getAbsolutePosition();

    // drivePosition and steerAbsPosition will be config by odometry thread
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " set signals update frequency",
        () ->
            BaseStatusSignal.setUpdateFrequencyForAll(
                100.0,
                driveVel,
                driveRotorVel,
                // drivePosition,
                driveOutputVoltage,
                driveSupplyCurrent,
                driveStatorCurrent,
                steerVel,
                steerRotorVel,
                // steerAbsPosition,
                steerOutputVoltage,
                steerSupplyCurrent,
                steerStatorCurrent));

    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " optimize drive CAN utilization", driveMotor::optimizeBusUtilization);
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " optimize steer CAN utilization", steerMotor::optimizeBusUtilization);
  }

  ModuleIOKrakenFOC withInputOnly() {
    wantInputOnly = false;
    return this;
  }

  @Override
  public void updateInputs(ModuleIOInputs inputs) {
    inputs.driveMotorConnected =
        BaseStatusSignal.refreshAll(
                driveVel,
                driveRotorVel,
                drivePosition,
                driveOutputVoltage,
                driveSupplyCurrent,
                driveStatorCurrent)
            .isOK();
    inputs.steerMotorConnected =
        BaseStatusSignal.refreshAll(
                steerVel,
                steerRotorVel,
                steerAbsPosition,
                steerOutputVoltage,
                steerSupplyCurrent,
                steerStatorCurrent)
            .isOK();
    inputs.steerEncoderConnected = BaseStatusSignal.refreshAll(steerCancoderAbsPosition).isOK();

    inputs.driveVelRadPerSec = Units.rotationsToRadians(driveVel.getValueAsDouble());
    inputs.drivePositionRad = Units.rotationsToRadians(drivePosition.getValueAsDouble());
    inputs.driveOutputVoltageVolt = driveOutputVoltage.getValueAsDouble();
    inputs.driveSupplyCurrentAmp = driveSupplyCurrent.getValueAsDouble();
    inputs.driveStatorCurrentAmp = driveStatorCurrent.getValueAsDouble();
    inputs.driveRotorVelocityRadPerSec =
        inputs.driveMotorConnected
            ? Units.rotationsToRadians(driveRotorVel.getValueAsDouble())
            : Double.NaN;

    inputs.steerVelRadPerSec = Units.rotationsToRadians(steerVel.getValueAsDouble());
    inputs.steerAbsPosition = Rotation2d.fromRotations(steerAbsPosition.getValueAsDouble());
    inputs.steerOutputVoltageVolt = steerOutputVoltage.getValueAsDouble();
    inputs.steerSupplyCurrentAmp = steerSupplyCurrent.getValueAsDouble();
    inputs.steerStatorCurrentAmp = steerStatorCurrent.getValueAsDouble();
    inputs.steerRotorVelocityRadPerSec =
        inputs.steerMotorConnected
            ? Units.rotationsToRadians(steerRotorVel.getValueAsDouble())
            : Double.NaN;
  }

  @Override
  public void setDrivePdf(double kp, double kd, double kv, double ks) {
    driveTalonConfig.Slot0.kP = kp;
    driveTalonConfig.Slot0.kD = kd;
    driveTalonConfig.Slot0.kV = kv;
    driveTalonConfig.Slot0.kS = ks;
    driveMotor.getConfigurator().apply(driveTalonConfig);
  }

  @Override
  public void setSteerPdf(double kp, double kd, double ks) {
    steerTalonConfig.Slot0.kP = kp;
    steerTalonConfig.Slot0.kD = kd;
    steerTalonConfig.Slot0.kS = ks;
    steerMotor.getConfigurator().apply(steerTalonConfig);
  }

  @Override
  public void setDriveVelocity(double velRadPerSec) {
    if (!wantInputOnly) {
      driveMotor.setControl(driveVelSetter.withVelocity(Units.radiansToRotations(velRadPerSec)));
    }
  }

  @Override
  public void setSteerPosition(double angleRad) {
    if (!wantInputOnly) {
      steerMotor.setControl(
          steerPositionSetter
              .withPosition(Units.radiansToRotations(angleRad))
              .withVelocity(10.0)
              .withAcceleration(100.0));
    }
  }

  @Override
  public boolean setDriveNeutralMode(boolean wantBrake) {
    driveTalonConfig.MotorOutput.NeutralMode =
        wantBrake ? NeutralModeValue.Brake : NeutralModeValue.Coast;
    return driveMotor.getConfigurator().apply(driveTalonConfig.MotorOutput).isOK();
  }

  @Override
  public void stop() {
    driveMotor.setControl(driveNeutralSetter);
    steerMotor.setControl(steerNeutralSetter);
  }

  @Override
  public void flushAutoDriveCurrentLimit() {
    driveTalonConfig.CurrentLimits.StatorCurrentLimit = 300.0;
    driveTalonConfig.CurrentLimits.StatorCurrentLimitEnable = true;

    driveTalonConfig.CurrentLimits.SupplyCurrentLimitEnable = false;

    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " flush auto drive current limit",
        () -> driveMotor.getConfigurator().apply(driveTalonConfig.CurrentLimits),
        2);
  }

  @Override
  public void flushTeleopDriveCurrentLimit() {
    driveTalonConfig.CurrentLimits.StatorCurrentLimit = 100.0;
    driveTalonConfig.CurrentLimits.StatorCurrentLimitEnable = true;

    driveTalonConfig.CurrentLimits.SupplyCurrentLimit = 40.0;
    driveTalonConfig.CurrentLimits.SupplyCurrentLimitEnable = true;

    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " flush teleop drive current limit",
        () -> driveMotor.getConfigurator().apply(driveTalonConfig.CurrentLimits),
        2);
  }
}
