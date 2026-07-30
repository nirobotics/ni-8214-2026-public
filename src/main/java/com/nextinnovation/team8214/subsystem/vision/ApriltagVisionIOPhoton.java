// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.vision;

import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.util.GeomUtil;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.HashSet;
import lombok.experimental.ExtensionMethod;
import org.photonvision.PhotonCamera;

@ExtensionMethod({GeomUtil.class})
class ApriltagVisionIOPhoton implements ApriltagVisionIO {
  protected final PhotonCamera camera;
  private final AprilTagFieldLayout apriltagLayout = Field.APRILTAG_LAYOUT.getLayout();

  ApriltagVisionIOPhoton(String cameraName) {
    PhotonCamera.setVersionCheckEnabled(false);

    camera = new PhotonCamera(cameraName);
  }

  protected boolean isTagAllowed(int id) {
    return true;
  }

  @Override
  public void updateInputs(ApriltagVisionIOInputs inputs) {
    inputs.connected = camera.isConnected();

    inputs.hasUpdate = false;

    if (!inputs.connected) {
      inputs.hasTargets = false;
      inputs.poseObservations = new PoseObservation[0];
      inputs.ids = new int[0];
      inputs.txTyObservations = new TxTyObservation[0];
      return;
    }

    final var results = camera.getAllUnreadResults();
    if (results.isEmpty()) {
      return;
    }

    inputs.hasUpdate = true;
    inputs.hasTargets = false;

    final var ids = new HashSet<Short>();
    final var poseObservations = new ArrayList<PoseObservation>(results.size());
    final var txTyObservations = new ArrayList<TxTyObservation>(results.size() * 2);

    resultsLoop:
    for (final var result : results) {
      if (!result.hasTargets()) {
        continue;
      }

      if (result.multitagResult.isPresent()) {
        final var multiTagResult = result.multitagResult.get();
        if (multiTagResult.fiducialIDsUsed.isEmpty()) {
          continue;
        }

        var sumDistance = 0.0;
        final var cameraInField = multiTagResult.estimatedPose.best.toPose3d();
        for (final var id : multiTagResult.fiducialIDsUsed) {
          if (!isTagAllowed(id)) {
            continue resultsLoop;
          }
          final var tagPose = apriltagLayout.getTagPose((int) id);
          if (tagPose.isEmpty()) {
            continue resultsLoop;
          }
          sumDistance += cameraInField.getTranslation().getDistance(tagPose.get().getTranslation());
        }

        final var tagCount = multiTagResult.fiducialIDsUsed.size();
        poseObservations.add(
            new PoseObservation(
                result.getTimestampSeconds(),
                cameraInField,
                multiTagResult.estimatedPose.ambiguity,
                tagCount,
                sumDistance / tagCount));
        ids.addAll(multiTagResult.fiducialIDsUsed);
      } else {
        final var singleTagTarget = result.getBestTarget();

        final var apriltagIdUsed = singleTagTarget.getFiducialId();
        final var tagInField = apriltagLayout.getTagPose(apriltagIdUsed);
        final var tagInCamera = singleTagTarget.getBestCameraToTarget();
        final var id = singleTagTarget.getFiducialId();

        if (isTagAllowed(id) && tagInField.isPresent()) {
          final var cameraInField = tagInField.get().plus(tagInCamera.inverse());
          poseObservations.add(
              new PoseObservation(
                  result.getTimestampSeconds(),
                  cameraInField,
                  singleTagTarget.getPoseAmbiguity(),
                  1,
                  cameraInField.getTranslation().getDistance(tagInField.get().getTranslation())));
          ids.add((short) id);
        }
      }

      for (final var target : result.getTargets()) {
        if (!isTagAllowed(target.getFiducialId())
            || apriltagLayout.getTagPose(target.getFiducialId()).isEmpty()) {
          continue;
        }

        txTyObservations.add(
            new TxTyObservation(
                result.getTimestampSeconds(),
                target.getFiducialId(),
                Units.degreesToRadians(target.getYaw()),
                Units.degreesToRadians(target.getPitch()),
                target.getBestCameraToTarget().getTranslation().getNorm()));
      }
    }

    inputs.hasTargets = !poseObservations.isEmpty() || !txTyObservations.isEmpty();
    inputs.poseObservations = poseObservations.toArray(new PoseObservation[0]);

    inputs.ids = new int[ids.size()];
    var i = 0;
    for (var id : ids) {
      inputs.ids[i++] = id;
    }

    inputs.txTyObservations = txTyObservations.toArray(new TxTyObservation[0]);
  }
}
