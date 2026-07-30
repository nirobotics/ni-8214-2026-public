// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: BSD-3-Clause AND MIT

package com.nextinnovation.team8214.subsystem.swerve;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.MountPoseConfigs;
import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.nextinnovation.team8214.util.driver.CanId;
import com.nextinnovation.team8214.util.driver.Phoenix6Helper;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import lombok.Getter;

public class GyroIOPigeon2 implements GyroIO {
  private final Pigeon2 pigeon;

  @Getter private final StatusSignal<Angle> yaw;
  private final StatusSignal<Angle> roll;
  private final StatusSignal<Angle> pitch;
  private final StatusSignal<AngularVelocity> yawVelocity;

  private boolean hasTiltOffset = false;
  private double rollOffsetDegree = 0.0;
  private double pitchOffsetDegree = 0.0;

  GyroIOPigeon2(CanId id) {
    pigeon = new Pigeon2(id.id(), id.bus());
    yaw = pigeon.getYaw();
    roll = pigeon.getRoll();
    pitch = pigeon.getPitch();
    yawVelocity = pigeon.getAngularVelocityZWorld();

    Phoenix6Helper.checkErrorAndRetry(
        "Gyro config",
        () ->
            pigeon
                .getConfigurator()
                .apply(
                    new Pigeon2Configuration()
                        .withMountPose(
                            new MountPoseConfigs()
                                .withMountPoseYaw(-88.90604400634766)
                                .withMountPosePitch(-8.12975025177002)
                                .withMountPoseRoll(5.204802513122559))));

    Phoenix6Helper.checkErrorAndRetry("Gyro zero", () -> pigeon.getConfigurator().setYaw(0.0));

    Phoenix6Helper.checkErrorAndRetry(
        "Gyro set signal update frequency",
        () -> BaseStatusSignal.setUpdateFrequencyForAll(100., yaw, roll, pitch, yawVelocity));

    Phoenix6Helper.checkErrorAndRetry(
        "Gyro optimize CAN utilization", pigeon::optimizeBusUtilization);
  }

  @Override
  public void updateInputs(GyroIOInputs inputs) {
    inputs.connected = BaseStatusSignal.refreshAll(yaw, roll, pitch, yawVelocity).isOK();

    var rawRollDegree = roll.getValueAsDouble();
    var rawPitchDegree = pitch.getValueAsDouble();
    if (inputs.connected && !hasTiltOffset) {
      rollOffsetDegree = rawRollDegree;
      pitchOffsetDegree = rawPitchDegree;
      hasTiltOffset = true;
    }

    inputs.yawPosition = Rotation2d.fromDegrees(yaw.getValueAsDouble());
    inputs.rollPosition =
        hasTiltOffset ? Rotation2d.fromDegrees(rawRollDegree - rollOffsetDegree) : new Rotation2d();
    inputs.pitchPosition =
        hasTiltOffset
            ? Rotation2d.fromDegrees(rawPitchDegree - pitchOffsetDegree)
            : new Rotation2d();
    inputs.yawVelocityRadPerSec = Units.degreesToRadians(yawVelocity.getValueAsDouble());
  }
}
