// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.hopper;

import com.nextinnovation.team8214.Sim;

class HopperIOSim implements HopperIO {
  private int storedFuelCount;

  @Override
  public void updateInputs(HopperIOInputs inputs) {
    storedFuelCount = Sim.getInstance().getIntake().getGamePiecesAmount();

    inputs.floorSensorConnected = true;
    inputs.floorSensorDetected = storedFuelCount > HopperConfig.SIM_FEEDER_SENSOR_CAPACITY;
    inputs.floorSensorDistanceMeter =
        HopperConfig.SENSOR_PROXIMITY_THRESHOLD_METER
            + (inputs.floorSensorDetected ? -1.0 : 1.0)
                * HopperConfig.SENSOR_PROXIMITY_HYSTERESIS_METER;
    inputs.floorSensorSignalStrength = HopperConfig.SENSOR_MIN_SIGNAL_STRENGTH;

    inputs.feederSensorConnected = true;
    inputs.feederSensorDetected = storedFuelCount > 0;
    inputs.feederSensorDistanceMeter =
        HopperConfig.SENSOR_PROXIMITY_THRESHOLD_METER
            + (inputs.feederSensorDetected ? -1.0 : 1.0)
                * HopperConfig.SENSOR_PROXIMITY_HYSTERESIS_METER;
    inputs.feederSensorSignalStrength = HopperConfig.SENSOR_MIN_SIGNAL_STRENGTH;
  }
}
