// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.command;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.Robot;
import com.nextinnovation.team8214.Sim;
import com.nextinnovation.team8214.subsystem.hopper.Hopper;
import com.nextinnovation.team8214.subsystem.indexer.Indexer;
import com.nextinnovation.team8214.subsystem.indexer.IndexerGoal;
import com.nextinnovation.team8214.subsystem.intake.Intake;
import com.nextinnovation.team8214.subsystem.intake.IntakeGoal;
import com.nextinnovation.team8214.subsystem.shooter.Shooter;
import com.nextinnovation.team8214.subsystem.swerve.Swerve;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import com.nextinnovation.team8214.util.AllianceValue;
import com.nextinnovation.team8214.util.EqualsUtil;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.Set;
import java.util.function.BooleanSupplier;
import lombok.RequiredArgsConstructor;
import org.littletonrobotics.junction.Logger;

@RequiredArgsConstructor
public class AutoCommands {
  private static final double BUMP_POUNCE_TIMEOUT_MARGIN_SEC = 0.5;

  private final Swerve swerve;
  private final Intake intake;
  private final Indexer indexer;
  private final Hopper hopper;
  private final Shooter shooter;

  Command resetPoseByTrajectory(AllianceValue<Trajectory<SwerveSample>> trajectory) {
    return Commands.runOnce(
        () ->
            Odometry.getInstance()
                .resetPose(
                    AllianceFlipUtil.apply(
                        trajectory.get().getInitialPose(AllianceFlipUtil.shouldFlip()).get())));
  }

  Command resetPoseByTrajectory(Trajectory<SwerveSample> trajectory) {
    return Commands.runOnce(
        () ->
            Odometry.getInstance()
                .resetPose(
                    AllianceFlipUtil.apply(
                        trajectory.getInitialPose(AllianceFlipUtil.shouldFlip()).get())));
  }

  public Command resetPose(Pose2d pose) {
    return Commands.runOnce(() -> Odometry.getInstance().resetPose(AllianceFlipUtil.apply(pose)));
  }

  Command followTrajectory(AllianceValue<Trajectory<SwerveSample>> trajectory) {
    return Commands.runOnce(() -> swerve.setTrajectory(trajectory.get()), swerve)
        .andThen(Commands.waitUntil(swerve::hasTrajectoryDone));
  }

  public Command followTrajectory(Trajectory<SwerveSample> trajectory) {
    return Commands.runOnce(() -> swerve.setTrajectory(trajectory), swerve)
        .andThen(Commands.waitUntil(swerve::hasTrajectoryDone));
  }

  public Command followTrajectory(
      AllianceValue<Trajectory<SwerveSample>> trajectories, int splitIndex) {
    return Commands.runOnce(
            () -> swerve.setTrajectory(trajectories.get().getSplit(splitIndex).get()), swerve)
        .andThen(Commands.waitUntil(swerve::hasTrajectoryDone));
  }

  public Command followTrajectory(
      AllianceValue<Trajectory<SwerveSample>> trajectories,
      int splitIndex,
      int presetRobotInFieldSuppliedTrajectorySplitIndex,
      double flywheelMaxAccelMeterPerSec2) {
    return Commands.runOnce(
            () -> swerve.setTrajectory(trajectories.get().getSplit(splitIndex).get()), swerve)
        .alongWith(
            setShooterPreset(
                trajectories,
                presetRobotInFieldSuppliedTrajectorySplitIndex,
                flywheelMaxAccelMeterPerSec2))
        .andThen(Commands.waitUntil(swerve::hasTrajectoryDone));
  }

  public Command followTrajectory(Trajectory<SwerveSample> trajectories, int splitIndex) {
    return Commands.runOnce(
            () -> swerve.setTrajectory(trajectories.getSplit(splitIndex).get()), swerve)
        .andThen(Commands.waitUntil(swerve::hasTrajectoryDone));
  }

