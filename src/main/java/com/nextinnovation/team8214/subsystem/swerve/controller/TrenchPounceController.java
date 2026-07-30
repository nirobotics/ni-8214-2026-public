// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve.controller;

import com.nextinnovation.team8214.util.AllianceFlipUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import java.util.Optional;

public class TrenchPounceController extends DriveToPointController {
  public TrenchPounceController(Pose2d goalPose) {
    super(
        new DriveToPointControllerConfig(
            "/trenchPounceController",
            0.2,
            0.3,
            4.0,
            8.0,
            6.0,
            0.1,
            1.75,
            1.6,
            4.5,
            0.3,
            4.0,
            540.0),
        () -> Optional.of(goalPose));
  }

  @Override
  protected Optional<Rotation2d> getEntryAngle() {
    return Optional.of(AllianceFlipUtil.apply(Rotation2d.kZero));
  }
}
