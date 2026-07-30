// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.hopper;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Ports;
import com.nextinnovation.team8214.util.Alert;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import lombok.Getter;
import org.littletonrobotics.junction.Logger;

public class Hopper extends SubsystemBase {
  private final HopperIO io;
  private final HopperIOInputsAutoLogged inputs = new HopperIOInputsAutoLogged();
  private final Debouncer floorSensorDetectedDebouncer =
      new Debouncer(HopperConfig.PRELOAD_DEBOUNCE_SEC, Debouncer.DebounceType.kRising);
  private final Debouncer autoFeederSensorEmptyDebouncer =
      new Debouncer(
          HopperConfig.AUTO_FEEDER_SENSOR_EMPTY_DEBOUNCE_SEC, Debouncer.DebounceType.kRising);
  private final Alert floorSensorOfflineAlert =
      new Alert("Floor sensor CANrange offline!", Alert.AlertType.WARNING);
  private final Alert feederSensorOfflineAlert =
      new Alert("Feeder sensor CANrange offline!", Alert.AlertType.WARNING);
  @Getter private boolean shouldPreload;
  @Getter private boolean feederSensorEmptyForAuto;

  public Hopper() {
    io =
        switch (Config.MODE) {
          case REAL ->
              new HopperIOCANrange(
                  Ports.Can.FLOOR_SENSOR_CANRANGE, Ports.Can.FEEDER_SENSOR_CANRANGE);
          case SIM -> new HopperIOSim();
          case REPLAY -> new HopperIO() {};
        };
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs(HopperConfig.LOG_ROOT, inputs);
    floorSensorOfflineAlert.set(Config.MODE == Config.Mode.REAL && !inputs.floorSensorConnected);
    feederSensorOfflineAlert.set(Config.MODE == Config.Mode.REAL && !inputs.feederSensorConnected);

    var floorSensorDetected =
        floorSensorDetectedDebouncer.calculate(
            inputs.floorSensorConnected && inputs.floorSensorDetected);
    shouldPreload =
        inputs.floorSensorConnected
            && inputs.feederSensorConnected
            && floorSensorDetected
            && !inputs.feederSensorDetected;
    feederSensorEmptyForAuto =
        autoFeederSensorEmptyDebouncer.calculate(
            isFeederSensorEmpty(inputs.feederSensorConnected, inputs.feederSensorDetected));
    Logger.recordOutput(HopperConfig.LOG_ROOT + "/shouldPreload", shouldPreload);
  }

  public void resetAutoFeederSensorEmptyDebouncer() {
    autoFeederSensorEmptyDebouncer.calculate(false);
    feederSensorEmptyForAuto = false;
  }

  static boolean isFeederSensorEmpty(boolean feederSensorConnected, boolean feederSensorDetected) {
    return feederSensorConnected && !feederSensorDetected;
  }
}
