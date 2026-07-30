// Copyright (c) 2024 FRC 6328
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve.controller;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.subsystem.swerve.SwerveConfig;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import java.util.function.Supplier;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class HeadingController {
  private static final double MAX_ALLOWABLE_ANGULAR_VEL_RAD_PER_SEC =
      SwerveConfig.MAX_ANGULAR_VEL_RAD_PER_SEC * 0.6;

  private static final LoggedTunableNumber kp =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/headingController/kP",
          6.0);
  private static final LoggedTunableNumber kd =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/headingController/kD",
          0.0);
  private static final LoggedTunableNumber maxVelocityRadPerSec =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/headingController/maxVelocityRadPerSec",
          MAX_ALLOWABLE_ANGULAR_VEL_RAD_PER_SEC);
  private static final LoggedTunableNumber maxAccelerationRadPerSec2 =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/headingController/maxAccelerationRadPerSec2",
          MAX_ALLOWABLE_ANGULAR_VEL_RAD_PER_SEC / 0.1);
  private static final LoggedTunableNumber toleranceDegree =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SWERVE.toString(),
          SwerveConfig.LOG_ROOT + "/headingController/toleranceDegree",
          1.5);

  private final ProfiledPIDController pid;

  private final Supplier<Rotation2d> goadHeadingSupplier;

  public HeadingController(Supplier<Rotation2d> goalHeadingSupplier) {
    pid =
        new ProfiledPIDController(
            kp.get(),
            0.0,
            kd.get(),
            new TrapezoidProfile.Constraints(0.0, 0.0),
            Config.LOOP_PERIOD_SEC);
    pid.enableContinuousInput(-Math.PI, Math.PI);
    pid.setTolerance(Units.degreesToRadians(toleranceDegree.get()));

    this.goadHeadingSupplier = goalHeadingSupplier;

    pid.reset(
        Odometry.getInstance().getEstimatedPose().getRotation().getRadians(),
        Odometry.getInstance().getFieldCentricVel().dtheta);
  }

  public double update() {
    pid.setPID(kp.get(), 0.0, kd.get());
    pid.setTolerance(Units.degreesToRadians(toleranceDegree.get()));
    pid.setConstraints(
        new TrapezoidProfile.Constraints(
            maxVelocityRadPerSec.get(), maxAccelerationRadPerSec2.get()));

    var goalHeading = goadHeadingSupplier.get();
    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/headingController/goalHeading", goalHeading);
    var output =
        pid.calculate(
            Odometry.getInstance().getEstimatedPose().getRotation().getRadians(),
            goalHeading.getRadians());

    Logger.recordOutput(SwerveConfig.LOG_ROOT + "/headingController/error", pid.getPositionError());

    return output;
  }

  @AutoLogOutput(key = SwerveConfig.LOG_ROOT + "/headingController/atGoal")
  public boolean atGoal() {
    return pid.atGoal();
  }
}
