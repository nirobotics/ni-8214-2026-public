// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import com.nextinnovation.team8214.agent.AgentAutoModeSelector;
import com.nextinnovation.team8214.agent.AgentAutoModes;
import com.nextinnovation.team8214.agent.AgentAutoRunner;
import com.nextinnovation.team8214.command.AutoModes;
import com.nextinnovation.team8214.subsystem.Visualizer;
import com.nextinnovation.team8214.subsystem.hopper.Hopper;
import com.nextinnovation.team8214.subsystem.indexer.Indexer;
import com.nextinnovation.team8214.subsystem.indexer.IndexerGoal;
import com.nextinnovation.team8214.subsystem.intake.Intake;
import com.nextinnovation.team8214.subsystem.intake.IntakeGoal;
import com.nextinnovation.team8214.subsystem.shooter.Shooter;
import com.nextinnovation.team8214.subsystem.swerve.Swerve;
import com.nextinnovation.team8214.subsystem.vision.ApriltagVision;
import com.nextinnovation.team8214.util.Alert;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import com.nextinnovation.team8214.util.EqualsUtil;
import com.nextinnovation.team8214.util.GeomUtil;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import java.util.Optional;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;

@ExtensionMethod({GeomUtil.class})
public class RobotContainer {
  private static final class VisualizerModelIndex {
    static final int DRIVETRAIN = 0;
    static final int SHOOTER = 1;
    static final int INTAKE = 2;
    static final int HOPPER = 3;
  }

  private static final double HOPPER_UNCOVERED_METERS = 0.3;

  private final Swerve swerve;
  private final Intake intake;
  private final Indexer indexer;
  private final Hopper hopper;
  private final Shooter shooter;
  private final ApriltagVision vision;

  private final Odometry odometry;

  private final Visualizer visualizer;

  private final CommandXboxController driver = new CommandXboxController(Ports.Joystick.DRIVER);
  private final CommandXboxController codriver = new CommandXboxController(Ports.Joystick.CODRIVER);

  private final AutoModeSelector autoModeSelector;
  private final AgentAutoModeSelector agentAutoModeSelector = new AgentAutoModeSelector();

  private final Alert driverDisconnected =
      new Alert("Driver controller disconnected (port 0).", Alert.AlertType.WARNING);
  private final Alert codriverDisconnected =
      new Alert("Codriver controller disconnected (port 1).", Alert.AlertType.WARNING);
  private final Alert autoWinnerNotSet =
      new Alert("!!! AUTO WINNER OR FORCE ACTIVE NOT SET !!!", Alert.AlertType.ERROR);
  private final Alert forceActive = new Alert("!!! FORCE ACTIVE !!!", Alert.AlertType.INFO);

  @Setter private boolean isRobotRebootDuringTeleop = true;
  private int cachedTeleopMatchTime = 140;
  private boolean isCoveredByHopper = true;

  RobotContainer() {
    odometry = Odometry.getInstance();
    swerve = new Swerve();
    intake = new Intake();
    indexer = new Indexer();
    hopper = new Hopper();
    shooter = new Shooter();
    indexer.registerIsCoveredByHopperSignalSupplier(() -> isCoveredByHopper);
    indexer.registerHopperPreloadSignalSupplier(hopper::isShouldPreload);
    shooter.registerIsCoveredByHopperSignalSupplier(() -> isCoveredByHopper);
    vision = new ApriltagVision();
    visualizer = new Visualizer();

    configVisualizer();
    visualizer.print();

    if (Config.MODE == Config.Mode.SIM) {
      Sim.getInstance().setAngleRadSupplier(shooter::getPitchAngleRad);
      Sim.getInstance().setVelMeterPerSecSupplier(shooter::getFlywheelVelMeterPerSec);
    }

    if (AgentAutoRunner.isEnabled()) {
      autoModeSelector = null;
      var agentAutoModes = new AgentAutoModes(swerve, intake, indexer, hopper, shooter);
      agentAutoModes.addDefaultModes(agentAutoModeSelector);
      AgentAutoRunner.selectAgentAutoMode(agentAutoModeSelector);
    } else {
      autoModeSelector = new AutoModeSelector("Auto");
      var autoModes = new AutoModes(swerve, intake, indexer, hopper, shooter);
      autoModes.addLeft2DoubleSweepBump(autoModeSelector); // 1LB
      autoModes.addRight2DoubleSweepBump(autoModeSelector); // 1RB
      autoModes.addLeft2Sweep2Depot(autoModeSelector); // 2
      autoModes.addLeftCloseStart2Depot(autoModeSelector); // 3
      autoModes.addLeftCloseStart2Sweep2BumpShoot2Depot(autoModeSelector); // 4L
      autoModes.addRightCloseStart2Sweep2BumpShoot(autoModeSelector); // 4R
    }

    configButtonBindings();
  }

