// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.vision;

import com.nextinnovation.team8214.Config;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.util.Units;

class ApriltagVisionConfig {
  static final String LOG_GROUP = Config.LiveDebugGroup.ODOMETRY.toString();
  static final String LOG_ROOT = "subsystem/apriltagVision";

  static final Pose3d UP_LEFT =
      new Pose3d(
          -0.3234,
          0.15004,
          0.40625,
          new Rotation3d(0.0, Units.degreesToRadians(-30.0), Units.degreesToRadians(-165.0)));
  static final Pose3d DOWN_LEFT =
      new Pose3d(
          -0.3065,
          0.34652,
          0.29808,
          new Rotation3d(0.0, Units.degreesToRadians(-25.0), Units.degreesToRadians(58.0)));
  static final Pose3d DOWN_RIGHT =
      new Pose3d(
          -0.3065,
          -0.34652,
          0.29808,
          new Rotation3d(0.0, Units.degreesToRadians(-25.0), Units.degreesToRadians(-58.0)));
  static final Pose3d UP_RIGHT =
      new Pose3d(
          -0.3234,
          -0.15004,
          0.40625,
          new Rotation3d(0.0, Units.degreesToRadians(-30.0), Units.degreesToRadians(165.0)));
  static final double FIELD_BORDER_THRESHOLD_METER = 0.1;
  static final double ROBOT_POSE_Z_THRESHOLD_METER = 0.1;
}
