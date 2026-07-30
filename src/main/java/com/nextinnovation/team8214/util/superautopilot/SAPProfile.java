// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.superautopilot;

import com.therekrab.autopilot.APConstraints;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.Distance;

public record SAPProfile(
    double velocityMeterPerSec,
    double accelerationMeterPerSec2,
    double jerkMeterPerSec3,
    Distance errorX,
    Distance errorY,
    Angle errorTheta,
    Distance beelineRadius,
    Distance autopilotRadius,
    Distance transitionPointShiftingX) {
  public APConstraints toAPConstraints() {
    return new APConstraints(velocityMeterPerSec, accelerationMeterPerSec2, jerkMeterPerSec3);
  }
}