  private void configVisualizer() {
    visualizer.registerVisualizedComponent(
        Visualizer.BASE_FRAME,
        "swerve",
        VisualizerModelIndex.DRIVETRAIN,
        swerve::getRobotToSwerveTransform);

    visualizer.registerVisualizedComponent(
        "swerve", "shooter", VisualizerModelIndex.SHOOTER, shooter::getSwerveToShooterTransform);
    visualizer.registerVisualizedComponent(
        "swerve", "intake", VisualizerModelIndex.INTAKE, intake::getSwerveToIntakeTransform);
    visualizer.registerVisualizedComponent(
        "swerve",
        "hopper",
        VisualizerModelIndex.HOPPER,
        () ->
            new Transform3d(
                0.0, 0.0, isCoveredByHopper ? 0.0 : HOPPER_UNCOVERED_METERS, new Rotation3d()));
  }

  private void configButtonBindings() {
    CommandScheduler.getInstance().getActiveButtonLoop().clear();

    codriver
        .x()
        .onTrue(
            Commands.runOnce(
                () -> {
                  HubShiftUtil.setAllianceWinOverride(() -> Optional.of(false));
                  HubShiftUtil.setForceActive(false);
                }));

    codriver
        .y()
        .onTrue(
            Commands.runOnce(
                () -> {
                  HubShiftUtil.setAllianceWinOverride(() -> Optional.of(true));
                  HubShiftUtil.setForceActive(false);
                }));

    codriver
        .a()
        .onTrue(
            Commands.runOnce(
                () -> {
                  HubShiftUtil.setAllianceWinOverride(Optional::empty);
                  HubShiftUtil.setForceActive(false);
                }));

    codriver
        .b()
        .onTrue(
            Commands.runOnce(
                () -> {
                  HubShiftUtil.setAllianceWinOverride(Optional::empty);
                  HubShiftUtil.setForceActive(true);
                }));

    swerve.setDefaultCommand(swerve.run(this::updateSwerveTeleopInput).withName("Swerve Teleop"));

    shooter.setDefaultCommand(
        shooter
            .run(
                () -> {
                  shooter.setIdle();
                })
            .withName("Shooter Idle"));

    driver
        .start()
        .onTrue(
            Commands.runOnce(
                    () -> {
                      swerve.disableTeleopControllerHeadingMaintainer();
                      odometry.resetPose(
                          new Pose2d(
                              odometry.getEstimatedPose().getTranslation(),
                              AllianceFlipUtil.apply(new Rotation2d())));
                    })
                .ignoringDisable(true)
                .withName("Swerve Home Gyro"));

    driver
        .a()
        .onTrue(
            Commands.runOnce(
                    () -> {
                      intake.setGoal(IntakeGoal.IDLE);
                      indexer.setGoal(IndexerGoal.IDLE);
                      shooter.setIdle();
                    },
                    intake,
                    indexer,
                    shooter)
                .withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming)
                .withName("Superstructure Idle"));