  public Command crossBump(AllianceValue<Trajectory<SwerveSample>> trajectories, int splitIndex) {
    return Commands.defer(
        () -> {
          var bumpTrajectory =
              trajectories
                  .get()
                  .getSplit(splitIndex)
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Missing bump trajectory split " + splitIndex));
          var startPose =
              bumpTrajectory
                  .getInitialPose(false)
                  .orElseThrow(() -> new IllegalArgumentException("Missing bump start pose"));
          var goalPose =
              bumpTrajectory
                  .getFinalPose(false)
                  .orElseThrow(() -> new IllegalArgumentException("Missing bump goal pose"));
          var timeoutSec = bumpTrajectory.getTotalTime() + BUMP_POUNCE_TIMEOUT_MARGIN_SEC;

          return Commands.sequence(
                  Commands.runOnce(
                      () -> {
                        Logger.recordOutput("auto/crossBump/timeoutSec", timeoutSec);
                        swerve.setBumpPounce(startPose, goalPose);
                      },
                      swerve),
                  Commands.waitUntil(
                      () -> swerve.hasBumpPounceDone() || swerve.hasBumpPounceSafetyStopped()))
              .withTimeout(timeoutSec)
              .finallyDo(swerve::clearBumpPounce);
        },
        Set.of(swerve));
  }

  public Command waitUntilAllianceConfirmed() {
    return Commands.waitUntil(Robot::hasAllianceConfirmed);
  }

  Command setIntakeAndIndexerIdle() {
    return Commands.runOnce(
        () -> {
          intake.setGoal(IntakeGoal.IDLE);
          indexer.setGoal(IndexerGoal.IDLE);
        },
        intake,
        indexer);
  }

  Command setShooterPreset(
      AllianceValue<Trajectory<SwerveSample>> trajectories,
      int splitIndex,
      double flywheelMaxAccelMeterPerSec2) {
    return Commands.runOnce(
        () ->
            shooter.setPreset(
                trajectories.get().getSplit(splitIndex).get().getFinalPose(false).get(),
                flywheelMaxAccelMeterPerSec2),
        shooter);
  }

  Command setShooterPreset(
      AllianceValue<Trajectory<SwerveSample>> trajectory, double flywheelMaxAccelMeterPerSec2) {
    return Commands.runOnce(
        () ->
            shooter.setPreset(
                trajectory.get().getFinalPose(false).get(), flywheelMaxAccelMeterPerSec2),
        shooter);
  }

  public Command setIntakeAndIndexerCollect() {
    return Commands.runOnce(
        () -> {
          intake.setGoal(IntakeGoal.COLLECT);
          indexer.setGoal(IndexerGoal.IDLE);
        },
        intake,
        indexer);
  }

  public Command setRobotScore() {
    return Commands.sequence(
            Commands.runOnce(
                () -> {
                  shooter.setUnderTrench(false);
                  intake.setGoal(IntakeGoal.SCORE);
                  indexer.setGoal(IndexerGoal.IDLE);
                },
                intake,
                indexer),
            shooter.runOnce(shooter::setScore),
            Commands.waitUntil(() -> shooter.getTargetFieldCentricYaw().isPresent()),
            Commands.runOnce(
                () -> swerve.setScoreHeadingGoal(() -> shooter.getTargetFieldCentricYaw().get())),
            Commands.waitUntil(
                () -> {
                  var hasSwerveRotationStop =
                      EqualsUtil.epsilonEquals(
                          Odometry.getInstance().getFieldCentricVel().dtheta,
                          0.0,
                          Units.degreesToRadians(10.0));

                  return shooter.onTarget() && swerve.atHeadingGoal() && hasSwerveRotationStop;
                }),
            Commands.runOnce(
                () -> {
                  indexer.setGoal(IndexerGoal.SCORE);
                  intake.setGoal(IntakeGoal.SCORE);
                  hopper.resetAutoFeederSensorEmptyDebouncer();
                },
                indexer,
                intake),
            Commands.deadline(
                Commands.waitUntil(hopper::isFeederSensorEmptyForAuto)
                    .alongWith(Commands.waitSeconds(1.0)),
                intake.singleCompressCmd(),
                Commands.run(
                    () -> {
                      if (Config.MODE == Config.Mode.SIM) {
                        Sim.getInstance().shoot();
                      }
                    })))
        .finallyDo(
            () -> {
              swerve.clearHeadingGoal();
              intake.setGoal(IntakeGoal.IDLE);
              indexer.setGoal(IndexerGoal.IDLE);
              shooter.setIdle();
            });
  }

