// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.genericsystem;

import org.littletonrobotics.junction.AutoLog;

public interface GenericRollerIO {
  @AutoLog
  class GenericRollerIOInputs {
    public boolean connected;

    public double velRadPerSec;
    public double rotorVelocityRadPerSec = Double.NaN;
    public double outputVoltageVolt;
    public double supplyCurrentAmp;
    public double statorCurrentAmp;
    public double tempCelsius;

    public boolean[] followersConnected = new boolean[0];
    public double[] followersSupplyCurrentAmp = new double[0];
  }

  default void updateInputs(GenericRollerIOInputs inputs) {}

  default void setPdf(double kp, double kd, double kv, double ks) {}

  default void setVoltage(double voltageVolt) {}

  default void setDutyCycle(double dutyCycle) {}

  default void setVel(double velRadPerSec) {}

  default void setVelCurrent(double velRadPerSec) {}

  default void setVel(double velRadPerSec, double accelRadPerSec2) {}

  default void setCurrent(double currentAmp) {}

  default void stop() {}
}
