// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import com.nextinnovation.cyberpower.LogSink;
import org.littletonrobotics.junction.Logger;

/** Records Cyber Power output through the active AdvantageKit logger. */
public final class AdvantageKitLogSink implements LogSink {
  @Override
  public void recordDouble(String path, double value, String unit, long ignoredTimestampMicros) {
    Logger.recordOutput(path, value, unit);
  }

  @Override
  public void recordLong(String path, long value, long ignoredTimestampMicros) {
    Logger.recordOutput(path, value);
  }

  @Override
  public void recordString(String path, String value, long ignoredTimestampMicros) {
    Logger.recordOutput(path, value);
  }

  @Override
  public void recordDoubleArray(
      String path, double[] values, String unit, long ignoredTimestampMicros) {
    Logger.recordOutput(path, values);
  }
}
