// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.genericsystem;

import org.littletonrobotics.junction.AutoLog;

public interface GenericElevatorIO {
  @AutoLog
  class GenericElevatorIOInputs {
    public boolean connected;

    public double velMeterPerSec;
    public double rotorVelocityRadPerSec = Double.NaN;
    public double positionMeter;
    public double outputVoltageVolt;
    public double supplyCurrentAmp;
    public double statorCurrentAmp;

    public boolean[] followersConnected;
    public double[] followersSupplyCurrentAmp;
  }

  default void updateInputs(GenericElevatorIOInputs inputs) {}

  default void setPosition(double positionMeter, double feedforward) {}

  default void setPosition(
      double positionMeter, double velMeterPerSec, double accelMeterPerSec2, double feedforward) {}

  default void setPdf(double kp, double kd, double ks, double kg) {}

  default void setVoltage(double voltageVolt) {}

  default void setCurrent(double currentAmp) {}

  default void stop() {}

  default void home(double homePositionMeter) {}
}
