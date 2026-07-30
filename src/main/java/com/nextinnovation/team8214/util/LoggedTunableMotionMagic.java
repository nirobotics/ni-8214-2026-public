// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.math.util.Units;

public class LoggedTunableMotionMagic {
  public static class Extend {
    private final LoggedTunableNumber maxVelocityMeterPerSec;
    private final LoggedTunableNumber maxAccelMeterPerSec2;

    public Extend(String logRoot, double maxVelocityMeterPerSec, double maxAccelMeterPerSec2) {
      this("Default", logRoot, maxVelocityMeterPerSec, maxAccelMeterPerSec2);
    }

    public Extend(
        String logGroup,
        String logRoot,
        double maxVelocityMeterPerSec,
        double maxAccelMeterPerSec2) {
      var cleanLogGroup = logGroup.replaceFirst("/$", "");
      var cleanLogRoot = logRoot.replaceFirst("/$", "") + "/motionMagic";

      this.maxVelocityMeterPerSec =
          new LoggedTunableNumber(
              cleanLogGroup, cleanLogRoot + "/maxVelocityMeterPerSec", maxVelocityMeterPerSec);
      this.maxAccelMeterPerSec2 =
          new LoggedTunableNumber(
              cleanLogGroup, cleanLogRoot + "/maxAccelMeterPerSes2", maxAccelMeterPerSec2);
    }

    public double getMaxVelocityMeterPerSec() {
      return maxVelocityMeterPerSec.getAsDouble();
    }

    public double getMaxAccelMeterPerSec2() {
      return maxAccelMeterPerSec2.getAsDouble();
    }
  }

  public static class Spin {
    private final LoggedTunableNumber maxVelocityDegreePerSec;
    private final LoggedTunableNumber maxAccelDegreePerSec2;

    public Spin(String logRoot, double maxVelocityDegreePerSec, double maxAccelDegreePerSec2) {
      this("Default", logRoot, maxVelocityDegreePerSec, maxAccelDegreePerSec2);
    }

    public Spin(
        String logGroup,
        String logRoot,
        double maxVelocityDegreePerSec,
        double maxAccelDegreePerSec2) {
      var cleanLogGroup = logGroup.replaceFirst("/$", "");
      var cleanLogRoot = logRoot.replaceFirst("/$", "") + "/motionMagic";

      this.maxVelocityDegreePerSec =
          new LoggedTunableNumber(
              cleanLogGroup, cleanLogRoot + "/maxVelocityDegreePerSec", maxVelocityDegreePerSec);
      this.maxAccelDegreePerSec2 =
          new LoggedTunableNumber(
              cleanLogGroup, cleanLogRoot + "/maxAccelDegreePerSec2", maxAccelDegreePerSec2);
    }

    public double getMaxVelocityRadPerSec() {
      return Units.degreesToRadians(maxVelocityDegreePerSec.getAsDouble());
    }

    public double getMaxVelocityRotationPerSec() {
      return Units.degreesToRotations(maxVelocityDegreePerSec.getAsDouble());
    }

    public double getMaxAccelRadPerSec2() {
      return Units.degreesToRadians(maxAccelDegreePerSec2.getAsDouble());
    }

    public double getMaxAccelRotationPerSec2() {
      return Units.degreesToRotations(maxAccelDegreePerSec2.getAsDouble());
    }
  }
}