  public Command setRobotScoreWithFeedDuration(double feedDurationSec) {
    return setRobotScore()
        .raceWith(
            Commands.waitUntil(() -> indexer.getGoal() == IndexerGoal.SCORE)
                .andThen(Commands.waitSeconds(feedDurationSec)));
  }

  public Command waitUntilChassisStop() {
    return Commands.waitUntil(
        () -> {
          var vel = Odometry.getInstance().getFieldCentricVel();

          return EqualsUtil.epsilonEquals(vel.dx, 0.0, 0.05)
              && EqualsUtil.epsilonEquals(vel.dy, 0.0, 0.05)
              && EqualsUtil.epsilonEquals(vel.dtheta, 0.0, Units.degreesToRadians(5.0));
        });
  }

  Command setTrenchPounce(Pose2d goalTrenchPose) {
    return Commands.runOnce(
            () -> swerve.setTrenchPounce(AllianceFlipUtil.apply(goalTrenchPose)), swerve)
        .andThen(Commands.waitUntil(swerve::hasTrenchPounceDone))
        .finallyDo(swerve::clearTrenchPounce);
  }

  public Command setDepotPounce() {
    return Commands.runOnce(swerve::setDepotPounce, swerve)
        .andThen(Commands.waitUntil(swerve::hasDepotPounceDone))
        .finallyDo(swerve::clearDepotPounce);
  }

  Command setShooterUnderTrench(boolean isUnderTrench) {
    return Commands.runOnce(() -> shooter.setUnderTrench(isUnderTrench));
  }

  public Command maybeStickOnTrajectoryEnd(
      AllianceValue<Trajectory<SwerveSample>> trajectories, int splitIndex) {
    final BooleanSupplier isCloseEnough =
        () ->
            Odometry.getInstance()
                    .getEstimatedPose()
                    .getTranslation()
                    .getDistance(
                        trajectories
                            .get()
                            .getSplit(splitIndex)
                            .get()
                            .getFinalPose(false)
                            .get()
                            .getTranslation())
                <= 0.4;

    final BooleanSupplier isHeadingFine =
        () -> {
          final var rawErr =
              trajectories
                      .get()
                      .getSplit(splitIndex)
                      .get()
                      .getFinalPose(false)
                      .get()
                      .getRotation()
                      .getRadians()
                  - Odometry.getInstance().getEstimatedPose().getRotation().getRadians();

          final var wrappedErr = MathUtil.inputModulus(rawErr, -Math.PI, Math.PI);

          return Math.abs(wrappedErr) < Units.degreesToRadians(30.0);
        };

    return Commands.either(
        Commands.none(),
        Commands.sequence(
                Commands.runOnce(
                    () ->
                        swerve.setStick2PointStartPounce(
                            trajectories
                                .get()
                                .getSplit(splitIndex)
                                .get()
                                .getFinalPose(false)
                                .get()),
                    swerve),
                Commands.waitUntil(swerve::hasStick2PointStartPounceDone))
            .finallyDo(swerve::clearStick2PointStartPounce)
            .until(() -> isCloseEnough.getAsBoolean() && isHeadingFine.getAsBoolean()),
        () -> isCloseEnough.getAsBoolean() && isHeadingFine.getAsBoolean());
  }

  Command setIntakeAndShooterHoming(boolean wantHoming) {
    return intake
        .runOnce(() -> intake.setHoming(wantHoming))
        .alongWith(shooter.runOnce(() -> shooter.setHoming(wantHoming)));
  }

  Command forceHomeIntakeAndShooter() {
    return intake
        .runOnce(intake::forceHomeAtStart)
        .alongWith(shooter.runOnce(shooter::forceHomeAtStart));
  }

  Command setSwerveDriveNeutralMode(boolean wantBrake) {
    return Commands.runOnce(() -> swerve.setDriveNeutralMode(wantBrake));
  }
}
