// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.shooter;

import com.nextinnovation.team8214.Config;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.util.Units;

public class ShooterConfig {
  public static final String LOG_GROUP = Config.LiveDebugGroup.SHOOTER.toString();
  public static final String LOG_ROOT = "subsystem/shooter";

  public static final Pose3d SHOOTER_IN_ROBOT_POSITION =
      new Pose3d(-0.191535, -0.0, 0.4462975, new Rotation3d());
  public static final double FLYWHEEL_RADIUS_METER = Units.inchesToMeters(2.0);

  public static final boolean ENABLE_AUTO_DODGE_TRENCH = true;

  public static final double START_ANGLE_DEGREE = 80.0;

  static final double FLYWHEEL_GEAR_RATIO = 36.0 / 22.0;
  static final double PITCH_FEEDBACK_SENSOR_TO_MECHANISM_RATIO = (60.0 / 12.0) * (148.0 / 10.0);
  static final double PITCH_ANALYSIS_REDUCTION = 60.0 / 12.0;

  static final double DEFAULT_MANUAL_SCORE_DISTANCE_OFFSET = 0.0;
}
