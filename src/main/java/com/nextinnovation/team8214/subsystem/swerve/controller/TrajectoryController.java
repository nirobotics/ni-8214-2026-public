// Copyright (c) 2024 FRC 6328
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve.controller;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.subsystem.swerve.SwerveConfig;
import com.nextinnovation.team8214.util.GeomUtil;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import org.littletonrobotics.junction.Logger;

@ExtensionMethod({GeomUtil.class})
public class TrajectoryController {
  private static final LoggedTunableNumber translationXKp =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/trajectoryController/translationXKp",
          7.0);
  private static final LoggedTunableNumber translationXKd =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/trajectoryController/translationXKd",
          0.0);
  private static final LoggedTunableNumber translationYKp =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/trajectoryController/translationYKp",
          5.0);
  private static final LoggedTunableNumber translationYKd =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/trajectoryController/translationYKd",
          0.5);
  private static final LoggedTunableNumber rotationKp =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/trajectoryController/rotationKp",
          5.0);
  private static final LoggedTunableNumber rotationKd =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/trajectoryController/rotationKd",
          0.0);

  private final Trajectory<SwerveSample> trajectory;
  private final PIDController xController;
  private final PIDController yController;
  private final PIDController rotationController;
  private final Timer timer = new Timer();

  @Getter
  private List<Vector<N2>> moduleForces =
      IntStream.range(0, 4).boxed().map(i -> VecBuilder.fill(0, 0)).toList();

  private SwerveSample setpoint = null;
  private boolean hasStart = false;

  public TrajectoryController(Trajectory<SwerveSample> trajectory) {
    this.trajectory = trajectory;

    xController = new PIDController(translationXKp.get(), 0.0, translationXKd.get());
    yController = new PIDController(translationYKp.get(), 0.0, translationYKd.get());
    rotationController = new PIDController(rotationKp.get(), 0.0, rotationKd.get());
    rotationController.enableContinuousInput(-Math.PI, Math.PI);

    xController.setTolerance(0.025);
    yController.setTolerance(0.025);
    rotationController.setTolerance(Units.degreesToRadians(1.0));

    // Log poses
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/trajectoryController/trajectoryPoses", trajectory.getPoses());
  }

  public ChassisSpeeds update() {
    if (setpoint != null) {
      Logger.recordOutput(
          SwerveConfig.LOG_ROOT + "/trajectoryController/setpointPose", setpoint.getPose());
      Logger.recordOutput(
          SwerveConfig.LOG_ROOT + "/trajectoryController/setpointVel/vx", setpoint.vx);
      Logger.recordOutput(
          SwerveConfig.LOG_ROOT + "/trajectoryController/setpointVel/vy", setpoint.vy);
      Logger.recordOutput(
          SwerveConfig.LOG_ROOT + "/trajectoryController/setpointVel/rotation", setpoint.omega);
    }

    if (!hasStart) {
      hasStart = true;
      setpoint = trajectory.getInitialSample(false).get();
      timer.start();
    } else if (!hasDone()) {
      setpoint = trajectory.sampleAt(timer.get(), false).get();
    } else {
      setpoint = trajectory.getInitialSample(false).get();
      return new ChassisSpeeds(0.0, 0.0, 0.0);
    }

    var trajectoryVel =
        ChassisSpeeds.fromFieldRelativeSpeeds(
            setpoint.vx, setpoint.vy, setpoint.omega, setpoint.getPose().getRotation());

    Odometry.getInstance().addTrajectoryVel(trajectoryVel.toTwist2d());

    var currentPose = Odometry.getInstance().getEstimatedPose();
    var setpointPose = setpoint.getPose();

    var xFeedback = xController.calculate(currentPose.getX(), setpointPose.getX());
    var yFeedback = yController.calculate(currentPose.getY(), setpointPose.getY());
    var rotationFeedback =
        rotationController.calculate(
            MathUtil.angleModulus(currentPose.getRotation().getRadians()),
            MathUtil.angleModulus(setpointPose.getRotation().getRadians()));

    var setpointHeading = setpointPose.getRotation();
    var moduleForcesX = setpoint.moduleForcesX();
    var moduleForcesY = setpoint.moduleForcesY();

    // [FL, FR, BL, BR] -> [FL, BL, BR, FR]
    moduleForces = new ArrayList<>(4);
    moduleForces.add(fixModuleForce(moduleForcesX[0], moduleForcesY[0], setpointHeading));
    moduleForces.add(fixModuleForce(moduleForcesX[2], moduleForcesY[2], setpointHeading));
    moduleForces.add(fixModuleForce(moduleForcesX[3], moduleForcesY[3], setpointHeading));
    moduleForces.add(fixModuleForce(moduleForcesX[1], moduleForcesY[1], setpointHeading));

    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/trajectoryController/translationError",
        currentPose.getTranslation().getDistance(setpointPose.getTranslation()));
    Logger.recordOutput(
        SwerveConfig.LOG_ROOT + "/trajectoryController/rotationError",
        currentPose.getRotation().minus(setpointPose.getRotation()));

    return ChassisSpeeds.fromFieldRelativeSpeeds(
        setpoint.vx + xFeedback,
        setpoint.vy + yFeedback,
        setpoint.omega + rotationFeedback,
        currentPose.getRotation());
  }

  public boolean hasDone() {
    return timer.hasElapsed(trajectory.getTotalTime());
  }

  private static Vector<N2> fixModuleForce(double x, double y, Rotation2d headingInField) {
    return new Translation2d(x, y)
        .rotateBy(Rotation2d.fromRadians(headingInField.getRadians()).unaryMinus())
        .toVector();
  }
}
