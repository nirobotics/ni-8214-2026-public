// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.command;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.nextinnovation.team8214.*;
import com.nextinnovation.team8214.subsystem.hopper.Hopper;
import com.nextinnovation.team8214.subsystem.indexer.Indexer;
import com.nextinnovation.team8214.subsystem.intake.Intake;
import com.nextinnovation.team8214.subsystem.shooter.Shooter;
import com.nextinnovation.team8214.subsystem.swerve.Swerve;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import com.nextinnovation.team8214.util.AllianceValue;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.List;

public class AutoModes {
  private static final double TRENCH_READY_Y_THRESHOLD_METER = 7.2;
  private static final double FIRST_SWEEP_SHORT_PRESET_MAX_ACCEL_METER_PER_SEC2 = 2.0;
  private static final double FIRST_SWEEP_LONG_PRESET_MAX_ACCEL_METER_PER_SEC2 = 2.0;
  private static final double FIRST_SWEEP_SCRUM_PRESET_MAX_ACCEL_METER_PER_SEC2 = 2.0;
  private static final double SECOND_SWEEP_LONG_PRESET_MAX_ACCEL_METER_PER_SEC2 = 1.72;
  private static final double SECOND_SWEEP_SHORT_PRESET_MAX_ACCEL_METER_PER_SEC2 = 1.74;
  private static final double DEPOT_REACCEL_PRESET_MAX_ACCEL_METER_PER_SEC2 = 0.56;
  private static final double CLOSE_DEPOT_PRESET_MAX_ACCEL_METER_PER_SEC2 = 2.00;
  private static final double LEFT_CLOSE_SWEEP_PRESET_MAX_ACCEL_METER_PER_SEC2 = 1.18;
  private static final double RIGHT_CLOSE_SWEEP_PRESET_MAX_ACCEL_METER_PER_SEC2 = 1.32;
  private static final double RIGHT_CLOSE_FINAL_PRESET_MAX_ACCEL_METER_PER_SEC2 = 0.54;

  private final AutoCommands autoCommands;

  public AutoModes(Swerve swerve, Intake intake, Indexer indexer, Hopper hopper, Shooter shooter) {
    autoCommands = new AutoCommands(swerve, intake, indexer, hopper, shooter);
  }

