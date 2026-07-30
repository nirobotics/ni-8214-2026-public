// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Odometry;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.wpilibj.RobotController;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

class PhoenixOdometryThread {
  private final Thread thread;
  private volatile boolean isRunning = false;
  private final BaseStatusSignal[] signals = new BaseStatusSignal[9];
  private final ArrayBlockingQueue<Odometry.WheeledObservation>
      odometryCachedWheeledObservationQueue = new ArrayBlockingQueue<>(20);
  private final AtomicLong queueOverflowCount;
  private final AtomicLong invalidWheelSampleCount;
  private boolean forceYawBaseline = false;

  PhoenixOdometryThread(
      StatusSignal<Angle> fl_drive_signal,
      StatusSignal<Angle> fl_steer_signal,
      StatusSignal<Angle> bl_drive_signal,
      StatusSignal<Angle> bl_steer_signal,
      StatusSignal<Angle> br_drive_signal,
      StatusSignal<Angle> br_steer_signal,
      StatusSignal<Angle> fr_drive_signal,
      StatusSignal<Angle> fr_steer_signal,
      StatusSignal<Angle> yaw_signal,
      AtomicLong queueOverflowCount,
      AtomicLong invalidWheelSampleCount) {
    thread = new Thread(this::run);
    thread.setName("PhoenixOdometryThread");
    thread.setDaemon(true);
    this.queueOverflowCount = queueOverflowCount;
    this.invalidWheelSampleCount = invalidWheelSampleCount;

    signals[0] = fl_drive_signal.clone();
    signals[1] = fl_steer_signal.clone();
    signals[2] = bl_drive_signal.clone();
    signals[3] = bl_steer_signal.clone();
    signals[4] = br_drive_signal.clone();
    signals[5] = br_steer_signal.clone();
    signals[6] = fr_drive_signal.clone();
    signals[7] = fr_steer_signal.clone();
    signals[8] = yaw_signal.clone();
  }

  ArrayBlockingQueue<Odometry.WheeledObservation> start() {
    if (!isRunning) {
      isRunning = true;
      thread.start();
    }
    isRunning = true;

    return odometryCachedWheeledObservationQueue;
  }

  private void run() {
    BaseStatusSignal.setUpdateFrequencyForAll(SwerveConfig.ODOMETRY_FREQUENCY_HZ, signals);

    while (isRunning) {
      BaseStatusSignal.waitForAll(Config.LOOP_PERIOD_SEC, signals);

      var wheelSignalsValid = true;
      for (int i = 0; i < 8; i++) {
        wheelSignalsValid &= signals[i].getStatus().isOK();
      }
      var yawSignalValid = signals[8].getStatus().isOK();
      if (!wheelSignalsValid) {
        invalidWheelSampleCount.incrementAndGet();
        forceYawBaseline |= !yawSignalValid;
        continue;
      }

      var totalLatencySec = 0.0;
      for (int i = 0; i < 8; i++) {
        totalLatencySec += signals[i].getTimestamp().getLatency();
      }
      var timestampSignalCount = 8;
      if (yawSignalValid) {
        totalLatencySec += signals[8].getTimestamp().getLatency();
        timestampSignalCount++;
      }
      var timestampSec =
          RobotController.getFPGATime() / 1.0e6 - totalLatencySec / timestampSignalCount;

      var observation =
          new Odometry.WheeledObservation(
              timestampSec,
              new SwerveModulePosition[] {
                signalValue2SwerveModulePosition(
                    signals[0].getValueAsDouble(), signals[1].getValueAsDouble()),
                signalValue2SwerveModulePosition(
                    signals[2].getValueAsDouble(), signals[3].getValueAsDouble()),
                signalValue2SwerveModulePosition(
                    signals[4].getValueAsDouble(), signals[5].getValueAsDouble()),
                signalValue2SwerveModulePosition(
                    signals[6].getValueAsDouble(), signals[7].getValueAsDouble()),
              },
              Rotation2d.fromDegrees(signals[8].getValueAsDouble()),
              yawSignalValid && !forceYawBaseline);
      if (!odometryCachedWheeledObservationQueue.offer(observation)) {
        queueOverflowCount.incrementAndGet();
        forceYawBaseline |= !yawSignalValid;
      } else {
        forceYawBaseline = false;
      }
    }

    isRunning = false;
  }

  private SwerveModulePosition signalValue2SwerveModulePosition(
      double rawDrivePosition, double rawSteerPosition) {
    return new SwerveModulePosition(
        Units.rotationsToRadians(rawDrivePosition) * SwerveConfig.WHEEL_RADIUS_METER,
        Rotation2d.fromRotations(rawSteerPosition));
  }
}
