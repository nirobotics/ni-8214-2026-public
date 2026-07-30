// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.genericsystem;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.util.IdealSimMotor;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.DriverStation;

public class GenericRollerIOSim implements GenericRollerIO {
  private final IdealSimMotor sim;
  private final double gearRatio;

  public GenericRollerIOSim(double gearRatio) {
    sim = new IdealSimMotor();
    this.gearRatio = gearRatio;
  }

  public GenericRollerIOSim() {
    this(1.0);
  }

  @Override
  public void updateInputs(GenericRollerIOInputs inputs) {
    if (DriverStation.isDisabled()) {
      stop();
    }

    sim.update(Config.LOOP_PERIOD_SEC);

    inputs.connected = true;

    inputs.velRadPerSec = sim.getVelocity();
    inputs.outputVoltageVolt = sim.getVoltageVolt();
    inputs.supplyCurrentAmp = sim.getCurrentAmp();
  }

  @Override
  public void setVoltage(double voltageVolt) {
    sim.setVoltageVolt(MathUtil.clamp(voltageVolt / gearRatio, -12.0, 12.0));
  }

  @Override
  public void setDutyCycle(double dutyCycle) {
    setVoltage(dutyCycle * 12.0);
  }

  @Override
  public void setVel(double velRadPerSec) {
    sim.setVelocity(velRadPerSec);
  }

  @Override
  public void setVel(double velRadPerSec, double accelRadPerSec2) {
    sim.setVelocity(
        sim.getVelocity()
            + MathUtil.clamp(
                velRadPerSec - sim.getVelocity(),
                -accelRadPerSec2 * Config.LOOP_PERIOD_SEC,
                accelRadPerSec2 * Config.LOOP_PERIOD_SEC));
  }

  @Override
  public void setVelCurrent(double velRadPerSec) {
    setVel(velRadPerSec);
  }

  @Override
  public void setCurrent(double currentAmp) {
    sim.setCurrentAmp(currentAmp);
  }

  @Override
  public void stop() {
    setVoltage(0.0);
  }
}
