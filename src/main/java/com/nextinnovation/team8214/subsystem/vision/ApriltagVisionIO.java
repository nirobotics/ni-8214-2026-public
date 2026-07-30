// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.vision;

import edu.wpi.first.math.geometry.Pose3d;
import org.littletonrobotics.junction.AutoLog;

interface ApriltagVisionIO {
  @AutoLog
  class ApriltagVisionIOInputs {
    boolean connected;
    boolean hasUpdate;
    boolean hasTargets;

    PoseObservation[] poseObservations;
    int[] ids;

    TxTyObservation[] txTyObservations;
  }

  record PoseObservation(
      double timestamp, Pose3d cameraInField, double ambiguity, int tagCount, double avgDistance) {}

  record TxTyObservation(double timestamp, int id, double txRad, double tyRad, double distance) {}

  default void updateInputs(ApriltagVisionIOInputs inputs) {
    inputs.connected = false;
    inputs.poseObservations = new PoseObservation[0];
    inputs.ids = new int[0];
    inputs.txTyObservations = new TxTyObservation[0];
  }
}
