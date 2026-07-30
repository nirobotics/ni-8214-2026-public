// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.wpilibj.DriverStation;

public record AllianceValue<T>(T blue, T red) {
  public T get() {
    var alliance = DriverStation.getAlliance();
    return get(alliance.isPresent() && alliance.get() == DriverStation.Alliance.Blue);
  }

  public T get(DriverStation.Alliance alliance) {
    return get(alliance == DriverStation.Alliance.Blue);
  }

  public T get(boolean isBlueAlliance) {
    return isBlueAlliance ? blue : red;
  }
}