    driver
        .rightTrigger()
        .onTrue(
            Commands.sequence(
                    Commands.runOnce(
                        () -> {
                          shooter.setUnderTrench(false);
                          intake.setGoal(IntakeGoal.SCORE);
                          indexer.setGoal(IndexerGoal.IDLE);
                          swerve.setTeleopInput(0.0, 0.0, 0.0, false);
                        },
                        intake,
                        indexer,
                        swerve),
                    setShooterScoreAndSwerveAimHub(),
                    Commands.waitUntil(
                        () -> {
                          var hasSwerveRotationStop =
                              EqualsUtil.epsilonEquals(
                                  Odometry.getInstance().getFieldCentricVel().dtheta,
                                  0.0,
                                  Units.degreesToRadians(10.0));

                          return shooter.onTarget()
                              && swerve.atHeadingGoal()
                              && hasSwerveRotationStop;
                        }),
                    Commands.run(
                            () -> {
                              if (swerve.atHeadingGoal()) {
                                swerve.setFort();
                              } else {
                                swerve.setTeleopInput(0.0, 0.0, 0.0, false);
                              }
                            },
                            swerve)
                        .alongWith(
                            Commands.waitUntil(
                                    () ->
                                        !Config.WANT_SCORE_BY_HUB_SHIFT
                                            || HubShiftUtil.isForceActive()
                                            || HubShiftUtil.getShiftedShiftInfo().active())
                                .andThen(
                                    Commands.sequence(
                                            Commands.runOnce(
                                                () -> {
                                                  indexer.setGoal(IndexerGoal.SCORE);
                                                  intake.setGoal(IntakeGoal.SCORE);
                                                },
                                                indexer,
                                                intake),
                                            intake.singleCompressCmd())
                                        .alongWith(
                                            Commands.run(
                                                () -> {
                                                  if (Config.MODE == Config.Mode.SIM) {
                                                    Sim.getInstance().shoot();
                                                  }
                                                }))
                                        .until(
                                            () ->
                                                !(!Config.WANT_SCORE_BY_HUB_SHIFT
                                                    || HubShiftUtil.isForceActive()
                                                    || HubShiftUtil.getShiftedShiftInfo().active()))
                                        .finallyDo(
                                            () -> {
                                              intake.setGoal(IntakeGoal.IDLE);
                                              indexer.setGoal(IndexerGoal.IDLE);
                                              shooter.setIdle();
                                              swerve.clearHeadingGoal();
                                            }))))
                .finallyDo(
                    () -> {
                      swerve.clearHeadingGoal();
                    })
                .withName("Score Shoot"));

    driver
        .povUp()
        .onTrue(
            Commands.runOnce(
                () ->
                    shooter.setManualScoreDistanceOffset(
                        shooter.getManualScoreDistanceOffset() + 0.1)));

    driver
        .povDown()
        .onTrue(
            Commands.runOnce(
                () ->
                    shooter.setManualScoreDistanceOffset(
                        shooter.getManualScoreDistanceOffset() - 0.1)));

    driver.povLeft().onTrue(Commands.runOnce(shooter::resetToDefaultScoreManualDistanceOffset));

    driver
        .rightBumper()
        .onTrue(
            Commands.sequence(
                    Commands.runOnce(
                        () -> {
                          shooter.setUnderTrench(false);
                          intake.setGoal(IntakeGoal.TRANSPORT);
                          indexer.setGoal(IndexerGoal.IDLE);
                          swerve.setTeleopInput(0.0, 0.0, 0.0, false);
                        },
                        intake,
                        indexer,
                        swerve),
                    setShooterTransportAndSwerveAimPlacement(),
                    Commands.waitUntil(() -> shooter.onTarget() && swerve.atHeadingGoal()),
                    Commands.run(
                            () -> {
                              if (swerve.atHeadingGoal()) {
                                swerve.setFort();
                              } else {
                                swerve.setTeleopInput(0.0, 0.0, 0.0, false);
                              }
                            },
                            swerve)
                        .alongWith(
                            Commands.run(
                                () -> {
                                  if (odometry.isInTransportForbiddenZone()) {
                                    indexer.setGoal(IndexerGoal.IDLE);
                                  } else {
                                    indexer.setGoal(IndexerGoal.TRANSPORT);
                                    if (Config.MODE == Config.Mode.SIM) {
                                      Sim.getInstance().shoot();
                                    }
                                  }
                                },
                                indexer),
                            intake.singleCompressCmd()))
                .finallyDo(
                    () -> {
                      swerve.clearHeadingGoal();
                    })
                .withName("Transport Shoot"));

    driver
        .leftTrigger()
        .onTrue(
            Commands.runOnce(
                    () -> {
                      intake.setGoal(IntakeGoal.COLLECT);
                      indexer.setGoal(IndexerGoal.IDLE);
                      shooter.setIdle();
                    },
                    intake,
                    indexer,
                    shooter)
                .withName("Collect"));

