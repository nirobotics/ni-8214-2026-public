// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.hopper;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CANrangeConfiguration;
import com.ctre.phoenix6.configs.ProximityParamsConfigs;
import com.ctre.phoenix6.hardware.CANrange;
import com.nextinnovation.team8214.util.driver.CanId;
import com.nextinnovation.team8214.util.driver.Phoenix6Helper;
import edu.wpi.first.units.measure.Distance;

class HopperIOCANrange implements HopperIO {
  private final CANrange floorSensor;
  private final StatusSignal<Double> floorSensorSignalStrength;
  private final StatusSignal<Boolean> floorSensorDetected;
  private final StatusSignal<Distance> floorSensorDistance;

  private final CANrange feederSensor;
  private final StatusSignal<Double> feederSensorSignalStrength;
  private final StatusSignal<Boolean> feederSensorDetected;
  private final StatusSignal<Distance> feederSensorDistance;

  HopperIOCANrange(CanId floorSensorId, CanId feederSensorId) {
    floorSensor = new CANrange(floorSensorId.id(), floorSensorId.bus());
    feederSensor = new CANrange(feederSensorId.id(), feederSensorId.bus());

    var config =
        new CANrangeConfiguration()
            .withProximityParams(
                new ProximityParamsConfigs()
                    .withProximityHysteresis(HopperConfig.SENSOR_PROXIMITY_HYSTERESIS_METER)
                    .withProximityThreshold(HopperConfig.SENSOR_PROXIMITY_THRESHOLD_METER)
                    .withMinSignalStrengthForValidMeasurement(
                        HopperConfig.SENSOR_MIN_SIGNAL_STRENGTH));
    configureSensor("Floor sensor CANrange", floorSensor, config);
    configureSensor("Feeder sensor CANrange", feederSensor, config);

    floorSensorSignalStrength = floorSensor.getSignalStrength();
    floorSensorDetected = floorSensor.getIsDetected();
    floorSensorDistance = floorSensor.getDistance();
    feederSensorSignalStrength = feederSensor.getSignalStrength();
    feederSensorDetected = feederSensor.getIsDetected();
    feederSensorDistance = feederSensor.getDistance();
  }

  private static void configureSensor(String name, CANrange sensor, CANrangeConfiguration config) {
    Phoenix6Helper.checkErrorAndRetry(name + " clear sticky faults", sensor::clearStickyFaults);
    Phoenix6Helper.checkErrorAndRetry(
        name + " config", () -> sensor.getConfigurator().apply(config));
  }

  @Override
  public void updateInputs(HopperIOInputs inputs) {
    inputs.floorSensorConnected =
        BaseStatusSignal.refreshAll(
                floorSensorSignalStrength, floorSensorDetected, floorSensorDistance)
            .isOK();
    inputs.floorSensorSignalStrength = floorSensorSignalStrength.getValueAsDouble();
    inputs.floorSensorDetected = floorSensorDetected.getValueAsDouble() == 1.0;
    inputs.floorSensorDistanceMeter = floorSensorDistance.getValueAsDouble();

    inputs.feederSensorConnected =
        BaseStatusSignal.refreshAll(
                feederSensorSignalStrength, feederSensorDetected, feederSensorDistance)
            .isOK();
    inputs.feederSensorSignalStrength = feederSensorSignalStrength.getValueAsDouble();
    inputs.feederSensorDetected = feederSensorDetected.getValueAsDouble() == 1.0;
    inputs.feederSensorDistanceMeter = feederSensorDistance.getValueAsDouble();
  }
}
