// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.genericsystem;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.util.EqualsUtil;
import com.nextinnovation.team8214.util.IdealSimMotor;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;

public class GenericArmIOSim implements GenericArmIO {
  private final IdealSimMotor sim;

  private TrapezoidProfile profile =
      new TrapezoidProfile(new TrapezoidProfile.Constraints(0.0, 0.0));
  private TrapezoidProfile.State lastSetpoint = new TrapezoidProfile.State(0.0, 0.0);
  private boolean hasProfileInit = false;
  private boolean needResetProfile = false;

  private double lastGoalPositionRad = 0.0;

  public GenericArmIOSim(double startingPositionRad) {
    sim = new IdealSimMotor(startingPositionRad);
  }

  public GenericArmIOSim(double startingPositionRad, double minPositionRad, double maxPositionRad) {
    sim = new IdealSimMotor(startingPositionRad, minPositionRad, maxPositionRad);
  }

  public void setVoltage() {}

  @Override
  public void updateInputs(GenericArmIOInputs inputs) {
    if (DriverStation.isDisabled()) {
      stop();
    }
    sim.update(Config.LOOP_PERIOD_SEC);

    inputs.motorConnected = true;
    inputs.encoderConnected = true;

    inputs.velRadPerSec = sim.getVelocity();
    inputs.positionRad = sim.getPosition();
    inputs.outputVoltageVolt = sim.getVoltageVolt();
    inputs.supplyCurrentAmp = sim.getCurrentAmp();
  }

  @Override
  public void setPosition(double positionRad, double feedforward) {
    needResetProfile = true;
    sim.setPosition(positionRad);
  }

  @Override
  public void setPosition(
      double positionRad, double velRadPerSec, double accelRadPerSec2, double feedforward) {
    needResetProfile = lastGoalPositionRad != positionRad;

    if (!hasProfileInit) {
      profile =
          new TrapezoidProfile(new TrapezoidProfile.Constraints(velRadPerSec, accelRadPerSec2));
      lastSetpoint = new TrapezoidProfile.State(sim.getPosition(), 0.0);
      lastGoalPositionRad = positionRad;

      hasProfileInit = true;

      return;
    } else if (needResetProfile) {
      profile =
          new TrapezoidProfile(new TrapezoidProfile.Constraints(velRadPerSec, accelRadPerSec2));
      lastSetpoint = new TrapezoidProfile.State(sim.getPosition(), lastSetpoint.velocity);
      lastGoalPositionRad = positionRad;
    }

    var setpoint =
        profile.calculate(
            Config.LOOP_PERIOD_SEC, lastSetpoint, new TrapezoidProfile.State(positionRad, 0.0));

    if (EqualsUtil.epsilonEquals(sim.getPosition(), positionRad, Units.degreesToRadians(3.0))) {
      setpoint = new TrapezoidProfile.State(positionRad, 0.0);
      sim.setPosition(setpoint.position);
      setVoltage(0.0);
    } else {
      sim.setPosition(setpoint.position);
    }

    lastGoalPositionRad = positionRad;
    lastSetpoint = setpoint;
  }

  @Override
  public void setVoltage(double voltageVolt) {
    needResetProfile = true;
    sim.setVoltageVolt(MathUtil.clamp(voltageVolt, -12.0, 12.0));
  }

  @Override
  public void setCurrent(double currentAmp) {
    needResetProfile = true;
    sim.setCurrentAmp(currentAmp);
  }

  @Override
  public void stop() {
    needResetProfile = true;
    setVoltage(0.0);
  }
}
