// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.genericsystem;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.*;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import com.nextinnovation.team8214.util.driver.CanId;
import com.nextinnovation.team8214.util.driver.Phoenix6Helper;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Voltage;
import java.util.ArrayList;
import java.util.List;

public class GenericArmIOKraken implements GenericArmIO {
  private final String name;

  private final TalonFX master;
  private final List<TalonFX> slaves = new ArrayList<>();

  private final TalonFXConfiguration masterConfig;

  private final StatusSignal<AngularVelocity> vel;
  private final StatusSignal<AngularVelocity> rotorVelocity;
  private final StatusSignal<Angle> position;
  private final StatusSignal<Voltage> outputVoltage;
  private final StatusSignal<Current> supplyCurrent;
  private final StatusSignal<Current> statorCurrent;

  private final PositionTorqueCurrentFOC positionSetter;
  private final DynamicMotionMagicTorqueCurrentFOC motionMagicSetter;
  private final TorqueCurrentFOC currentSetter = new TorqueCurrentFOC(0.0);
  private final VoltageOut voltageSetter = new VoltageOut(0.0);
  private final NeutralOut neutralSetter = new NeutralOut();
  private final List<Follower> slaveSetters = new ArrayList<>();
  private final String wrappedName;

  private boolean wantInputOnly = false;

