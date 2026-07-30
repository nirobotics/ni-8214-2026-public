// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem;

import com.nextinnovation.team8214.util.TransformTree;
import com.nextinnovation.team8214.util.VirtualSubsystem;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.littletonrobotics.junction.Logger;

public class Visualizer extends VirtualSubsystem {
  public static final String BASE_FRAME = "robot";
  private final TransformTree transformTree = new TransformTree(BASE_FRAME);
  private final Map<Integer, String> indexToPath = new HashMap<>();
  private final Map<String, String> nameToPath = new HashMap<>();

  public Visualizer() {
    nameToPath.put(BASE_FRAME, BASE_FRAME);
    transformTree.setRootPose(new Pose3d(0.0, 0.0, 0.047, new Rotation3d()));
  }

  /**
   * Registers a node for visualization
   *
   * @param parentName Name of the parent node
   * @param childName Name of the child node
   * @param visualizationId Unique ID for the visualization
   * @param transformSupplier Supplier for the transform relative to its parent
   */
  public void registerVisualizedComponent(
      String parentName,
      String childName,
      int visualizationId,
      Supplier<Transform3d> transformSupplier) {
    var path = nameToPath.get(parentName) + "/" + childName;
    transformTree.registerComponent(path, transformSupplier);
    nameToPath.put(childName, path);
    if (visualizationId >= 0) {
      indexToPath.put(visualizationId, path);
    }
  }

  /**
   * Registers a node without visualization
   *
   * @param parentName Name of the parent node
   * @param childName Name of the child node
   * @param transformSupplier Supplier for the transform relative to its parent
   */
  public void registerUnvisualizedComponent(
      String parentName, String childName, Supplier<Transform3d> transformSupplier) {
    var path = nameToPath.get(parentName) + "/" + childName;
    transformTree.registerComponent(path, transformSupplier);
    nameToPath.put(childName, path);
  }

  /** Updates all visualized poses */
  @Override
  public void periodic() {
    transformTree.update();

    var maxId = indexToPath.keySet().stream().max(Integer::compare).orElse(-1);
    var poses = new Pose3d[maxId + 1];

    for (int id = 0; id < maxId + 1; id++) {
      if (indexToPath.get(id) == null) {
        poses[id] = new Pose3d(1e9, 1e9, 1e9, new Rotation3d());
      } else {
        poses[id] = transformTree.getNodePose(indexToPath.get(id));
      }
    }

    Logger.recordOutput("subsystem/visualizer/components", poses);
  }

  public void print() {
    transformTree.print();
  }
}
