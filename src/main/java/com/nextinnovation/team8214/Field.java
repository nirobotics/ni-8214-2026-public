// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Filesystem;
import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Class to store all field constants. Length/Height -> meter, Angle -> degree */
public final class Field {
  public static final double LENGTH = 16.518;
  public static final double WIDTH = 8.043;

  public static final FieldType FIELD_TYPE = FieldType.ANDYMARK;

  public static final Pose2d LEFT_CLOSE_START =
      new Pose2d(3.551265373229981, 7.633405036926269, Rotation2d.kCW_90deg);

  public static final Pose2d LEFT_TRENCH_READY =
      new Pose2d(3.54126537322998, 7.453405036926269, Rotation2d.kZero);
  public static final Pose2d RIGHT_TRENCH_READY =
      new Pose2d(
          LEFT_TRENCH_READY.getX(),
          WIDTH - LEFT_TRENCH_READY.getY(),
          LEFT_TRENCH_READY.getRotation());

  public static final Translation2d HUB_CENTER =
      new Translation2d(4.62633752822876, Field.WIDTH / 2.0);

  public static final Translation2d HUB_EXPORT =
      new Translation2d(5.221786022186279, Field.WIDTH / 2.0);

  public static final double HUB_EXPORT_HEIGHT = 0.768382 + 0.075;

  public static final double TRENCH_DANGER_ZONE_Y = 6.482734680175781;

  public static final double TRENCH_AWAY_X_CLOSE = 4.549248218536377;
  public static final double TRENCH_AWAY_X_FAR = 11.838566780090332;

  public static final double TRENCH_BACK_X_CLOSE = 4.699671268463135;
  public static final double TRENCH_BACK_X_FAR = 11.990937232971191;

  private static final double TRENCH_BLUE_FACING_X = HUB_CENTER.getX() - Units.inchesToMeters(23.5);
  private static final double TRENCH_RED_FACING_X = HUB_CENTER.getX() + Units.inchesToMeters(23.5);
  private static final double TRENCH_OUTER_Y = Units.inchesToMeters(49.8125);
  private static final double TRENCH_INNER_Y = Units.inchesToMeters(61.8125);

  public static final Translation2d[] BLUE_SCORE_ROTATION_OBSTACLES = {
    new Translation2d(1.251, 4.6065),
    new Translation2d(1.251, 4.6065),
    new Translation2d(1.251, 3.4635),
    new Translation2d(1.251, 3.4635),
    new Translation2d(TRENCH_BLUE_FACING_X, TRENCH_INNER_Y),
    new Translation2d(TRENCH_RED_FACING_X, TRENCH_INNER_Y),
    new Translation2d(TRENCH_BLUE_FACING_X, TRENCH_OUTER_Y),
    new Translation2d(TRENCH_BLUE_FACING_X, TRENCH_INNER_Y),
    new Translation2d(TRENCH_BLUE_FACING_X, WIDTH - TRENCH_INNER_Y),
    new Translation2d(TRENCH_RED_FACING_X, WIDTH - TRENCH_INNER_Y),
    new Translation2d(TRENCH_BLUE_FACING_X, WIDTH - TRENCH_OUTER_Y),
    new Translation2d(TRENCH_BLUE_FACING_X, WIDTH - TRENCH_INNER_Y),
  };

  public static final Translation2d[] BLUE_SCORE_ROTATION_BUMPER_ONLY_OBSTACLES = {
    new Translation2d(HUB_CENTER.getX(), 0.0),
    new Translation2d(HUB_CENTER.getX(), TRENCH_INNER_Y),
    new Translation2d(HUB_CENTER.getX(), WIDTH),
    new Translation2d(HUB_CENTER.getX(), WIDTH - TRENCH_INNER_Y),
  };

  public static final Translation2d TRANSPORT_LEFT_PLACEMENT =
      new Translation2d(0.0, WIDTH * (6.0 / 8.0) - 0.75);

  public static final Translation2d TRANSPORT_RIGHT_PLACEMENT =
      new Translation2d(0.0, WIDTH * (2.0 / 8.0) + 0.75);

  public static final double TRANSPORT_LINE_X = 5.25;

  public static final Translation2d[] TRANSPORT_FORBIDDEN_ZONE_CLOSE = {
    new Translation2d(3.976987361907959, 5.109637260437012),
    new Translation2d(6.821966648101807, Field.WIDTH / 2.0),
    new Translation2d(3.976987361907959, 2.9562408924102783),
  };

  public static final Translation2d[] TRANSPORT_FORBIDDEN_ZONE_FAR = {
    new Translation2d(11.351537704467773, 5.109637260437012),
    new Translation2d(Field.LENGTH, 5.109637260437012),
    new Translation2d(Field.LENGTH, 2.933084011077881),
    new Translation2d(11.351537704467773, 2.933084011077881),
  };

  public static final Translation2d DEPOT_CENTER =
      new Translation2d(0.3, (WIDTH / 2) + Units.inchesToMeters(75.93));

  public static final Pose2d DEPOT_POUNCE_PRE =
      new Pose2d(0.6400334060192108, 7.38352746963501, Rotation2d.fromDegrees(-135.0));

  public static final Pose2d OUTPOST_RIGHT_COLLECT =
      new Pose2d(0.4969831943511963, 0.664755642414093, Rotation2d.k180deg);
  public static final Pose2d OUTPOST_LEFT_COLLECT =
      new Pose2d(
          OUTPOST_RIGHT_COLLECT.getX(),
          Field.WIDTH - OUTPOST_RIGHT_COLLECT.getY(),
          OUTPOST_RIGHT_COLLECT.getRotation());

  public static final Pose2d OUTPOST_RIGHT_SHOOT =
      new Pose2d(
          2.0022695064544678, 2.4835269451141357, Rotation2d.fromRadians(0.5200660884814355));
  public static final Pose2d OUTPOST_LEFT_SHOOT =
      new Pose2d(
          OUTPOST_RIGHT_SHOOT.getX(),
          Field.WIDTH - OUTPOST_RIGHT_SHOOT.getY(),
          OUTPOST_RIGHT_SHOOT.getRotation());

  @Getter
  @RequiredArgsConstructor
  public enum FieldType {
    ANDYMARK("andymark"),
    WELDED("welded");

    private final String jsonFolder;
  }

  public static final AprilTagLayoutType APRILTAG_LAYOUT = AprilTagLayoutType.HUB_ONLY;

  @Getter
  public enum AprilTagLayoutType {
    OFFICIAL("2026-official"),
    NO_TRENCH("2026-no-trench"),
    HUB_ONLY("2026-hub-only"),
    NONE("2026-none");

    AprilTagLayoutType(String name) {
      try {
        layout =
            new AprilTagFieldLayout(
                Path.of(
                    Filesystem.getDeployDirectory().getPath(),
                    "apriltags",
                    FIELD_TYPE.getJsonFolder(),
                    name + ".json"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      try {
        layoutString = new ObjectMapper().writeValueAsString(layout);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialize AprilTag layout JSON " + this);
      }
    }

    private final AprilTagFieldLayout layout;
    private final String layoutString;
  }
}