    driver
        .leftBumper()
        .onTrue(
            Commands.runOnce(
                    () -> {
                      shooter.setUnderTrench(true);
                      shooter.setIdle();
                      intake.setGoal(IntakeGoal.EJECT);
                      indexer.setGoal(IndexerGoal.EJECT);
                    },
                    intake,
                    indexer,
                    shooter)
                .withName("Eject"));

    driver
        .b()
        .onTrue(
            Commands.runOnce(() -> shooter.setEnableIdleFlywheel(true))
                .withName("Flywheel Enable"));

    driver
        .x()
        .onTrue(
            Commands.runOnce(() -> shooter.setEnableIdleFlywheel(false))
                .withName("Flywheel Disable"));

    driver
        .y()
        .onTrue(
            Commands.sequence(
                    Commands.runOnce(
                        () -> {
                          shooter.setUnderTrench(false);
                          shooter.setFence();
                          intake.setGoal(IntakeGoal.SCORE);
                          indexer.setGoal(IndexerGoal.IDLE);
                        },
                        intake,
                        indexer,
                        shooter),
                    Commands.waitUntil(shooter::onTarget),
                    Commands.runOnce(
                        () -> {
                          indexer.setGoal(IndexerGoal.FENCE);
                          intake.setGoal(IntakeGoal.SCORE);
                        },
                        indexer,
                        intake),
                    intake.singleCompressCmd())
                .withName("Fence Shoot"));

    driver
        .back()
        .onTrue(
            intake
                .getHomeCmd()
                .alongWith(
                    Commands.either(
                        Commands.deferredProxy(shooter::getHomeCmd),
                        Commands.none(),
                        () ->
                            shooter.getMode() != Shooter.ControlMode.SCORE
                                && shooter.getMode() != Shooter.ControlMode.TRANSPORT
                                && shooter.getMode() != Shooter.ControlMode.FENCE)));

    driver
        .rightStick()
        .onTrue(
            Commands.runOnce(
                () -> {
                  intake.setGoal(IntakeGoal.SAFE_IDLE);
                  indexer.setGoal(IndexerGoal.IDLE);
                },
                intake,
                indexer));

    RobotModeTriggers.teleop()
        .and(() -> isRobotRebootDuringTeleop)
        .and(DriverStation::isFMSAttached)
        .and(() -> DriverStation.getMatchTime() < 140.0 && DriverStation.getMatchTime() > 0.0)
        .onTrue(
            Commands.sequence(
                Commands.runOnce(() -> cachedTeleopMatchTime = (int) DriverStation.getMatchTime()),
                Commands.waitUntil(
                    () -> cachedTeleopMatchTime != (int) DriverStation.getMatchTime()),
                Commands.runOnce(
                    () -> HubShiftUtil.resetShiftTimer(140.0 - DriverStation.getMatchTime()))));

    // Warn for missing game data
    var teleopElapsedTimer = new Timer();
    RobotModeTriggers.teleop().onTrue(Commands.runOnce(teleopElapsedTimer::restart));
    RobotModeTriggers.teleop()
        .and(() -> DriverStation.getGameSpecificMessage().isEmpty())
        .and(() -> HubShiftUtil.getAllianceWinOverride().isEmpty())
        .and(() -> !HubShiftUtil.isForceActive())
        .and(() -> teleopElapsedTimer.hasElapsed(1.0))
        .whileTrue(
            Commands.runEnd(
                () -> {
                  driver.setRumble(GenericHID.RumbleType.kBothRumble, 1);
                  codriver.setRumble(GenericHID.RumbleType.kBothRumble, 1);
                },
                () -> {
                  driver.setRumble(GenericHID.RumbleType.kBothRumble, 0);
                  codriver.setRumble(GenericHID.RumbleType.kBothRumble, 0);
                }))
        .whileTrue(
            Commands.startEnd(() -> autoWinnerNotSet.set(true), () -> autoWinnerNotSet.set(false)));

    RobotModeTriggers.teleop()
        .and(HubShiftUtil::isForceActive)
        .whileTrue(Commands.startEnd(() -> forceActive.set(true), () -> forceActive.set(false)));

