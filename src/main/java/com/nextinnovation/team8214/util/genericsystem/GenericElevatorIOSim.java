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

public class GenericElevatorIOSim implements GenericElevatorIO {
  private final IdealSimMotor sim;

  private TrapezoidProfile profile =
      new TrapezoidProfile(new TrapezoidProfile.Constraints(0.0, 0.0));
  private TrapezoidProfile.State lastSetpoint = new TrapezoidProfile.State(0.0, 0.0);
  private boolean hasProfileInit = false;
  private boolean needResetProfile = false;

  private double lastGoalPositionMeter = 0.0;

  public GenericElevatorIOSim() {
    this(0.0);
  }

  public GenericElevatorIOSim(double startingHeightMeter) {
    this(startingHeightMeter, Double.POSITIVE_INFINITY);
  }

  public GenericElevatorIOSim(double startingHeightMeter, double maxHeightMeter) {
    sim = new IdealSimMotor(startingHeightMeter, 0.0, maxHeightMeter);
  }

  @Override
  public void updateInputs(GenericElevatorIOInputs inputs) {
    if (DriverStation.isDisabled()) {
      stop();
    }

    sim.update(Config.LOOP_PERIOD_SEC);

    inputs.connected = true;

    inputs.velMeterPerSec = sim.getVelocity();
    inputs.positionMeter = sim.getPosition();
    inputs.outputVoltageVolt = sim.getVoltageVolt();
    inputs.supplyCurrentAmp = sim.getCurrentAmp();
  }

  @Override
  public void setPosition(double positionMeter, double feedforward) {
    needResetProfile = true;
    sim.setPosition(positionMeter);
  }

  @Override
  public void setPosition(
      double positionMeter, double velMeterPerSec, double accelMeterPerSec2, double feedforward) {
    needResetProfile = lastGoalPositionMeter != positionMeter;

    if (!hasProfileInit) {
      profile =
          new TrapezoidProfile(new TrapezoidProfile.Constraints(velMeterPerSec, accelMeterPerSec2));
      lastSetpoint = new TrapezoidProfile.State(sim.getPosition(), 0.0);
      lastGoalPositionMeter = positionMeter;

      hasProfileInit = true;

      return;
    } else if (needResetProfile) {
      profile =
          new TrapezoidProfile(new TrapezoidProfile.Constraints(velMeterPerSec, accelMeterPerSec2));
      lastSetpoint = new TrapezoidProfile.State(sim.getPosition(), lastSetpoint.velocity);
      lastGoalPositionMeter = positionMeter;
    }

    var setpoint =
        profile.calculate(
            Config.LOOP_PERIOD_SEC, lastSetpoint, new TrapezoidProfile.State(positionMeter, 0.0));

    if (EqualsUtil.epsilonEquals(sim.getPosition(), positionMeter, 0.03)) {
      setpoint = new TrapezoidProfile.State(positionMeter, 0.0);
      sim.setPosition(setpoint.position);
      setVoltage(0.0);
    } else {
      sim.setPosition(setpoint.position);
    }

    lastGoalPositionMeter = positionMeter;
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

  private double meter2Rad(double meter, double meterPerRotation) {
    return Units.degreesToRadians(meter / meterPerRotation);
  }
}
