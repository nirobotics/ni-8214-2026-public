// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.genericsystem;

import org.littletonrobotics.junction.AutoLog;

public interface GenericArmIO {
  @AutoLog
  class GenericArmIOInputs {
    public boolean motorConnected;
    public boolean encoderConnected;

    public double velRadPerSec;
    public double rotorVelocityRadPerSec = Double.NaN;
    public double positionRad;
    public double outputVoltageVolt;
    public double supplyCurrentAmp;
    public double statorCurrentAmp;

    public boolean[] followersConnected = new boolean[0];
    public double[] followersSupplyCurrentAmp = new double[0];
  }

  default void updateInputs(GenericArmIOInputs inputs) {}

  default void setPosition(double positionRad, double feedforward) {}

  default void setPosition(
      double positionRad, double velRadPerSec, double accelRadPerSec2, double feedforward) {}

  default void setPdf(double kp, double kd, double ks, double kg) {}

  default void setCurrent(double currentAmp) {}

  default void setVoltage(double voltageVolt) {}

  default void stop() {}

  default void home(double homeAngleRad) {}
}
