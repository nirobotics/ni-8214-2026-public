// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import org.littletonrobotics.junction.Logger;

public final class LoggerUtil {
  private static final double MICROSECONDS_PER_SECOND = 1.0e6;

  private LoggerUtil() {}

  public static double getTimestampSec() {
    return Logger.getTimestamp() / MICROSECONDS_PER_SECOND;
  }
}