  // *************************** 2 Sweep Bump ***************************
  private Command getTrenchStart2NeutralZoneWander2BumpShoot(boolean wantLeft, boolean wantShort) {
    var trajectory =
        wantLeft
            ? wantShort
                ? TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .leftTrenchStart2NeutralZoneWanderShort2BumpShoot
                : TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .leftTrenchStart2NeutralZoneWander2BumpShoot
            : wantShort
                ? TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .rightTrenchStart2NeutralZoneWanderShort2BumpShoot
                : TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .rightTrenchStart2NeutralZoneWander2BumpShoot;
    var presetMaxAccelMeterPerSec2 =
        wantShort
            ? FIRST_SWEEP_SHORT_PRESET_MAX_ACCEL_METER_PER_SEC2
            : FIRST_SWEEP_LONG_PRESET_MAX_ACCEL_METER_PER_SEC2;

    return Commands.sequence(
        autoCommands
            .waitUntilAllianceConfirmed()
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterPreset(trajectory, 1, presetMaxAccelMeterPerSec2)),
        autoCommands
            .resetPoseByTrajectory(trajectory)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterUnderTrench(true)),
        autoCommands.followTrajectory(trajectory, 0),
        autoCommands.maybeStickOnTrajectoryEnd(trajectory, 0),
        autoCommands
            .crossBump(trajectory, 1)
            .alongWith(
                autoCommands.setShooterUnderTrench(false), autoCommands.setIntakeAndIndexerIdle()));
  }

  private Command getTrenchStart2NeutralZoneScrum2BumpShoot(boolean wantLeft) {
    var trajectory =
        wantLeft
            ? TrajectoryLoader.getInstance()
                .getTrajectorySet()
                .leftTrenchStart2NeutralZoneScrum2BumpShoot
            : TrajectoryLoader.getInstance()
                .getTrajectorySet()
                .rightTrenchStart2NeutralZoneScrum2BumpShoot;

    return Commands.sequence(
        autoCommands
            .waitUntilAllianceConfirmed()
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterPreset(
                    trajectory, 1, FIRST_SWEEP_SCRUM_PRESET_MAX_ACCEL_METER_PER_SEC2)),
        autoCommands
            .resetPoseByTrajectory(trajectory)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterUnderTrench(true)),
        autoCommands.followTrajectory(trajectory, 0),
        autoCommands.maybeStickOnTrajectoryEnd(trajectory, 0),
        autoCommands
            .crossBump(trajectory, 1)
            .alongWith(
                autoCommands.setShooterUnderTrench(false), autoCommands.setIntakeAndIndexerIdle()));
  }

  private Command getTrenchReady2NeutralZoneDrunk2BumpShoot(boolean wantLeft, boolean wantShort) {
    var trajectory =
        wantLeft
            ? wantShort
                ? TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .leftTrenchReady2NeutralZoneDrunkShort2BumpShoot
                : TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .leftTrenchReady2NeutralZoneDrunk2BumpShoot
            : wantShort
                ? TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .rightTrenchReady2NeutralZoneDrunkShort2BumpShoot
                : TrajectoryLoader.getInstance()
                    .getTrajectorySet()
                    .rightTrenchReady2NeutralZoneDrunk2BumpShoot;
    var firstSplitIndex = trajectory.get().splits().size() - 2;
    var secondSplitIndex = firstSplitIndex + 1;
    var presetMaxAccelMeterPerSec2 =
        wantShort
            ? SECOND_SWEEP_SHORT_PRESET_MAX_ACCEL_METER_PER_SEC2
            : SECOND_SWEEP_LONG_PRESET_MAX_ACCEL_METER_PER_SEC2;

    return Commands.sequence(
        autoCommands
            .followTrajectory(
                trajectory, firstSplitIndex, secondSplitIndex, presetMaxAccelMeterPerSec2)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterUnderTrench(true)),
        autoCommands.maybeStickOnTrajectoryEnd(trajectory, firstSplitIndex),
        autoCommands
            .crossBump(trajectory, secondSplitIndex)
            .alongWith(
                autoCommands.setShooterUnderTrench(false), autoCommands.setIntakeAndIndexerIdle()));
  }

  private Command getTrenchReady2NeutralZoneDash(boolean wantLeft) {
    var trajectory =
        wantLeft
            ? TrajectoryLoader.getInstance().getTrajectorySet().leftTrenchReady2NeutralZoneDash
            : TrajectoryLoader.getInstance().getTrajectorySet().rightTrenchReady2NeutralZoneDash;
    var dashSplitIndex = trajectory.get().splits().size() - 1;

    return Commands.sequence(
        autoCommands.setSwerveDriveNeutralMode(false),
        autoCommands
            .followTrajectory(trajectory, dashSplitIndex)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterUnderTrench(true)));
  }

  private static AllianceValue<Trajectory<SwerveSample>> getFirstSweepTrajectory(
      boolean wantLeft, List<String> responses) {
    var trajectories = TrajectoryLoader.getInstance().getTrajectorySet();
    return switch (responses.get(0)) {
      case "scrum" ->
          wantLeft
              ? trajectories.leftTrenchStart2NeutralZoneScrum2BumpShoot
              : trajectories.rightTrenchStart2NeutralZoneScrum2BumpShoot;
      case "long" ->
          wantLeft
              ? trajectories.leftTrenchStart2NeutralZoneWander2BumpShoot
              : trajectories.rightTrenchStart2NeutralZoneWander2BumpShoot;
      default ->
          wantLeft
              ? trajectories.leftTrenchStart2NeutralZoneWanderShort2BumpShoot
              : trajectories.rightTrenchStart2NeutralZoneWanderShort2BumpShoot;
    };
  }

  private static Pose2d getInitialPose(AllianceValue<Trajectory<SwerveSample>> trajectory) {
    return AllianceFlipUtil.apply(
        trajectory.get().getInitialPose(AllianceFlipUtil.shouldFlip()).get());
  }

  public Command getLeft2DoubleSweepBump(List<String> responses) {
    return getStart2DoubleSweepBump(true, responses);
  }

  public Command getRight2DoubleSweepBump(List<String> responses) {
    return getStart2DoubleSweepBump(false, responses);
  }

  private void addStart2DoubleSweepBump(AutoModeSelector autoModeSelector, boolean wantLeft) {
    var namePrefix = wantLeft ? "1LB.left" : "1RB.right";

    autoModeSelector.addMode(
        namePrefix + "2DoubleSweepBump",
        List.of(
            new AutoModeSelector.AutoQuestion("1st sweep?", List.of("short", "long", "scrum")),
            new AutoModeSelector.AutoQuestion("2nd sweep?", List.of("long", "short"))),
        responses -> getInitialPose(getFirstSweepTrajectory(wantLeft, responses)),
        responses -> getStart2DoubleSweepBump(wantLeft, responses));
  }

  private Command getStart2DoubleSweepBump(boolean wantLeft, List<String> responses) {
    var trenchReadyPouncePose = wantLeft ? Field.LEFT_TRENCH_READY : Field.RIGHT_TRENCH_READY;

    return Commands.sequence(
        Commands.either(
            getTrenchStart2NeutralZoneScrum2BumpShoot(wantLeft),
            Commands.either(
                getTrenchStart2NeutralZoneWander2BumpShoot(wantLeft, false),
                getTrenchStart2NeutralZoneWander2BumpShoot(wantLeft, true),
                () -> responses.get(0).equals("long")),
            () -> responses.get(0).equals("scrum")),
        autoCommands.setRobotScore().withTimeout(5.0),
        autoCommands
            .setTrenchPounce(trenchReadyPouncePose)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(), autoCommands.setShooterUnderTrench(true))
            .until(
                () ->
                    hasReachedTrenchReadyY(
                        Odometry.getInstance().getEstimatedPose().getY(),
                        wantLeft,
                        AllianceFlipUtil.shouldFlip())),
        Commands.either(
            getTrenchReady2NeutralZoneDrunk2BumpShoot(wantLeft, false),
            getTrenchReady2NeutralZoneDrunk2BumpShoot(wantLeft, true),
            () -> responses.get(1).equals("long")),
        autoCommands.setRobotScore().withTimeout(5.0),
        autoCommands
            .setTrenchPounce(trenchReadyPouncePose)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(), autoCommands.setShooterUnderTrench(true))
            .until(
                () ->
                    hasReachedTrenchReadyY(
                        Odometry.getInstance().getEstimatedPose().getY(),
                        wantLeft,
                        AllianceFlipUtil.shouldFlip())),
        getTrenchReady2NeutralZoneDash(wantLeft));
  }

  static boolean hasReachedTrenchReadyY(
      double currentYMeters, boolean wantLeft, boolean wantAllianceFlip) {
    return wantLeft != wantAllianceFlip
        ? currentYMeters >= TRENCH_READY_Y_THRESHOLD_METER
        : currentYMeters <= Field.WIDTH - TRENCH_READY_Y_THRESHOLD_METER;
  }

  public void addLeft2DoubleSweepBump(AutoModeSelector autoModeSelector) {
    addStart2DoubleSweepBump(autoModeSelector, true);
  }

  public void addRight2DoubleSweepBump(AutoModeSelector autoModeSelector) {
    addStart2DoubleSweepBump(autoModeSelector, false);
  }

  // *************************** Misc ***************************
  public void addLeft2Sweep2Depot(AutoModeSelector autoModeSelector) {
    var initialTrajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .leftTrenchStart2NeutralZoneWander2BumpShoot;

    autoModeSelector.addMode(
        "2.left2Sweep2Depot",
        List.of(),
        responses -> getInitialPose(initialTrajectory),
        responses -> getLeft2Sweep2Depot());
  }

  public Command getLeft2Sweep2Depot() {
    var trajectory = TrajectoryLoader.getInstance().getTrajectorySet().depotSweep2SideShoot;

    return Commands.sequence(
        getTrenchStart2NeutralZoneWander2BumpShoot(true, false),
        autoCommands.setRobotScore().withTimeout(5.0),
        autoCommands
            .setDepotPounce()
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterPreset(
                    trajectory, 1, DEPOT_REACCEL_PRESET_MAX_ACCEL_METER_PER_SEC2))
            .withTimeout(2.0),
        autoCommands
            .followTrajectory(trajectory, 0)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands
            .followTrajectory(trajectory, 1)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands.waitUntilChassisStop(),
        autoCommands.setRobotScore());
  }

  public void addLeftCloseStart2Depot(AutoModeSelector autoModeSelector) {
    autoModeSelector.addMode(
        "3.leftClose2Depot",
        List.of(),
        responses -> AllianceFlipUtil.apply(Field.LEFT_CLOSE_START),
        responses -> getLeftCloseStart2Depot());
  }

  public Command getLeftCloseStart2Depot() {
    var trajectory = TrajectoryLoader.getInstance().getTrajectorySet().depotSweep2SideShoot;

    return Commands.sequence(
        autoCommands
            .waitUntilAllianceConfirmed()
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterPreset(
                    trajectory, 1, CLOSE_DEPOT_PRESET_MAX_ACCEL_METER_PER_SEC2)),
        autoCommands.resetPose(Field.LEFT_CLOSE_START),
        autoCommands
            .setDepotPounce()
            .alongWith(autoCommands.setIntakeAndIndexerCollect())
            .withTimeout(2.0),
        autoCommands
            .followTrajectory(trajectory, 0)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands
            .followTrajectory(trajectory, 1)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands.waitUntilChassisStop(),
        autoCommands.setRobotScore());
  }

  // *************************** Risky ***************************
  public void addLeftCloseStart2Sweep2BumpShoot2Depot(AutoModeSelector autoModeSelector) {
    var sweepTrajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .leftCloseTrenchStart2NeutralZoneWander2BumpShoot;

    var depotPreTrajectory = TrajectoryLoader.getInstance().getTrajectorySet().leftBumpShoot2Depot;

    var depotShootTrajectory =
        TrajectoryLoader.getInstance().getTrajectorySet().depotSweep2SideShoot;

    autoModeSelector.addMode(
        "4L.leftClose2Sweep2BumpShoot2Depot",
        List.of(new AutoModeSelector.AutoQuestion("End dash?", List.of("no", "yes"))),
        responses -> getInitialPose(sweepTrajectory),
        this::getLeftCloseStart2Sweep2BumpShoot2Depot);
  }

  public Command getLeftCloseStart2Sweep2BumpShoot2Depot(List<String> responses) {
    var sweepTrajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .leftCloseTrenchStart2NeutralZoneWander2BumpShoot;
    var depotPreTrajectory = TrajectoryLoader.getInstance().getTrajectorySet().leftBumpShoot2Depot;
    var depotShootTrajectory =
        TrajectoryLoader.getInstance().getTrajectorySet().depotSweep2SideShoot;

    return Commands.sequence(
        Commands.waitSeconds(2.0)
            .alongWith(
                autoCommands.setShooterPreset(
                    sweepTrajectory, 2, LEFT_CLOSE_SWEEP_PRESET_MAX_ACCEL_METER_PER_SEC2)),
        autoCommands
            .waitUntilAllianceConfirmed()
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands
            .resetPoseByTrajectory(sweepTrajectory)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterUnderTrench(true)),
        autoCommands.followTrajectory(sweepTrajectory, 0),
        Commands.waitSeconds(3.5),
        autoCommands
            .followTrajectory(sweepTrajectory, 1)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands.maybeStickOnTrajectoryEnd(sweepTrajectory, 1),
        autoCommands
            .crossBump(sweepTrajectory, 2)
            .alongWith(autoCommands.setShooterUnderTrench(false)),
        autoCommands.setRobotScoreWithFeedDuration(0.5),
        autoCommands
            .followTrajectory(depotPreTrajectory)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterPreset(
                    depotShootTrajectory, 1, DEPOT_REACCEL_PRESET_MAX_ACCEL_METER_PER_SEC2)),
        autoCommands.followTrajectory(depotShootTrajectory, 0),
        autoCommands.followTrajectory(depotShootTrajectory, 1),
        autoCommands.waitUntilChassisStop(),
        Commands.either(
            autoCommands.setRobotScore(),
            autoCommands
                .setRobotScore()
                .withTimeout(2.0)
                .andThen(
                    autoCommands
                        .setTrenchPounce(Field.LEFT_TRENCH_READY)
                        .alongWith(
                            autoCommands.setIntakeAndIndexerCollect(),
                            autoCommands.setShooterUnderTrench(true))),
            () -> responses.get(0).equals("no")));
  }

  public void addRightCloseStart2Sweep2BumpShoot(AutoModeSelector autoModeSelector) {
    var mainTrajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .rightCloseTrenchStart2NeutralZoneWander2BumpShoot;

    autoModeSelector.addMode(
        "4R.rightClose2Sweep2BumpShoot",
        List.of(),
        responses -> getInitialPose(mainTrajectory),
        responses -> getRightCloseStart2Sweep2BumpShoot());
  }

  public Command getRightCloseStart2Sweep2BumpShoot() {
    var mainTrajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .rightCloseTrenchStart2NeutralZoneWander2BumpShoot;
    var subTrajectory = TrajectoryLoader.getInstance().getTrajectorySet().rightBumpShoot2MidShoot;

    return Commands.sequence(
        Commands.waitSeconds(1.0)
            .alongWith(
                autoCommands.setShooterPreset(
                    mainTrajectory, 2, RIGHT_CLOSE_SWEEP_PRESET_MAX_ACCEL_METER_PER_SEC2)),
        autoCommands
            .waitUntilAllianceConfirmed()
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands
            .resetPoseByTrajectory(mainTrajectory)
            .alongWith(
                autoCommands.setIntakeAndIndexerCollect(),
                autoCommands.setShooterUnderTrench(true)),
        autoCommands.followTrajectory(mainTrajectory, 0),
        Commands.waitSeconds(2.0),
        autoCommands
            .followTrajectory(mainTrajectory, 1)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands.maybeStickOnTrajectoryEnd(mainTrajectory, 1),
        autoCommands
            .crossBump(mainTrajectory, 2)
            .alongWith(autoCommands.setShooterUnderTrench(false)),
        autoCommands
            .followTrajectory(subTrajectory)
            .alongWith(
                autoCommands.setShooterPreset(
                    subTrajectory, RIGHT_CLOSE_FINAL_PRESET_MAX_ACCEL_METER_PER_SEC2)),
        autoCommands.setRobotScore());
  }
}
