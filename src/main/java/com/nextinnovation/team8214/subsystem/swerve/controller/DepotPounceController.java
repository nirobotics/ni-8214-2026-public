// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve.controller;

import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import java.util.Optional;

public class DepotPounceController extends DriveToPointController {
  public DepotPounceController() {
    super(
        new DriveToPointController.DriveToPointControllerConfig(
            "/depotPounceController",
            0.05,
            0.05,
            4.0,
            8.0,
            4.0,
            0.1,
            3.0,
            2.5,
            4.5,
            0.3,
            4.0,
            540.0),
        () -> Optional.of(AllianceFlipUtil.apply(Field.DEPOT_POUNCE_PRE)));
  }

  @Override
  protected Optional<Rotation2d> getEntryAngle() {
    return Optional.of(AllianceFlipUtil.apply(Rotation2d.k180deg));
  }
}
