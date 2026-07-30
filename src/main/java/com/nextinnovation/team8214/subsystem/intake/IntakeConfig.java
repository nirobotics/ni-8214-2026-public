// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.intake;

import com.nextinnovation.team8214.Config;

class IntakeConfig {
  static final String LOG_GROUP = Config.LiveDebugGroup.INTAKE.toString();
  static final String LOG_ROOT = "subsystem/intake";

  static final IntakeGoal INIT_GOAL = IntakeGoal.START;
  static final double PIVOT_GEAR_RATIO = (46.0 / 14.0) * (64.0 / 16.0) * (28.0 / 9.0);
}
