// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.hopper;

final class HopperConfig {
  static final String LOG_ROOT = "subsystem/hopper";
  static final double SENSOR_PROXIMITY_THRESHOLD_METER = 0.57;
  static final double SENSOR_PROXIMITY_HYSTERESIS_METER = 0.01;
  static final double SENSOR_MIN_SIGNAL_STRENGTH = 3000.0;
  static final double PRELOAD_DEBOUNCE_SEC = 0.25;
  static final double AUTO_FEEDER_SENSOR_EMPTY_DEBOUNCE_SEC = 0.21;
  static final int SIM_FEEDER_SENSOR_CAPACITY = 4;
}