    // End-of-active-shift warning
    for (var i = 1; i <= 5; i++) {
      double time = i;
      var shiftAboutToEnd =
          new Trigger(
              () -> {
                var shiftInfo = HubShiftUtil.getShiftedShiftInfo();
                return shiftInfo.active() && shiftInfo.remainingTime() < time;
              });
      shiftAboutToEnd
          .and(RobotModeTriggers.teleop())
          .onTrue(
              Commands.runEnd(
                      () -> driver.setRumble(GenericHID.RumbleType.kRightRumble, 1.0),
                      () -> driver.setRumble(GenericHID.RumbleType.kBothRumble, 0.0))
                  .withTimeout(0.25));
    }

    // End-of-inactive-shift warning
    for (var i = 1; i <= 10; i++) {
      double time = i;
      var shiftAboutToEnd =
          new Trigger(
              () -> {
                var shiftInfo = HubShiftUtil.getShiftedShiftInfo();
                return !shiftInfo.active() && shiftInfo.remainingTime() < time;
              });
      shiftAboutToEnd
          .and(RobotModeTriggers.teleop())
          .onTrue(
              Commands.runEnd(
                      () -> driver.setRumble(GenericHID.RumbleType.kRightRumble, 1.0),
                      () -> driver.setRumble(GenericHID.RumbleType.kBothRumble, 0.0))
                  .withTimeout(0.25));
    }

    // End-of-game warning
    for (var i = 1; i <= 10; i++) {
      double time = i;
      var gameAboutToEnd =
          new Trigger(
              () -> {
                var shiftInfo = HubShiftUtil.getShiftedShiftInfo();
                return shiftInfo.currentShift() == HubShiftUtil.ShiftEnum.ENDGAME
                    && shiftInfo.remainingTime() < time;
              });
      gameAboutToEnd
          .and(RobotModeTriggers.teleop())
          .onTrue(
              Commands.runEnd(
                      () -> driver.setRumble(GenericHID.RumbleType.kRightRumble, 1.0),
                      () -> driver.setRumble(GenericHID.RumbleType.kBothRumble, 0.0))
                  .withTimeout(0.25));
    }
  }

  private void updateSwerveTeleopInput() {
    swerve.setTeleopInput(-driver.getLeftY(), -driver.getLeftX(), -driver.getRightX(), false);
  }

  public Command setShooterScoreAndSwerveAimHub() {
    return Commands.sequence(
        Commands.runOnce(shooter::setScore, shooter),
        Commands.waitUntil(() -> shooter.getTargetFieldCentricYaw().isPresent()),
        Commands.runOnce(
            () -> swerve.setScoreHeadingGoal(() -> shooter.getTargetFieldCentricYaw().get())));
  }

  public Command setShooterTransportAndSwerveAimPlacement() {
    return Commands.sequence(
        Commands.runOnce(shooter::setTransport, shooter),
        Commands.waitUntil(() -> shooter.getTargetFieldCentricYaw().isPresent()),
        Commands.runOnce(
            () -> swerve.setHeadingGoal(() -> shooter.getTargetFieldCentricYaw().get())));
  }

  public Command getAutoCmd() {
    if (AgentAutoRunner.isEnabled()) {
      return agentAutoModeSelector.getCmd();
    }

    return autoModeSelector == null ? Commands.none() : autoModeSelector.getCmd();
  }

  void updateHopperCover() {
    if (Units.radiansToDegrees(intake.getSwerveToIntakeTransform().getRotation().getAngle())
        < 60.0) {
      isCoveredByHopper = false;
    }
  }

  void resetHopperCover() {
    isCoveredByHopper = true;
  }

  void updateJoysticksDisconnectedAlert() {
    driverDisconnected.set(!DriverStation.isJoystickConnected(driver.getHID().getPort()));
    codriverDisconnected.set(!DriverStation.isJoystickConnected(codriver.getHID().getPort()));
  }

  void setSwerveDriveBrakeMode() {
    swerve.setDriveNeutralMode(true);
  }

  void flushSwerveDriveCurrentLimit(boolean wantAutoMode) {
    swerve.flushDriveCurrentLimit(wantAutoMode);
  }
}
