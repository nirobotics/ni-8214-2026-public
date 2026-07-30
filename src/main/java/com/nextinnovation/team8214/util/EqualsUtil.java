// Copyright (c) 2024 FRC 6328
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Twist2d;

public class EqualsUtil {
  public static boolean epsilonEquals(double a, double b, double epsilon) {
    return (a - epsilon <= b) && (a + epsilon >= b);
  }

  public static boolean epsilonEquals(double a, double b) {
    return epsilonEquals(a, b, 1e-9);
  }

  /** Extension methods for wpi geometry objects */
  public static class GeomExtensions {
    public static boolean epsilonEquals(Twist2d twist, Twist2d other) {
      return EqualsUtil.epsilonEquals(twist.dx, other.dx)
          && EqualsUtil.epsilonEquals(twist.dy, other.dy)
          && EqualsUtil.epsilonEquals(twist.dtheta, other.dtheta);
    }

    /**
     * Checks if two Rotation2d angles radians are equal within epsilon tolerance
     *
     * @param rotation1 The first angle
     * @param rotation2 The second angle
     * @param epsilon The allowed error tolerance (in radians)
     * @param wrapped Whether to consider angle periodicity (true for wrapped, false for unwrapped)
     * @return true if the two angles are equal within the error tolerance
     */
    public static boolean epsilonEquals(
        Rotation2d rotation1, Rotation2d rotation2, double epsilon, boolean wrapped) {
      double angle1 = rotation1.getRadians();
      double angle2 = rotation2.getRadians();

      if (wrapped) {
        // Wrapped case: consider angle periodicity, calculate minimum angle difference
        double angleDiff = Math.abs(angle1 - angle2);
        // Normalize to [0, 2π] range
        angleDiff = angleDiff % (2 * Math.PI);
        // Take minimum angle difference (considering cases crossing 2π)
        angleDiff = Math.min(angleDiff, 2 * Math.PI - angleDiff);
        return angleDiff <= epsilon;
      } else {
        // Unwrapped case: directly compare angle values
        return EqualsUtil.epsilonEquals(angle1, angle2, epsilon);
      }
    }

    /**
     * Checks if two Rotation2d angles are equal within default epsilon tolerance
     *
     * @param rotation1 The first angle
     * @param rotation2 The second angle
     * @param wrapped Whether to consider angle periodicity (true for wrapped, false for unwrapped)
     * @return true if the two angles are equal within the default error tolerance
     */
    public static boolean epsilonEquals(
        Rotation2d rotation1, Rotation2d rotation2, boolean wrapped) {
      return epsilonEquals(rotation1, rotation2, 1e-9, wrapped);
    }
  }
}
