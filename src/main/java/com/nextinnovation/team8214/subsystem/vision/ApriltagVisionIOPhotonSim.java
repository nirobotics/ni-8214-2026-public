// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.vision;

import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.Odometry;
import edu.wpi.first.math.geometry.Transform3d;
import java.util.Set;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionSystemSim;

public class ApriltagVisionIOPhotonSim extends ApriltagVisionIOPhoton {
  private static final Set<Integer> BOTTOM_LEFT_IDS = Set.of(29, 30, 31, 32, 25, 26, 18, 27, 28);
  private static final Set<Integer> TOP_LEFT_IDS = Set.of(29, 30, 31, 32, 25, 26, 21, 24, 23);
  private static final Set<Integer> BOTTOM_MID_IDS = Set.of(6, 17, 5, 8, 18, 27, 4, 3, 19, 20);
  private static final Set<Integer> TOP_MID_IDS = Set.of(11, 2, 21, 24, 1, 22, 4, 3, 19, 20);
  private static final Set<Integer> BOTTOM_RIGHT_IDS = Set.of(7, 8, 5, 9, 10, 16, 15, 13, 14);
  private static final Set<Integer> TOP_RIGHT_IDS = Set.of(12, 11, 2, 9, 10, 16, 15, 13, 14);

  ApriltagVisionIOPhotonSim(
      String cameraName,
      SimCameraProperties simCameraProperties,
      Transform3d robot2Camera,
      VisionSystemSim visionSystemSim) {
    super(cameraName);

    var sim =
        new PhotonCameraSim(super.camera, simCameraProperties, Field.APRILTAG_LAYOUT.getLayout());
    sim.enableProcessedStream(true);

    visionSystemSim.addCamera(sim, robot2Camera);
  }

  @Override
  protected boolean isTagAllowed(int id) {
    var position = Odometry.getInstance().getSimPose().getTranslation();
    return allowedIdsForPosition(position.getX(), position.getY()).contains(id);
  }

  static Set<Integer> allowedIdsForPosition(double x, double y) {
    if (y < 4.035) {
      if (x < 4.630) {
        return BOTTOM_LEFT_IDS;
      }
      if (x < 11.910) {
        return BOTTOM_MID_IDS;
      }
      return BOTTOM_RIGHT_IDS;
    }

    if (x < 4.630) {
      return TOP_LEFT_IDS;
    }
    if (x < 11.910) {
      return TOP_MID_IDS;
    }
    return TOP_RIGHT_IDS;
  }
}
