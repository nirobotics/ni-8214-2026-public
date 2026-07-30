// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.led;

public record TimedColor(Color color, double intervalSec) {
  @Override
  public String toString() {
    return "(" + color.r() + "," + color.g() + "," + color.b() + "," + intervalSec + ")";
  }
}
