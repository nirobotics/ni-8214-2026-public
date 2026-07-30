// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.hopper;

import org.littletonrobotics.junction.AutoLog;

interface HopperIO {
  @AutoLog
  class HopperIOInputs {
    public boolean floorSensorConnected;
    public boolean floorSensorDetected;
    public double floorSensorDistanceMeter;
    public double floorSensorSignalStrength;

    public boolean feederSensorConnected;
    public boolean feederSensorDetected;
    public double feederSensorDistanceMeter;
    public double feederSensorSignalStrength;
  }

  default void updateInputs(HopperIOInputs inputs) {}
}
