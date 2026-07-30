// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.led;

public record Color(int r, int g, int b) {
  public static final Color RED = new Color(255, 0, 0);
  public static final Color BLUE = new Color(0, 0, 255);
  public static final Color MINT = new Color(152, 255, 152);
  public static final Color GREEN = new Color(0, 255, 0);
  public static final Color YELLOW = new Color(255, 215, 0);
  public static final Color ORANGE = new Color(255, 155, 0);
  public static final Color PURPLE = new Color(255, 0, 255);
  public static final Color WHITE = new Color(255, 255, 255);
  public static final Color PINK = new Color(255, 105, 180);
  public static final Color OFF = new Color(0, 0, 0);

  @Override
  public String toString() {
    return "(" + r + "," + g + "," + b + ")";
  }
}
