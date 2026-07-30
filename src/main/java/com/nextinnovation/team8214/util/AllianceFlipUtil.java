// Copyright (c) 2025 FRC 6328
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import com.nextinnovation.team8214.Field;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.wpilibj.DriverStation;

public class AllianceFlipUtil {

  public static double applyX(double x) {
    return shouldFlip() ? Field.LENGTH - x : x;
  }

  public static double forceApplyX(double x) {
    return Field.LENGTH - x;
  }

  public static double applyY(double y) {
    return shouldFlip() ? Field.WIDTH - y : y;
  }

  public static double forceApplyY(double y) {
    return Field.WIDTH - y;
  }

  public static Translation2d apply(Translation2d translation) {
    return new Translation2d(applyX(translation.getX()), applyY(translation.getY()));
  }

  public static Translation2d[] apply(Translation2d[] translations) {
    if (translations == null || translations.length == 0) {
      return new Translation2d[0];
    }

    var flippedTranslations = new Translation2d[translations.length];
    for (int i = 0; i < translations.length; i++) {
      flippedTranslations[i] = apply(translations[i]);
    }

    return flippedTranslations;
  }

  public static Translation2d forceApply(Translation2d translation) {
    return new Translation2d(forceApplyX(translation.getX()), forceApplyY(translation.getY()));
  }

  public static Rotation2d apply(Rotation2d rotation) {
    return shouldFlip() ? rotation.rotateBy(Rotation2d.kPi) : rotation;
  }

  public static Rotation2d forceApply(Rotation2d rotation) {
    return rotation.rotateBy(Rotation2d.kPi);
  }

  public static Pose2d apply(Pose2d pose) {
    return shouldFlip()
        ? new Pose2d(apply(pose.getTranslation()), apply(pose.getRotation()))
        : pose;
  }

  public static Pose2d forceApply(Pose2d pose) {
    return new Pose2d(forceApply(pose.getTranslation()), forceApply(pose.getRotation()));
  }

  public static Pose3d apply(Pose3d pose) {
    var translation2d = apply(pose.getTranslation().toTranslation2d());

    return shouldFlip()
        ? new Pose3d(
            new Translation3d(translation2d.getX(), translation2d.getY(), pose.getZ()),
            new Rotation3d(
                pose.getRotation().getX(),
                pose.getRotation().getY(),
                apply(pose.getRotation().toRotation2d()).getRadians()))
        : pose;
  }

  public static Pose3d forceApply(Pose3d pose) {
    var translation2d = forceApply(pose.getTranslation().toTranslation2d());

    return new Pose3d(
        new Translation3d(translation2d.getX(), translation2d.getY(), pose.getZ()),
        new Rotation3d(
            pose.getRotation().getX(),
            pose.getRotation().getY(),
            forceApply(pose.getRotation().toRotation2d()).getRadians()));
  }

  public static boolean shouldFlip() {
    return DriverStation.getAlliance().isPresent()
        && DriverStation.getAlliance().get() == DriverStation.Alliance.Red;
  }
}
