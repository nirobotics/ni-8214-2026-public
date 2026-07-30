// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.nextinnovation.cyberpower.EnergyLogger;
import com.nextinnovation.cyberpower.EnergySubsystem;
import com.nextinnovation.team8214.*;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.subsystem.swerve.controller.*;
import com.nextinnovation.team8214.util.*;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.kinematics.*;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import org.ironmaple.simulation.SimulatedArena;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

@ExtensionMethod({GeomUtil.class, EqualsUtil.GeomExtensions.class})
public class Swerve extends SubsystemBase {
  private final LoggedTunableNumber maxTiltAccelXMeterPerSec2 =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/maxTiltAccelXMeterPerSec2",
          Config.MODE == Config.Mode.REAL ? 25.0 : 100.0);
  private final LoggedTunableNumber maxTiltAccelYMeterPerSec2 =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/maxTiltAccelYMeterPerSec2",
          Config.MODE == Config.Mode.REAL ? 25.0 : 100.0);
  private final LoggedTunableNumber maxSkidAccelMeterPerSec2 =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/maxSkidAccelMeterPerSec2",
          Config.MODE == Config.Mode.REAL ? 19.0 : 100.0);
  private final LoggedTunableNumber maxForwardAccelMeterPerSec2 =
      new LoggedTunableNumber(
          SwerveConfig.LOG_GROUP,
          SwerveConfig.LOG_ROOT + "/maxForwardAccelMeterPerSec2",
          Config.MODE == Config.Mode.REAL ? 25.0 : 100.0);

  public enum ControlMode {
    TELEOP,
    TRAJECTORY,
    BUMP_POUNCE,
    TRENCH_POUNCE,
    STICK_2_POINT,
    DEPOT_POUNCE,
    FORT,
  }

  static class SwerveOdometryInputs implements LoggableInputs {
    double[] odometryTimestamps = new double[0];
    SwerveModulePosition[] odometryFLPositions = new SwerveModulePosition[0];
    SwerveModulePosition[] odometryBLPositions = new SwerveModulePosition[0];
    SwerveModulePosition[] odometryBRPositions = new SwerveModulePosition[0];
    SwerveModulePosition[] odometryFRPositions = new SwerveModulePosition[0];
    Rotation2d[] odometryYaws = new Rotation2d[0];
    boolean[] odometryYawValid = new boolean[0];
    long queueOverflowCount = 0;
    long invalidWheelSampleCount = 0;

    @Override
    public void toLog(LogTable table) {
      table.put("OdometryTimestamps", odometryTimestamps);
      table.put("OdometryFLPositions", odometryFLPositions);
      table.put("OdometryBLPositions", odometryBLPositions);
      table.put("OdometryBRPositions", odometryBRPositions);
      table.put("OdometryFRPositions", odometryFRPositions);
      table.put("OdometryYaws", odometryYaws);
      table.put("OdometryYawValid", odometryYawValid);
      table.put("QueueOverflowCount", queueOverflowCount);
      table.put("InvalidWheelSampleCount", invalidWheelSampleCount);
    }

    @Override
    public void fromLog(LogTable table) {
      odometryTimestamps = table.get("OdometryTimestamps", odometryTimestamps);
      odometryFLPositions = table.get("OdometryFLPositions", odometryFLPositions);
      odometryBLPositions = table.get("OdometryBLPositions", odometryBLPositions);
      odometryBRPositions = table.get("OdometryBRPositions", odometryBRPositions);
      odometryFRPositions = table.get("OdometryFRPositions", odometryFRPositions);
      odometryYaws = table.get("OdometryYaws", new Rotation2d[0]);
      var loggedYawValid = table.get("OdometryYawValid", new boolean[0]);
      var hasCompleteYawLog = odometryYaws.length == odometryTimestamps.length;
      if (!hasCompleteYawLog) {
        odometryYaws = new Rotation2d[odometryTimestamps.length];
        Arrays.fill(odometryYaws, Rotation2d.kZero);
      }
      if (hasCompleteYawLog && loggedYawValid.length == odometryTimestamps.length) {
        odometryYawValid = loggedYawValid;
      } else {
        odometryYawValid = new boolean[odometryTimestamps.length];
        Arrays.fill(odometryYawValid, hasCompleteYawLog);
      }
      queueOverflowCount = table.get("QueueOverflowCount", 0L);
      invalidWheelSampleCount = table.get("InvalidWheelSampleCount", 0L);
    }
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/controlMode")
  @Getter
  private ControlMode controlMode = ControlMode.TELEOP;

  private final Module[] modules = new Module[4];
  private final EnergySubsystem energySubsystem =
      EnergyLogger.getInstance().createSubsystem("swerve");
  private final GyroIO gyroIO;
  private final GyroIOInputsAutoLogged gyroInputs = new GyroIOInputsAutoLogged();
  private final SwerveOdometryInputs swerveOdometryInputs = new SwerveOdometryInputs();
  private final ArrayBlockingQueue<Odometry.WheeledObservation>
      odometryCachedWheeledObservationQueue;
  private final AtomicLong odometryQueueOverflowCount = new AtomicLong();
  private final AtomicLong odometryInvalidWheelSampleCount = new AtomicLong();

  private SwerveModuleState[] lastGoalModuleStates =
      new SwerveModuleState[] {
        new SwerveModuleState(),
        new SwerveModuleState(),
        new SwerveModuleState(),
        new SwerveModuleState(),
      };

  private final TeleopController teleopController = new TeleopController();
  private HeadingController headingController = null;
  private TrajectoryController trajectoryController = null;
  private BumpPounceController bumpPounceController = null;
  private Stick2PointController stick2PointController = null;
  private DepotPounceController depotPounceController = null;
  private TrenchPounceController trenchPounceController = null;
  private Translation2d centerOfRotation = Translation2d.kZero;

  @Setter private Supplier<Double> customMaxTiltAccelScale = () -> 1.0;

  private final Alert gyroOfflineAlert = new Alert("Gyro offline!", Alert.AlertType.WARNING);

  public Swerve() {
    switch (Config.MODE) {
      case REAL -> {
        var flModuleIo =
            new ModuleIOKrakenFOC(
                SwerveConfig.FL_MODULE_NAME,
                Ports.Can.FL_DRIVE_MOTOR,
                Ports.Can.FL_STEER_MOTOR,
                Ports.Can.FL_STEER_SENSOR,
                SwerveConfig.FL_MODULE_CONFIG);

        var blModuleIo =
            new ModuleIOKrakenFOC(
                SwerveConfig.BL_MODULE_NAME,
                Ports.Can.BL_DRIVE_MOTOR,
                Ports.Can.BL_STEER_MOTOR,
                Ports.Can.BL_STEER_SENSOR,
                SwerveConfig.BL_MODULE_CONFIG);

        var brModuleIo =
            new ModuleIOKrakenFOC(
                SwerveConfig.BR_MODULE_NAME,
                Ports.Can.BR_DRIVE_MOTOR,
                Ports.Can.BR_STEER_MOTOR,
                Ports.Can.BR_STEER_SENSOR,
                SwerveConfig.BR_MODULE_CONFIG);

        var frModuleIo =
            new ModuleIOKrakenFOC(
                SwerveConfig.FR_MODULE_NAME,
                Ports.Can.FR_DRIVE_MOTOR,
                Ports.Can.FR_STEER_MOTOR,
                Ports.Can.FR_STEER_SENSOR,
                SwerveConfig.FR_MODULE_CONFIG);

        var gyroIOPigeon2 = new GyroIOPigeon2(Ports.Can.CHASSIS_PIGEON);

        gyroIO = gyroIOPigeon2;

        modules[0] = new Module(flModuleIo, SwerveConfig.FL_MODULE_NAME, energySubsystem);
        modules[1] = new Module(blModuleIo, SwerveConfig.BL_MODULE_NAME, energySubsystem);
        modules[2] = new Module(brModuleIo, SwerveConfig.BR_MODULE_NAME, energySubsystem);
        modules[3] = new Module(frModuleIo, SwerveConfig.FR_MODULE_NAME, energySubsystem);

        odometryCachedWheeledObservationQueue =
            new PhoenixOdometryThread(
                    flModuleIo.getDrivePosition(),
                    flModuleIo.getSteerAbsPosition(),
                    blModuleIo.getDrivePosition(),
                    blModuleIo.getSteerAbsPosition(),
                    brModuleIo.getDrivePosition(),
                    brModuleIo.getSteerAbsPosition(),
                    frModuleIo.getDrivePosition(),
                    frModuleIo.getSteerAbsPosition(),
                    gyroIOPigeon2.getYaw(),
                    odometryQueueOverflowCount,
                    odometryInvalidWheelSampleCount)
                .start();
      }

      case SIM -> {
        var robotSim = Sim.getInstance();
        var sim = robotSim.getSwerve();
        SimulatedArena.getInstance().addDriveTrainSimulation(sim);

        var flModuleIo = new ModuleIOSim(sim.getModules()[0]);
        var blModuleIo = new ModuleIOSim(sim.getModules()[1]);
        var brModuleIo = new ModuleIOSim(sim.getModules()[2]);
        var frModuleIo = new ModuleIOSim(sim.getModules()[3]);
        gyroIO =
            new GyroIOMapleSim(
                sim.getGyroSimulation(), robotSim::getBumpRollRad, robotSim::getBumpPitchRad);

        modules[0] = new Module(flModuleIo, SwerveConfig.FL_MODULE_NAME, energySubsystem);
        modules[1] = new Module(blModuleIo, SwerveConfig.BL_MODULE_NAME, energySubsystem);
        modules[2] = new Module(brModuleIo, SwerveConfig.BR_MODULE_NAME, energySubsystem);
        modules[3] = new Module(frModuleIo, SwerveConfig.FR_MODULE_NAME, energySubsystem);

        odometryCachedWheeledObservationQueue =
            new SimOdometryThread(
                    modules[0]::getDrivePositionRad,
                    modules[0]::getSteerPositionRad,
                    modules[1]::getDrivePositionRad,
                    modules[1]::getSteerPositionRad,
                    modules[2]::getDrivePositionRad,
                    modules[2]::getSteerPositionRad,
                    modules[3]::getDrivePositionRad,
                    modules[3]::getSteerPositionRad,
                    () -> gyroInputs.yawPosition.getRadians(),
                    odometryQueueOverflowCount)
                .start();
      }

      default -> {
        gyroIO = new GyroIO() {};

        modules[0] = new Module(new ModuleIO() {}, SwerveConfig.FL_MODULE_NAME, energySubsystem);
        modules[1] = new Module(new ModuleIO() {}, SwerveConfig.BL_MODULE_NAME, energySubsystem);
        modules[2] = new Module(new ModuleIO() {}, SwerveConfig.BR_MODULE_NAME, energySubsystem);
        modules[3] = new Module(new ModuleIO() {}, SwerveConfig.FR_MODULE_NAME, energySubsystem);

        odometryCachedWheeledObservationQueue = new ArrayBlockingQueue<>(20);
      }
    }

    Odometry.getInstance().addTrajectoryVel(new Twist2d());
  }

  public Transform3d getRobotToSwerveTransform() {
    return new Transform3d(
        0.0, 0.0, 0.0, new Rotation3d(Math.PI / 2, Math.PI / 2 * 0, 1 * Math.PI / 2));
  }

  private void updateInputs() {
    gyroIO.updateInputs(gyroInputs);
    Logger.processInputs(SwerveConfig.LOG_ROOT + "/gyroInputs", gyroInputs);

    for (var module : modules) {
      module.updateInputs();
    }

    var odometrySamples = new ArrayList<Odometry.WheeledObservation>();
    odometryCachedWheeledObservationQueue.drainTo(odometrySamples);

    var sampleNum = odometrySamples.size();
    swerveOdometryInputs.odometryTimestamps = new double[sampleNum];
    swerveOdometryInputs.odometryFLPositions = new SwerveModulePosition[sampleNum];
    swerveOdometryInputs.odometryBLPositions = new SwerveModulePosition[sampleNum];
    swerveOdometryInputs.odometryBRPositions = new SwerveModulePosition[sampleNum];
    swerveOdometryInputs.odometryFRPositions = new SwerveModulePosition[sampleNum];
    swerveOdometryInputs.odometryYaws = new Rotation2d[sampleNum];
    swerveOdometryInputs.odometryYawValid = new boolean[sampleNum];
    swerveOdometryInputs.queueOverflowCount = odometryQueueOverflowCount.get();
    swerveOdometryInputs.invalidWheelSampleCount = odometryInvalidWheelSampleCount.get();

    for (int i = 0; i < sampleNum; i++) {
      var observation = odometrySamples.get(i);
      swerveOdometryInputs.odometryTimestamps[i] = observation.timestamp();
      swerveOdometryInputs.odometryFLPositions[i] = observation.wheelPositions()[0];
      swerveOdometryInputs.odometryBLPositions[i] = observation.wheelPositions()[1];
      swerveOdometryInputs.odometryBRPositions[i] = observation.wheelPositions()[2];
      swerveOdometryInputs.odometryFRPositions[i] = observation.wheelPositions()[3];
      swerveOdometryInputs.odometryYaws[i] = observation.yaw();
      swerveOdometryInputs.odometryYawValid[i] = observation.yawValid();
    }

    Logger.processInputs(SwerveConfig.LOG_ROOT + "/swerveOdometryInputs", swerveOdometryInputs);
  }

  @Override
  public void periodic() {
    updateInputs();

    gyroOfflineAlert.set(!gyroInputs.connected);

    var odometry = Odometry.getInstance();
    for (int i = 0; i < swerveOdometryInputs.odometryTimestamps.length; i++) {
      var wheeledObservation =
          new Odometry.WheeledObservation(
              swerveOdometryInputs.odometryTimestamps[i],
              new SwerveModulePosition[] {
                swerveOdometryInputs.odometryFLPositions[i],
                swerveOdometryInputs.odometryBLPositions[i],
                swerveOdometryInputs.odometryBRPositions[i],
                swerveOdometryInputs.odometryFRPositions[i],
              },
              swerveOdometryInputs.odometryYaws[i],
              swerveOdometryInputs.odometryYawValid[i]);
      odometry.addWheeledObservation(wheeledObservation);
    }

    var currentVel = getVel();
    odometry.addRobotCentricVel(currentVel.toTwist2d());

    if (DriverStation.isDisabled()) {
      clearHeadingGoal();
    }

    var goalVel = new ChassisSpeeds();

    if (!DriverStation.isTeleopEnabled() && controlMode == ControlMode.TELEOP) {
      teleopController.setInput(0.0, 0.0, 0.0, false);
    }

    if (controlMode != ControlMode.TELEOP) {
      teleopController.setInput(0.0, 0.0, 0.0, false);
    }

    switch (controlMode) {
      case TELEOP -> {
        goalVel = teleopController.update();
        if (headingController != null) {
          teleopController.resetHeadingMaintainerSetpointToCurrent();
          goalVel.omegaRadiansPerSecond = headingController.update();
        }
      }

      case FORT -> {
        goalVel = new ChassisSpeeds();

        if (headingController != null) {
          teleopController.resetHeadingMaintainerSetpointToCurrent();
          headingController.update();
        }
      }

      case TRAJECTORY -> {
        if (trajectoryController != null) {
          teleopController.resetHeadingMaintainerSetpointToCurrent();
          goalVel = trajectoryController.update();
          if (headingController != null) {
            goalVel.omegaRadiansPerSecond = headingController.update();
          }
        }
      }

      case BUMP_POUNCE -> {
        if (bumpPounceController != null) {
          goalVel =
              bumpPounceController.update(
                  gyroInputs.rollPosition, gyroInputs.pitchPosition, gyroInputs.connected);
        }
      }

      case STICK_2_POINT -> {
        if (stick2PointController != null) {
          goalVel = stick2PointController.update();
        }
      }

      case DEPOT_POUNCE -> {
        if (depotPounceController != null) {
          goalVel = depotPounceController.update();
        }
      }

      case TRENCH_POUNCE -> {
        if (trenchPounceController != null) {
          goalVel = trenchPounceController.update();
        }
      }
    }

    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/controlMode", controlMode);

    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/rawGoalVel", goalVel);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/rawGoalVel/vx", goalVel.vxMetersPerSecond);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/rawGoalVel/vy", goalVel.vyMetersPerSecond);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/rawGoalVel/omega", goalVel.omegaRadiansPerSecond);

    var useRotationPriority = headingController != null || controlMode == ControlMode.TELEOP;
    if (useRotationPriority) {
      goalVel = applyRotationPriority(goalVel, centerOfRotation);
    }

    var rawGoalModuleStates =
        SwerveConfig.SWERVE_KINEMATICS.toSwerveModuleStates(goalVel, centerOfRotation);
    SwerveDriveKinematics.desaturateWheelSpeeds(
        rawGoalModuleStates, SwerveConfig.MAX_TRANSLATION_VEL_METER_PER_SEC);
    goalVel = SwerveConfig.SWERVE_KINEMATICS.toChassisSpeeds(rawGoalModuleStates);

    // 1690 Orbit accel limitation
    if (controlMode != ControlMode.TRAJECTORY
        && controlMode != ControlMode.BUMP_POUNCE
        && controlMode != ControlMode.TRENCH_POUNCE
        && controlMode != ControlMode.STICK_2_POINT
        && controlMode != ControlMode.DEPOT_POUNCE) {
      goalVel = applyGeneralAccelLimitation(currentVel, goalVel);
    }

    // Dynamics compensation
    goalVel = ChassisSpeeds.discretize(goalVel, Config.LOOP_PERIOD_SEC);

    var goalVelAtCenterOfRotation = toCenterOfRotationSpeeds(goalVel, centerOfRotation);
    if (useRotationPriority) {
      goalVelAtCenterOfRotation =
          applyRotationPriority(goalVelAtCenterOfRotation, centerOfRotation);
    }

    // Use last goal angle for module if chassis want stop completely
    var goalModuleStates =
        SwerveConfig.SWERVE_KINEMATICS.toSwerveModuleStates(
            goalVelAtCenterOfRotation, centerOfRotation);
    SwerveDriveKinematics.desaturateWheelSpeeds(
        goalModuleStates, SwerveConfig.MAX_TRANSLATION_VEL_METER_PER_SEC);
    goalVel = SwerveConfig.SWERVE_KINEMATICS.toChassisSpeeds(goalModuleStates);
    if (goalVel.toTwist2d().epsilonEquals(new Twist2d())) {
      for (int i = 0; i < modules.length; i++) {
        goalModuleStates[i].angle = lastGoalModuleStates[i].angle;
        goalModuleStates[i].speedMetersPerSecond = 0.0;
      }
    }

    var optimizedGoalModuleStates = new SwerveModuleState[4];

    for (int i = 0; i < modules.length; i++) {
      // Optimize setpoints
      optimizedGoalModuleStates[i] = goalModuleStates[i];
      optimizedGoalModuleStates[i].optimize(modules[i].getState().angle);
      if (controlMode != ControlMode.FORT) {
        modules[i].setState(optimizedGoalModuleStates[i]);
      } else {
        final var rawTargetAngle =
            (i == 1 || i == 2) ? Rotation2d.kZero : Rotation2d.fromDegrees(-(45.0 + 90.0 * i));
        var optimizedTargetState = new SwerveModuleState(0.0, rawTargetAngle);
        optimizedTargetState.optimize(modules[i].getState().angle);
        modules[i].setState(optimizedTargetState);
      }
    }

    lastGoalModuleStates = goalModuleStates;

    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/goalModuleStates", goalModuleStates);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/finalGoalVel", goalVel);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/finalGoalVel/vx", goalVel.vxMetersPerSecond);
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/finalGoalVel/vy", goalVel.vyMetersPerSecond);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/finalGoalVel/omega", goalVel.omegaRadiansPerSecond);
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/optimizedGoalModuleStates", optimizedGoalModuleStates);
    energySubsystem.periodic(controlMode);
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/robotCentricVel")
  public ChassisSpeeds getVel() {
    var vel = SwerveConfig.SWERVE_KINEMATICS.toChassisSpeeds(getModuleStates());
    if (gyroInputs.connected) {
      vel.omegaRadiansPerSecond = gyroInputs.yawVelocityRadPerSec;
    }

    return vel;
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/moduleStates")
  public SwerveModuleState[] getModuleStates() {
    return Arrays.stream(modules).map(Module::getState).toArray(SwerveModuleState[]::new);
  }

  public void setTeleopInput(double x, double y, double omega, boolean wantEscape) {
    if (DriverStation.isTeleopEnabled()) {
      controlMode = ControlMode.TELEOP;
      teleopController.setInput(x, y, omega, wantEscape);
    }
  }

  public void disableTeleopControllerHeadingMaintainer() {
    teleopController.disableHeadingMaintainer();
  }

  public void setTrajectory(Trajectory<SwerveSample> trajectory) {
    if (DriverStation.isAutonomousEnabled()) {
      disableTeleopControllerHeadingMaintainer();
      controlMode = ControlMode.TRAJECTORY;
      trajectoryController = new TrajectoryController(trajectory);
    }
  }

  public void clearTrajectory() {
    trajectoryController = null;
    controlMode = ControlMode.TELEOP;
    Odometry.getInstance().addTrajectoryVel(new Twist2d());
  }

  public void setBumpPounce(Pose2d startPose, Pose2d goalPose) {
    disableTeleopControllerHeadingMaintainer();
    clearHeadingGoal();
    bumpPounceController = new BumpPounceController(startPose, goalPose);
    controlMode = ControlMode.BUMP_POUNCE;
  }

  public void clearBumpPounce() {
    bumpPounceController = null;
    clearHeadingGoal();
    controlMode = ControlMode.TELEOP;
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/hasBumpPounceDone")
  public boolean hasBumpPounceDone() {
    return bumpPounceController != null && bumpPounceController.hasDone();
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/hasBumpPounceSafetyStopped")
  public boolean hasBumpPounceSafetyStopped() {
    return bumpPounceController != null && bumpPounceController.hasSafetyStopped();
  }

  public void setFort() {
    disableTeleopControllerHeadingMaintainer();
    controlMode = ControlMode.FORT;
  }

  public void setTrenchPounce(Pose2d goalPounce) {
    disableTeleopControllerHeadingMaintainer();
    controlMode = ControlMode.TRENCH_POUNCE;
    trenchPounceController = new TrenchPounceController(goalPounce);
  }

  public void clearTrenchPounce() {
    trenchPounceController = null;
    controlMode = ControlMode.TELEOP;
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/hasTrenchPounceDone")
  public boolean hasTrenchPounceDone() {
    return trenchPounceController != null && trenchPounceController.hasDone();
  }

  public void setStick2PointStartPounce(Pose2d goalPounce) {
    disableTeleopControllerHeadingMaintainer();
    controlMode = ControlMode.STICK_2_POINT;
    stick2PointController = new Stick2PointController(goalPounce);
  }

  public void clearStick2PointStartPounce() {
    stick2PointController = null;
    controlMode = ControlMode.TELEOP;
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/hasStick2PointStartPounceDone")
  public boolean hasStick2PointStartPounceDone() {
    return stick2PointController != null && stick2PointController.hasDone();
  }

  public void setDepotPounce() {
    disableTeleopControllerHeadingMaintainer();
    controlMode = ControlMode.DEPOT_POUNCE;
    depotPounceController = new DepotPounceController();
  }

  public void clearDepotPounce() {
    depotPounceController = null;
    controlMode = ControlMode.TELEOP;
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/hasDepotPounceDone")
  public boolean hasDepotPounceDone() {
    return depotPounceController != null && depotPounceController.hasDone();
  }

  public void setHeadingGoal(Supplier<Rotation2d> goalHeadingSupplier) {
    centerOfRotation = Translation2d.kZero;
    headingController = new HeadingController(goalHeadingSupplier);
  }

  public void setScoreHeadingGoal(Supplier<Rotation2d> goalHeadingSupplier) {
    centerOfRotation = selectScoreCenterOfRotation(goalHeadingSupplier.get());
    headingController = new HeadingController(goalHeadingSupplier);
  }

  public void clearHeadingGoal() {
    headingController = null;
    centerOfRotation = Translation2d.kZero;
  }

  private Translation2d selectScoreCenterOfRotation(Rotation2d goalHeading) {
    var robotPose = Odometry.getInstance().getEstimatedPose();
    if (isSweepCollisionFree(robotPose, goalHeading, Translation2d.kZero)) {
      return Translation2d.kZero;
    }

    var bestCenterOfRotation = Translation2d.kZero;
    var minimumTravelDistance = Double.POSITIVE_INFINITY;
    var turningAngleRad = Math.abs(goalHeading.minus(robotPose.getRotation()).getRadians());

    for (var candidate : SwerveConfig.SCORE_CENTER_OF_ROTATIONS) {
      var travelDistance = candidate.getNorm() * turningAngleRad;
      if (travelDistance < minimumTravelDistance
          && isSweepCollisionFree(robotPose, goalHeading, candidate)) {
        bestCenterOfRotation = candidate;
        minimumTravelDistance = travelDistance;
      }
    }

    return bestCenterOfRotation;
  }

  private boolean isSweepCollisionFree(
      Pose2d startPose, Rotation2d goalHeading, Translation2d centerOfRotation) {
    var deltaHeading = goalHeading.minus(startPose.getRotation());
    return !intersectsObstacle(
            startPose,
            deltaHeading,
            centerOfRotation,
            AllianceFlipUtil.apply(Field.BLUE_SCORE_ROTATION_OBSTACLES),
            SwerveConfig.INTAKE_SIDE_X_METER)
        && !intersectsObstacle(
            startPose,
            deltaHeading,
            centerOfRotation,
            AllianceFlipUtil.apply(Field.BLUE_SCORE_ROTATION_BUMPER_ONLY_OBSTACLES),
            SwerveConfig.ROBOT_HALF_WIDTH_METER);
  }

  private static boolean intersectsObstacle(
      Pose2d startPose,
      Rotation2d deltaHeading,
      Translation2d centerOfRotation,
      Translation2d[] obstacleSegments,
      double robotFrontX) {
    var pivotInField =
        startPose.getTranslation().plus(centerOfRotation.rotateBy(startPose.getRotation()));
    var fieldToStartRotation = startPose.getRotation().unaryMinus();

    for (int i = 0; i < obstacleSegments.length; i += 2) {
      var start = obstacleSegments[i].minus(pivotInField).rotateBy(fieldToStartRotation);
      var end = obstacleSegments[i + 1].minus(pivotInField).rotateBy(fieldToStartRotation);
      if (PolygonUtil.segmentIntersectsRotatedRectangleSweep(
          start,
          end,
          deltaHeading,
          SwerveConfig.SHOOTER_SIDE_X_METER - centerOfRotation.getX(),
          robotFrontX - centerOfRotation.getX(),
          -SwerveConfig.ROBOT_HALF_WIDTH_METER - centerOfRotation.getY(),
          SwerveConfig.ROBOT_HALF_WIDTH_METER - centerOfRotation.getY())) {
        return true;
      }
    }

    return false;
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/hasTrajectoryDone")
  public boolean hasTrajectoryDone() {
    return trajectoryController != null && trajectoryController.hasDone();
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/atHeadingGoal")
  public boolean atHeadingGoal() {
    return headingController != null && headingController.atGoal();
  }

  public void clearCustomMaxTiltAccelFactor() {
    customMaxTiltAccelScale = () -> 1.0;
  }

  static ChassisSpeeds toCenterOfRotationSpeeds(
      ChassisSpeeds robotCenterSpeeds, Translation2d centerOfRotation) {
    return new ChassisSpeeds(
        robotCenterSpeeds.vxMetersPerSecond
            - robotCenterSpeeds.omegaRadiansPerSecond * centerOfRotation.getY(),
        robotCenterSpeeds.vyMetersPerSecond
            + robotCenterSpeeds.omegaRadiansPerSecond * centerOfRotation.getX(),
        robotCenterSpeeds.omegaRadiansPerSecond);
  }

  static ChassisSpeeds applyRotationPriority(
      ChassisSpeeds desiredSpeeds, Translation2d centerOfRotation) {
    var maxModuleSpeed = SwerveConfig.MAX_TRANSLATION_VEL_METER_PER_SEC;
    var maxModuleRadius = 0.0;
    for (var moduleTranslation : SwerveConfig.MODULE_TRANSLATIONS) {
      maxModuleRadius =
          Math.max(
              maxModuleRadius,
              Math.hypot(
                  moduleTranslation.getX() - centerOfRotation.getX(),
                  moduleTranslation.getY() - centerOfRotation.getY()));
    }

    var omega =
        MathUtil.clamp(
            desiredSpeeds.omegaRadiansPerSecond,
            -maxModuleSpeed / maxModuleRadius,
            maxModuleSpeed / maxModuleRadius);
    var translationSpeedSquared =
        desiredSpeeds.vxMetersPerSecond * desiredSpeeds.vxMetersPerSecond
            + desiredSpeeds.vyMetersPerSecond * desiredSpeeds.vyMetersPerSecond;
    if (translationSpeedSquared < 1e-12) {
      return new ChassisSpeeds(0.0, 0.0, omega);
    }

    var translationScale = 1.0;
    // Solve |scale * translation + rotation| <= maxModuleSpeed for every module.
    for (var moduleTranslation : SwerveConfig.MODULE_TRANSLATIONS) {
      var rotationX = -omega * (moduleTranslation.getY() - centerOfRotation.getY());
      var rotationY = omega * (moduleTranslation.getX() - centerOfRotation.getX());
      var projection =
          desiredSpeeds.vxMetersPerSecond * rotationX + desiredSpeeds.vyMetersPerSecond * rotationY;
      var remainingSpeedSquared =
          Math.max(
              0.0, maxModuleSpeed * maxModuleSpeed - rotationX * rotationX - rotationY * rotationY);
      var root =
          Math.sqrt(projection * projection + translationSpeedSquared * remainingSpeedSquared);
      var moduleTranslationScale =
          projection > 0.0
              ? remainingSpeedSquared / (projection + root)
              : (-projection + root) / translationSpeedSquared;
      translationScale = Math.min(translationScale, moduleTranslationScale);
    }
    translationScale = MathUtil.clamp(translationScale, 0.0, 1.0);

    return new ChassisSpeeds(
        desiredSpeeds.vxMetersPerSecond * translationScale,
        desiredSpeeds.vyMetersPerSecond * translationScale,
        omega);
  }

  private ChassisSpeeds applyGeneralAccelLimitation(
      final ChassisSpeeds currentVel, final ChassisSpeeds goalVel) {
    var currentTranslationVel =
        new Translation2d(currentVel.vxMetersPerSecond, currentVel.vyMetersPerSecond);

    var goalTranslationVel =
        new Translation2d(goalVel.vxMetersPerSecond, goalVel.vyMetersPerSecond);

    var deltaVel = goalTranslationVel.minus(currentTranslationVel);

    if (EqualsUtil.epsilonEquals(deltaVel.getNorm(), 0.0)) {
      return new ChassisSpeeds(
          goalTranslationVel.getX(), goalTranslationVel.getY(), goalVel.omegaRadiansPerSecond);
    }

    var customMaxTiltAccelScaleVal = customMaxTiltAccelScale.get();
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/customMaxTiltAccelScale", customMaxTiltAccelScaleVal);

    var maxTiltLimitedDeltaVelX =
        maxTiltAccelXMeterPerSec2.get() * customMaxTiltAccelScaleVal * Config.LOOP_PERIOD_SEC;
    var maxTiltLimitedDeltaVelY =
        maxTiltAccelYMeterPerSec2.get() * customMaxTiltAccelScaleVal * Config.LOOP_PERIOD_SEC;

    deltaVel =
        new Translation2d(
            MathUtil.clamp(deltaVel.getX(), -maxTiltLimitedDeltaVelX, maxTiltLimitedDeltaVelX),
            MathUtil.clamp(deltaVel.getY(), -maxTiltLimitedDeltaVelY, maxTiltLimitedDeltaVelY));

    var maxSkidLimitedDeltaVel = maxSkidAccelMeterPerSec2.get() * Config.LOOP_PERIOD_SEC;

    deltaVel =
        new Translation2d(
            MathUtil.clamp(deltaVel.getNorm(), -maxSkidLimitedDeltaVel, maxSkidLimitedDeltaVel),
            deltaVel.toRotation2d());

    var noForwardLimitedGoalVel = currentTranslationVel.plus(deltaVel);

    var finalGoalVel = noForwardLimitedGoalVel;

    if (noForwardLimitedGoalVel.getNorm() > currentTranslationVel.getNorm()) {
      var forwardLimitedDeltaVelVal =
          maxForwardAccelMeterPerSec2.get()
              * (1.0
                  - MathUtil.clamp(
                      currentTranslationVel.getNorm()
                          / SwerveConfig.MAX_TRANSLATION_VEL_METER_PER_SEC,
                      0.0,
                      1.0))
              * Config.LOOP_PERIOD_SEC;

      finalGoalVel =
          new Translation2d(
              currentTranslationVel.getNorm()
                  + Math.min(
                      noForwardLimitedGoalVel.getNorm() - currentTranslationVel.getNorm(),
                      forwardLimitedDeltaVelVal),
              finalGoalVel.toRotation2d());
    }

    return new ChassisSpeeds(
        finalGoalVel.getX(), finalGoalVel.getY(), goalVel.omegaRadiansPerSecond);
  }

  public boolean setDriveNeutralMode(boolean wantBrake) {
    var isOk = true;
    for (var module : modules) {
      isOk &= module.setDriveNeutralMode(wantBrake);
    }
    return isOk;
  }

  public void flushDriveCurrentLimit(boolean wantAutoMode) {
    for (var module : modules) {
      module.flushDriveCurrentLimit(wantAutoMode);
    }
  }
}