  public GenericArmIOKraken(
      String name, CanId id, TalonFXConfiguration talonConfig, double homePositionRad) {
    this.name = name;
    master = new TalonFX(id.id(), id.bus());
    masterConfig = talonConfig;
    this.wrappedName = "[" + name + "]";

    var homePositionRotation = Units.radiansToRotations(homePositionRad);

    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " clear sticky fault", master::clearStickyFaults);
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " config", () -> master.getConfigurator().apply(masterConfig));
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " home position", () -> master.setPosition(homePositionRotation));

    positionSetter = new PositionTorqueCurrentFOC(homePositionRotation);
    motionMagicSetter = new DynamicMotionMagicTorqueCurrentFOC(homePositionRotation, 0.0, 0.0);

    vel = master.getVelocity();
    rotorVelocity = master.getRotorVelocity();
    position = master.getPosition();
    outputVoltage = master.getMotorVoltage();
    supplyCurrent = master.getSupplyCurrent();
    statorCurrent = master.getStatorCurrent();

    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " set signals update frequency",
        () ->
            BaseStatusSignal.setUpdateFrequencyForAll(
                100.0, vel, rotorVelocity, position, outputVoltage, supplyCurrent, statorCurrent));

    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " optimize CAN utilization", master::optimizeBusUtilization);
  }

  public GenericArmIOKraken withFollower(CanId id, boolean isInvert) {
    var slave = new TalonFX(id.id(), id.bus());

    var slaveConfig = new TalonFXConfiguration();
    slaveConfig.MotorOutput = masterConfig.MotorOutput;
    slaveConfig.CurrentLimits = masterConfig.CurrentLimits;
    slaveConfig.Slot0 = masterConfig.Slot0;
    slaveConfig.Slot1 = masterConfig.Slot1;
    slaveConfig.Slot2 = masterConfig.Slot2;
    slaveConfig.Voltage = masterConfig.Voltage;
    slaveConfig.TorqueCurrent = masterConfig.TorqueCurrent;

    var slaveSetter =
        new Follower(
            master.getDeviceID(),
            isInvert ? MotorAlignmentValue.Opposed : MotorAlignmentValue.Aligned);

    var wrappedName = "[" + name + "Slave" + slaves.size() + "]";
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " clear sticky fault", slave::clearStickyFaults);
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " config", () -> slave.getConfigurator().apply(slaveConfig));
    Phoenix6Helper.checkErrorAndRetry(wrappedName + " follow", () -> slave.setControl(slaveSetter));
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " optimize CAN utilization", slave::optimizeBusUtilization);

    slaves.add(slave);
    slaveSetters.add(slaveSetter);

    return this;
  }

  public GenericArmIOKraken withInputOnly() {
    wantInputOnly = true;

    return this;
  }

  @Override
  public void updateInputs(GenericArmIOInputs inputs) {
    inputs.motorConnected =
        BaseStatusSignal.refreshAll(
                vel, rotorVelocity, position, outputVoltage, supplyCurrent, statorCurrent)
            .isOK();
    inputs.encoderConnected = false;

    inputs.velRadPerSec = Units.rotationsToRadians(vel.getValueAsDouble());
    inputs.rotorVelocityRadPerSec =
        inputs.motorConnected
            ? Units.rotationsToRadians(rotorVelocity.getValueAsDouble())
            : Double.NaN;
    inputs.positionRad = Units.rotationsToRadians(position.getValueAsDouble());
    inputs.outputVoltageVolt = outputVoltage.getValueAsDouble();
    inputs.supplyCurrentAmp = supplyCurrent.getValueAsDouble();
    inputs.statorCurrentAmp = statorCurrent.getValueAsDouble();

    if (!slaves.isEmpty()) {
      var followersConnected = new boolean[slaves.size()];
      var followersSupplyCurrentAmp = new double[slaves.size()];
      for (var i = 0; i < slaves.size(); i++) {
        final var supplyCurrent = slaves.get(i).getSupplyCurrent();
        followersConnected[i] = supplyCurrent.refresh().getStatus().isOK();
        followersSupplyCurrentAmp[i] =
            followersConnected[i] ? supplyCurrent.getValueAsDouble() : 0.0;
      }
      inputs.followersConnected = followersConnected;
      inputs.followersSupplyCurrentAmp = followersSupplyCurrentAmp;
    } else {
      inputs.followersConnected = new boolean[0];
      inputs.followersSupplyCurrentAmp = new double[0];
    }
  }

  @Override
  public void setPosition(double positionRad, double feedforward) {
    if (!wantInputOnly) {
      master.setControl(
          positionSetter
              .withPosition(Units.radiansToRotations(positionRad))
              .withFeedForward(feedforward));
      setSlavesFollow();
    }
  }

  @Override
  public void setPosition(
      double positionRad, double velRadPerSec, double accelRadPerSec2, double feedforward) {
    if (!wantInputOnly) {
      master.setControl(
          motionMagicSetter
              .withPosition(Units.radiansToRotations(positionRad))
              .withFeedForward(feedforward)
              .withVelocity(Units.radiansToRotations(velRadPerSec))
              .withAcceleration(Units.radiansToRotations(accelRadPerSec2)));
      setSlavesFollow();
    }
  }

  @Override
  public void setPdf(double kp, double kd, double ks, double kg) {
    masterConfig.Slot0.kP = kp;
    masterConfig.Slot0.kD = kd;
    masterConfig.Slot0.kS = ks;
    masterConfig.Slot0.kG = kg;
    master.getConfigurator().apply(masterConfig);
  }

  @Override
  public void setCurrent(double currentAmp) {
    if (!wantInputOnly) {
      master.setControl(currentSetter.withOutput(currentAmp));
      setSlavesFollow();
    }
  }

  @Override
  public void setVoltage(double voltageVolt) {
    if (!wantInputOnly) {
      master.setControl(voltageSetter.withOutput(voltageVolt));
      setSlavesFollow();
    }
  }

  @Override
  public void stop() {
    master.setControl(neutralSetter);
    setSlavesFollow();
  }

  private void setSlavesFollow() {
    for (int i = 0; i < slaves.size(); i++) {
      slaves.get(i).setControl(slaveSetters.get(i));
    }
  }

  @Override
  public void home(double homeAngleRad) {
    Phoenix6Helper.checkErrorAndRetry(
        wrappedName + " home position",
        () -> master.setPosition(Units.radiansToRotations(homeAngleRad)));
  }
}
