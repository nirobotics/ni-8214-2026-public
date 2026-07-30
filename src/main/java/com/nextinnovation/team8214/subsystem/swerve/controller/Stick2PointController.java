// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve.controller;

import com.nextinnovation.team8214.Odometry;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import java.util.Optional;

public class Stick2PointController extends DriveToPointController {
  public Stick2PointController(Pose2d goalPose) {
    super(
        new DriveToPointController.DriveToPointControllerConfig(
            "/stick2PointController",
            0.05,
            0.05,
            4.0,
            8.0,
            4.0,
            0.1,
            4.0,
            0.0,
            4.5,
            0.3,
            4.0,
            540.0),
        () -> Optional.of(goalPose));
  }

  @Override
  protected Optional<Rotation2d> getEntryAngle() {
    var entryAngle =
        super.getGoalPose()
            .get()
            .getTranslation()
            .minus(Odometry.getInstance().getEstimatedPose().getTranslation())
            .getAngle();

    return Optional.of(entryAngle);
  }
}
