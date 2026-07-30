// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.agent;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.TrajectoryLoader;
import com.nextinnovation.team8214.command.AutoCommands;
import com.nextinnovation.team8214.command.AutoModes;
import com.nextinnovation.team8214.subsystem.hopper.Hopper;
import com.nextinnovation.team8214.subsystem.indexer.Indexer;
import com.nextinnovation.team8214.subsystem.intake.Intake;
import com.nextinnovation.team8214.subsystem.shooter.Shooter;
import com.nextinnovation.team8214.subsystem.swerve.Swerve;
import com.nextinnovation.team8214.util.AllianceValue;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.List;

final class AgentAutoCommands {
  private final AutoCommands autoCommands;
  private final AutoModes autoModes;

  AgentAutoCommands(Swerve swerve, Intake intake, Indexer indexer, Hopper hopper, Shooter shooter) {
    autoCommands = new AutoCommands(swerve, intake, indexer, hopper, shooter);
    autoModes = new AutoModes(swerve, intake, indexer, hopper, shooter);
  }

  Command resetPose(Pose2d pose) {
    return autoCommands.resetPose(pose);
  }

  Command scoreFromPose(Pose2d pose, double timeoutSec) {
    return Commands.sequence(resetPose(pose), autoCommands.setRobotScore().withTimeout(timeoutSec));
  }

  Command leftClose2Depot() {
    var trajectory = TrajectoryLoader.getInstance().getTrajectorySet().depotSweep2SideShoot;

    return Commands.sequence(
        autoCommands
            .waitUntilAllianceConfirmed()
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        resetPose(Field.LEFT_CLOSE_START),
        autoCommands
            .setDepotPounce()
            .alongWith(autoCommands.setIntakeAndIndexerCollect())
            .withTimeout(2.0),
        autoCommands
            .followTrajectory(trajectory, 0)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands
            .followTrajectory(trajectory, 1, 1, 1.5)
            .alongWith(autoCommands.setIntakeAndIndexerCollect()),
        autoCommands.waitUntilChassisStop().withTimeout(1.0),
        autoCommands.setRobotScore().withTimeout(5.0));
  }

  Command bumpThrough() {
    var trajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .leftCloseTrenchStart2NeutralZoneWander2BumpShoot;
    var bumpThroughSplitIndex = 2;

    return Commands.sequence(
        resetPoseBySelectedTrajectorySplit(trajectory, bumpThroughSplitIndex),
        autoCommands.crossBump(trajectory, bumpThroughSplitIndex),
        autoCommands.waitUntilChassisStop().withTimeout(1.0),
        Commands.waitSeconds(0.75));
  }

  Command bumpThroughAfterStick() {
    var trajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .leftCloseTrenchStart2NeutralZoneWander2BumpShoot;
    var stickSplitIndex = 1;
    var bumpThroughSplitIndex = 2;

    return Commands.sequence(
        Commands.runOnce(
            () -> {
              var stickGoal =
                  trajectory.get().getSplit(stickSplitIndex).get().getFinalPose(false).get();
              var bumpGoal =
                  trajectory.get().getSplit(bumpThroughSplitIndex).get().getFinalPose(false).get();
              var awayFromBump =
                  stickGoal.getTranslation().minus(bumpGoal.getTranslation()).getAngle();
              var offsetPose =
                  new Pose2d(
                      stickGoal.getTranslation().plus(new Translation2d(0.6, awayFromBump)),
                      stickGoal.getRotation().rotateBy(Rotation2d.fromDegrees(35.0)));
              Odometry.getInstance().resetPose(offsetPose);
            }),
        autoCommands.maybeStickOnTrajectoryEnd(trajectory, stickSplitIndex).withTimeout(4.0),
        autoCommands.crossBump(trajectory, bumpThroughSplitIndex),
        autoCommands.waitUntilChassisStop().withTimeout(1.0),
        Commands.waitSeconds(0.75));
  }

  Command left2DoubleSweepBump(List<String> responses) {
    return autoModes.getLeft2DoubleSweepBump(responses);
  }

  Command right2DoubleSweepBump(List<String> responses) {
    return autoModes.getRight2DoubleSweepBump(responses);
  }

  Command left2Sweep2Depot() {
    return autoModes.getLeft2Sweep2Depot();
  }

  Command leftCloseStart2Depot() {
    return autoModes.getLeftCloseStart2Depot();
  }

  Command leftCloseStart2Sweep2BumpShoot2Depot(List<String> responses) {
    return autoModes.getLeftCloseStart2Sweep2BumpShoot2Depot(responses);
  }

  Command rightCloseStart2Sweep2BumpShoot() {
    return autoModes.getRightCloseStart2Sweep2BumpShoot();
  }

  Command longestTrajectory() {
    var trajectory =
        TrajectoryLoader.getInstance()
            .getTrajectorySet()
            .leftTrenchReady2NeutralZoneDrunk2BumpShoot;

    return Commands.sequence(
        resetPoseBySelectedTrajectorySplit(trajectory, 0),
        autoCommands.followTrajectory(trajectory.get()).withTimeout(8.0),
        autoCommands.waitUntilChassisStop().withTimeout(1.0),
        Commands.waitSeconds(0.25));
  }

  private Command resetPoseBySelectedTrajectorySplit(
      AllianceValue<Trajectory<SwerveSample>> trajectory, int splitIndex) {
    return Commands.runOnce(
        () ->
            Odometry.getInstance()
                .resetPose(
                    trajectory.get().getSplit(splitIndex).get().getInitialPose(false).get()));
  }
}
