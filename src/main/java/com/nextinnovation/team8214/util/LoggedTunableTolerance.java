// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.math.util.Units;

public class LoggedTunableTolerance {
  public static class Extend {
    private final LoggedTunableNumber stopVelocityToleranceMeterPerSec;
    private final LoggedTunableNumber atPositionToleranceMeter;

    public Extend(
        String logRoot, double stopVelocityToleranceMeterPerSec, double atPositionToleranceMeter) {
      this("Default", logRoot, stopVelocityToleranceMeterPerSec, atPositionToleranceMeter);
    }

    public Extend(
        String logGroup,
        String logRoot,
        double stopVelocityToleranceMeterPerSec,
        double atPositionToleranceMeter) {
      var cleanLogGroup = logGroup.replaceFirst("/$", "");
      var cleanLogRoot = logRoot.replaceFirst("/$", "") + "/tolerance";

      this.stopVelocityToleranceMeterPerSec =
          new LoggedTunableNumber(
              cleanLogGroup,
              cleanLogRoot + "/stopVelocityToleranceMeterPerSec",
              stopVelocityToleranceMeterPerSec);
      this.atPositionToleranceMeter =
          new LoggedTunableNumber(
              cleanLogGroup, cleanLogRoot + "/atPositionToleranceMeter", atPositionToleranceMeter);
    }

    public double getStopVelocityToleranceMeterPerSec() {
      return stopVelocityToleranceMeterPerSec.getAsDouble();
    }

    public double getAtPositionToleranceMeter() {
      return atPositionToleranceMeter.getAsDouble();
    }

    public boolean hasStop(double velocityMeterPerSec) {
      return EqualsUtil.epsilonEquals(
          0.0, velocityMeterPerSec, getStopVelocityToleranceMeterPerSec());
    }

    public boolean atPosition(double currentPositionMeter, double goalPositionMeter) {
      return EqualsUtil.epsilonEquals(
          goalPositionMeter, currentPositionMeter, getAtPositionToleranceMeter());
    }
  }

  public static class Spin {
    private final LoggedTunableNumber stopVelocityToleranceDegreePerSec;
    private final LoggedTunableNumber atPositionToleranceDegree;

    public Spin(
        String logRoot,
        double stopVelocityToleranceDegreePerSec,
        double atPositionToleranceDegree) {
      this("Default", logRoot, stopVelocityToleranceDegreePerSec, atPositionToleranceDegree);
    }

    public Spin(
        String logGroup,
        String logRoot,
        double stopVelocityToleranceDegreePerSec,
        double atPositionToleranceDegree) {
      var cleanLogGroup = logGroup.replaceFirst("/$", "");
      var cleanLogRoot = logRoot.replaceFirst("/$", "") + "/tolerance";

      this.stopVelocityToleranceDegreePerSec =
          new LoggedTunableNumber(
              cleanLogGroup,
              cleanLogRoot + "/stopVelocityToleranceDegreePerSec",
              stopVelocityToleranceDegreePerSec);
      this.atPositionToleranceDegree =
          new LoggedTunableNumber(
              cleanLogGroup,
              cleanLogRoot + "/atPositionToleranceDegree",
              atPositionToleranceDegree);
    }

    public double getStopVelocityToleranceRadPerSec() {
      return Units.degreesToRadians(stopVelocityToleranceDegreePerSec.getAsDouble());
    }

    public double getStopVelocityToleranceRotationPerSec() {
      return Units.degreesToRotations(stopVelocityToleranceDegreePerSec.getAsDouble());
    }

    public double getAtGoalPositionToleranceRad() {
      return Units.degreesToRadians(atPositionToleranceDegree.getAsDouble());
    }

    public double getAtGoalPositionToleranceRotation() {
      return Units.degreesToRotations(atPositionToleranceDegree.getAsDouble());
    }

    public boolean hasStop(double velocityRadSec) {
      return EqualsUtil.epsilonEquals(0.0, velocityRadSec, getStopVelocityToleranceRadPerSec());
    }

    public boolean atPosition(double currentPositionRad, double goalPositionRad) {
      return EqualsUtil.epsilonEquals(
          goalPositionRad, currentPositionRad, getAtGoalPositionToleranceRad());
    }
  }
}
